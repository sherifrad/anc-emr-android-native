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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.CameraCaptureHelper
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
    val latestFile by rememberUpdatedState(captureFile)
    val latestCommitted by rememberUpdatedState(committed)
    DisposableEffect(Unit) {
        onDispose {
            session.gone = true
            // A capture still being written is deleted here; its callback deletes it too if it lands later.
            session.inFlight?.let { it.delete() }
            session.inFlight = null
            if (!latestCommitted) {
                latestFile?.let { f -> CameraCaptureHelper.deleteIfCaptureTemp(context, CameraCaptureHelper.uriFor(context, f)) }
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
            val shot = captureFile
            when {
                !granted -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (permissionAsked) {
                        Text(
                            text = stringResource(R.string.camera_permission_needed),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    CloseButton(onDismiss)
                }

                shot != null -> {
                    val still by produceState<Bitmap?>(null, shot) {
                        value = withContext(Dispatchers.Default) { BoundedBitmap.decodeFile(shot.absolutePath, STILL_MAX_SIDE) }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { discardCapture() },
                                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("camera_retake"),
                            ) { Text(stringResource(R.string.camera_retake), color = Color.White) }
                            Button(
                                onClick = {
                                    committed = true
                                    onCaptured(CameraCaptureHelper.uriFor(context, shot))
                                },
                                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("camera_use"),
                            ) { Text(stringResource(R.string.camera_use)) }
                        }
                        CloseButton(onDismiss)
                    }
                }

                else -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).also { session.previewView = it } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("camera_preview"),
                    )
                    if (failed) {
                        Text(
                            text = stringResource(R.string.camera_capture_failed),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (bindFailed) {
                        Text(
                            text = stringResource(R.string.camera_bind_failed),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val captureLabel = stringResource(R.string.camera_capture)
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (capturing) Color.Gray else Color.White)
                                .clickable(enabled = !capturing, onClickLabel = captureLabel) { takePhoto() }
                                .semantics { contentDescription = captureLabel }
                                .testTag("camera_capture"),
                        )
                    }
                    CloseButton(onDismiss)
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    OutlinedButton(
        onClick = onDismiss,
        border = BorderStroke(1.dp, Color.White),
        modifier = Modifier.minimumTouchTarget().testTag("camera_close"),
    ) { Text(stringResource(R.string.camera_close), color = Color.White) }
}
