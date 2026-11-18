package android.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

/** Lives in `android.print` because [PrintDocumentAdapter.LayoutResultCallback] and [PrintDocumentAdapter.WriteResultCallback] have package-private constructors. */
class PdfPrint(
    private val printAttributes: PrintAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build(),
) {

    /** Renders every page of [printAdapter] into [output] as an A4 PDF and notifies a terminal callback after closing [output]. */
    fun print(
        printAdapter: PrintDocumentAdapter,
        output: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal = CancellationSignal(),
        onSuccess: () -> Unit,
        onError: () -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        val layoutHandled = AtomicBoolean(false)
        val finished = AtomicBoolean(false)

        fun finish(notify: () -> Unit) {
            if (!finished.compareAndSet(false, true)) return
            runCatching { output.close() }
            notify()
        }

        fun reportCancelled() {
            finish { (onCancel ?: onError)() }
        }

        fun reportFailed() {
            finish(onError)
        }

        if (cancellationSignal.isCanceled) {
            reportCancelled()
            return
        }

        val writeCallback = object : PrintDocumentAdapter.WriteResultCallback() {
            override fun onWriteFinished(pages: Array<PageRange>?) {
                finish(onSuccess)
            }

            override fun onWriteFailed(error: CharSequence?) {
                reportFailed()
            }

            override fun onWriteCancelled() {
                reportCancelled()
            }
        }

        val layoutCallback = object : PrintDocumentAdapter.LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                if (!layoutHandled.compareAndSet(false, true) || finished.get()) return
                if (cancellationSignal.isCanceled) {
                    reportCancelled()
                    return
                }
                try {
                    printAdapter.onWrite(
                        arrayOf(PageRange.ALL_PAGES),
                        output,
                        cancellationSignal,
                        writeCallback,
                    )
                } catch (t: Throwable) {
                    reportFailed()
                    if (t is Error) throw t
                }
            }

            override fun onLayoutFailed(error: CharSequence?) {
                if (!layoutHandled.compareAndSet(false, true)) return
                reportFailed()
            }

            override fun onLayoutCancelled() {
                if (!layoutHandled.compareAndSet(false, true)) return
                reportCancelled()
            }
        }

        try {
            printAdapter.onLayout(
                null,
                printAttributes,
                cancellationSignal,
                layoutCallback,
                null,
            )
        } catch (t: Throwable) {
            reportFailed()
            if (t is Error) throw t
        }
    }
}
