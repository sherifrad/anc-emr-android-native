package com.sherifradwan.ancnative.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.CameraCaptureHelper
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STILL_MAX_SIDE = 1600

/** Mutable state shared with camera callbacks that may fire after the dialog has left composition. */
private class CameraSession {
    var inFlight: File? = null
    var gone = false
    var previewView: PreviewView? = null
}

/** Full-screen in-app camera: live preview, capture, then Retake / Use photo. The app is never left. */
@Composable
fun InAppCameraDialog(
    onCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionAsked by remember { mutableStateOf(granted) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var committed by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var bindFailed by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    val session = remember { CameraSession() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        permissionAsked = true
    }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    // Drop any uncommitted temp photo when the dialog leaves composition (close, back, app lock).
    // The delegated state is read directly in onDispose so it sees the value at disposal time.
    DisposableEffect(Unit) {
        onDispose {
            session.gone = true
            // A capture still being written is deleted here; its callback deletes it too if it lands later.
            session.inFlight?.let { it.delete() }
            session.inFlight = null
            if (!committed) {
                captureFile?.let { f -> CameraCaptureHelper.deleteIfCaptureTemp(context, CameraCaptureHelper.uriFor(context, f)) }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val live = granted && captureFile == null
    DisposableEffect(lifecycleOwner, live) {
        var provider: ProcessCameraProvider? = null
        var disposed = false
        if (live) {
            bindFailed = false
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (disposed) return@addListener
                runCatching {
                    val p = future.get()
                    provider = p
                    val pv = checkNotNull(session.previewView)
                    val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
                    p.unbindAll()
                    p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                }.onFailure { bindFailed = true }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            disposed = true
            provider?.unbindAll()
        }
    }

    fun discardCapture() {
        captureFile?.let { CameraCaptureHelper.deleteIfCaptureTemp(context, CameraCaptureHelper.uriFor(context, it)) }
        captureFile = null
    }

    fun takePhoto() {
        if (capturing) return
        capturing = true
        failed = false
        val file = CameraCaptureHelper.createCameraTempFile(context)
        session.inFlight = file
        imageCapture.targetRotation = view.display?.rotation ?: Surface.ROTATION_0
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    session.inFlight = null
                    if (session.gone) {
                        file.delete()
                        return
                    }
                    capturing = false
                    captureFile = file
                    failed = false
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    session.inFlight = null
                    if (session.gone) return
                    capturing = false
                    failed = true
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val shot = captureFile
                when {
                    !granted -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (permissionAsked) {
                            Surface(
                                shape = RecordShapes.Large,
                                color = Color(0xFF1A202E),
                                border = BorderStroke(1.dp, Color(0xFF3A4459)),
                                modifier = Modifier.fillMaxWidth().testTag("cameraPermissionCard"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.camera_permission_title),
                                        style = RecordType.SectionTitle,
                                        color = Color(0xFFE2E2EA),
                                        modifier = Modifier.semantics { heading() },
                                    )
                                    Text(
                                        text = stringResource(R.string.camera_permission_needed),
                                        style = RecordType.FieldValue,
                                        color = Color(0xFFCDC3C6),
                                    )
                                    Button(
                                        onClick = onDismiss,
                                        shape = RecordShapes.Small,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = ClinicalNavy,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumTouchTarget()
                                            .testTag("camera_close"),
                                    ) {
                                        Text(stringResource(R.string.camera_close), style = RecordType.Action)
                                    }
                                }
                            }
                        } else {
                            CloseButton(onDismiss)
                        }
                    }

                    shot != null -> {
                        val still by produceState<Bitmap?>(null, shot) {
                            value = withContext(Dispatchers.Default) { BoundedBitmap.decodeFile(shot.absolutePath, STILL_MAX_SIDE) }
                        }
                        val stackActions = LocalDensity.current.fontScale > 1.3f
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                CloseButton(onDismiss)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RecordShapes.Large)
                                    .border(1.dp, Color(0xFF3A4459), RecordShapes.Large),
                                contentAlignment = Alignment.Center,
                            ) {
                                val ready = still
                                if (ready != null) {
                                    Image(
                                        bitmap = ready.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                    )
                                } else {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                            val reviewButtons: @Composable (Modifier) -> Unit = { itemModifier ->
                                OutlinedButton(
                                    onClick = { discardCapture() },
                                    shape = RecordShapes.Small,
                                    border = BorderStroke(1.dp, Color(0xFF8E95A5)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = itemModifier.minimumTouchTarget().testTag("camera_retake"),
                                ) {
                                    Text(
                                        stringResource(R.string.camera_retake),
                                        style = RecordType.Action,
                                        color = Color.White,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Button(
                                    onClick = {
                                        committed = true
                                        onCaptured(CameraCaptureHelper.uriFor(context, shot))
                                    },
                                    shape = RecordShapes.Small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = ClinicalNavy,
                                    ),
                                    modifier = itemModifier.minimumTouchTarget().testTag("camera_use"),
                                ) {
                                    Text(
                                        stringResource(R.string.camera_use),
                                        style = RecordType.Action,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            if (stackActions) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    reviewButtons(Modifier.fillMaxWidth())
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    reviewButtons(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    else -> Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            CloseButton(onDismiss)
                        }
                        AndroidView(
                            factory = { ctx -> PreviewView(ctx).also { session.previewView = it } },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RecordShapes.Large)
                                .border(1.dp, Color(0xFF3A4459), RecordShapes.Large)
                                .testTag("camera_preview"),
                        )
                        if (failed || bindFailed) {
                            Surface(
                                shape = RecordShapes.Small,
                                color = StatusHighDarkBg,
                                border = BorderStroke(1.dp, StatusHighDarkBorder),
                                modifier = Modifier.fillMaxWidth().testTag("cameraErrorBanner"),
                            ) {
                                Text(
                                    text = stringResource(if (bindFailed) R.string.camera_bind_failed else R.string.camera_capture_failed),
                                    style = RecordType.Secondary,
                                    color = StatusHighDarkText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                        if (!bindFailed) {
                            val captureLabel = stringResource(R.string.camera_capture)
                            val shutterColor = if (capturing) Color(0xFF8E95A5) else Color.White
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, shutterColor, CircleShape)
                                    .clickable(enabled = !capturing, onClickLabel = captureLabel) { takePhoto() }
                                    .semantics { contentDescription = captureLabel }
                                    .testTag("camera_capture")
                                    .padding(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(shutterColor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    OutlinedButton(
        onClick = onDismiss,
        shape = RecordShapes.Small,
        border = BorderStroke(1.dp, Color(0xFF8E95A5)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.minimumTouchTarget().testTag("camera_close"),
    ) {
        Text(
            stringResource(R.string.camera_close),
            style = RecordType.Action,
            color = Color.White,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
