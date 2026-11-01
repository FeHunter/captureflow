package com.fehunter.captureflow.features.imageimport.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/** CameraX viewfinder owned by Capture Flow; the photo remains temporary until review confirmation. */
@Composable
fun ImageCaptureScreen(onImageCaptured: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    cameraProvider = provider
                    imageCapture = capture
                }.onFailure { captureError = it.localizedMessage ?: "Não foi possível iniciar a câmera." }
            }, mainExecutor)
        }
        onDispose {
            cameraProvider?.unbindAll()
            cameraProvider = null
            imageCapture = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
        Box(Modifier.fillMaxSize()) {
            if (hasPermission) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            else Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(44.dp))
                Text(
                    if (permissionDenied) "A permissão da câmera é necessária para fotografar." else "Aguardando permissão da câmera…",
                    color = androidx.compose.ui.graphics.Color.White,
                )
                if (permissionDenied) Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Permitir câmera") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar câmera", tint = androidx.compose.ui.graphics.Color.White)
                }
                Text("Nova captura", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
            }
            captureError?.let {
                Text(it, modifier = Modifier.align(Alignment.Center).padding(24.dp), color = androidx.compose.ui.graphics.Color.White)
            }
            IconButton(
                onClick = {
                    val useCase = imageCapture ?: return@IconButton
                    if (isCapturing) return@IconButton
                    isCapturing = true
                    captureError = null
                    val output = File(context.cacheDir, "captureflow-${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(output).build()
                    useCase.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onImageCaptured(Uri.fromFile(output).toString())
                        }
                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            captureError = exception.localizedMessage ?: "Não foi possível tirar a foto."
                            output.delete()
                        }
                    })
                },
                enabled = hasPermission && imageCapture != null && !isCapturing,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp)
                    .size(76.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f), CircleShape),
            ) {
                if (isCapturing) CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                else Box(Modifier.size(58.dp).background(androidx.compose.ui.graphics.Color.White, CircleShape))
            }
        }
    }
}
