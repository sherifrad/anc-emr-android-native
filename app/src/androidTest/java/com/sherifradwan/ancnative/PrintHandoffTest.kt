package com.sherifradwan.ancnative

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.view.ContextThemeWrapper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.ui.report.PatientPrintManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumentation tests verifying PatientPrintManager handoff to Android PrintManager.
 *
 * Verifies that ContextWrapper chains (e.g. Compose Dialog ContextThemeWrapper) resolve the underlying
 * Activity without crashing with IllegalStateException, that Activity contexts complete printing,
 * and that non-Activity contexts safely invoke onUnavailable without crashing.
 */
@RunWith(AndroidJUnit4::class)
class PrintHandoffTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient: DemoPatient = DemoFixtures.patients.first()

    @After
    fun tearDown() {
        runCatching {
            UiDevice.pressBack()
        }
    }

    @Test
    fun printPatientReport_withActivityContext_callsOnCompleteWithin30SecondsAndNeverUnavailable() {
        Assume.assumeTrue("WebView cannot render in this environment", webViewCanLoadPages())
        val completeLatch = CountDownLatch(1)
        val unavailableCalled = AtomicBoolean(false)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            PatientPrintManager.printPatientReport(
                context = compose.activity,
                patient = patient,
                settings = null,
                onComplete = {
                    completeLatch.countDown()
                },
                onUnavailable = {
                    unavailableCalled.set(true)
                },
                selectedPictureKeys = emptySet(),
                pictureDataUris = emptyMap(),
            )
        }

        val completed = completeLatch.await(30, TimeUnit.SECONDS)
        assertTrue("printing with Activity as context must call onComplete within 30 seconds", completed)
        assertFalse("onUnavailable must never be called when printing with Activity context", unavailableCalled.get())
    }

    @Test
    fun printPatientReport_withContextWrapperAroundActivity_callsOnComplete() {
        Assume.assumeTrue("WebView cannot render in this environment", webViewCanLoadPages())
        val wrappedContext = ContextThemeWrapper(compose.activity, R.style.Theme_AncNativeDemo)
        val completeLatch = CountDownLatch(1)
        val unavailableCalled = AtomicBoolean(false)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            PatientPrintManager.printPatientReport(
                context = wrappedContext,
                patient = patient,
                settings = null,
                onComplete = {
                    completeLatch.countDown()
                },
                onUnavailable = {
                    unavailableCalled.set(true)
                },
                selectedPictureKeys = emptySet(),
                pictureDataUris = emptyMap(),
            )
        }

        val completed = completeLatch.await(30, TimeUnit.SECONDS)
        assertTrue("printing with ContextWrapper around Activity must complete within 30 seconds", completed)
        assertFalse("onUnavailable must never be called when printing with ContextWrapper around Activity", unavailableCalled.get())
    }

    private fun webViewCanLoadPages(): Boolean {
        val pageLatch = CountDownLatch(1)
        val jsLatch = CountDownLatch(1)
        val jsSuccess = AtomicBoolean(false)
        var webView: WebView? = null
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView = WebView(compose.activity).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageLatch.countDown()
                        }
                    }
                    loadDataWithBaseURL(null, "<html><body>test</body></html>", "text/html", "UTF-8", null)
                }
            }
            if (!pageLatch.await(10, TimeUnit.SECONDS)) {
                return false
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView?.evaluateJavascript("1+1") { result ->
                    if (result == "2") {
                        jsSuccess.set(true)
                    }
                    jsLatch.countDown()
                }
            }
            return jsLatch.await(10, TimeUnit.SECONDS) && jsSuccess.get()
        } catch (_: Throwable) {
            return false
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runCatching {
                    webView?.destroy()
                }
            }
        }
    }

    @Test
    fun printPatientReport_withApplicationContext_callsOnUnavailableAndDoesNotCrash() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val unavailableLatch = CountDownLatch(1)
        val completeCalled = AtomicBoolean(false)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            PatientPrintManager.printPatientReport(
                context = appContext,
                patient = patient,
                settings = null,
                onComplete = {
                    completeCalled.set(true)
                },
                onUnavailable = {
                    unavailableLatch.countDown()
                },
                selectedPictureKeys = emptySet(),
                pictureDataUris = emptyMap(),
            )
        }

        val unavailable = unavailableLatch.await(5, TimeUnit.SECONDS)
        assertTrue("printing with applicationContext must call onUnavailable", unavailable)
        assertFalse("onComplete must never be called when printing with applicationContext", completeCalled.get())
    }
}

internal object UiDevice {
    fun getInstance(): UiDevice = this
    fun getInstance(instrumentation: Instrumentation): UiDevice = this

    fun pressBack(): Boolean {
        return runCatching {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val clazz = runCatching { Class.forName("androidx.test.uiautomator.UiDevice") }.getOrNull()
            if (clazz != null) {
                val getInstanceMethod = clazz.getMethod("getInstance", Instrumentation::class.java)
                val instance = getInstanceMethod.invoke(null, instrumentation)
                val method = clazz.getMethod("pressBack")
                (method.invoke(instance) as? Boolean) ?: true
            } else {
                val handled = runCatching {
                    instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                }.getOrDefault(false)
                if (!handled) {
                    runCatching {
                        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                    }
                }
                true
            }
        }.getOrDefault(false)
    }
}
