package com.rubookscanner.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScannerScreen(
    settings: AiSettings,
    pendingCrops: SnapshotStateList<Bitmap>,
    onFlashcardsAccepted: (List<Flashcard>) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        CameraScanContent(
            settings = settings,
            pendingCrops = pendingCrops,
            onFlashcardsAccepted = onFlashcardsAccepted
        )
    } else {
        PermissionRequiredScreen(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
    }
}

@Composable
private fun PermissionRequiredScreen(onRequest: () -> Unit) {
    val t = LocalStrings.current
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(t.cameraPermissionNeeded)
            Button(onClick = onRequest) {
                Text(t.allowCamera)
            }
        }
    }
}

@Composable
private fun CameraScanContent(
    settings: AiSettings,
    pendingCrops: SnapshotStateList<Bitmap>,
    onFlashcardsAccepted: (List<Flashcard>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val t = LocalStrings.current
    val previewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }

    var isCapturing by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Statikus, sárga, üres kör a képernyő közepén — ide célozd a szót.
        Box(
            Modifier
                .align(Alignment.Center)
                .size(30.dp)
                .border(2.dp, Color.Yellow, CircleShape)
        )



        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            errorMsg?.let {
                Card(Modifier.padding(bottom = 12.dp)) {
                    Text(t.errorPrefix(it), Modifier.padding(12.dp))
                }
            }

            if (pendingCrops.isNotEmpty()) {
                Card(Modifier.padding(bottom = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            t.collectedWords(pendingCrops.size),
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(
                            Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingCrops.size) { idx ->
                                Box {
                                    Image(
                                        bitmap = pendingCrops[idx].asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    )
                                    IconButton(
                                        onClick = { pendingCrops.removeAt(idx) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                    ) { Text("✕", color = Color.Red) }
                                }
                            }
                        }
                        Row(
                            Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { pendingCrops.clear() }) {
                                Text(t.clearList)
                            }
                            Button(
                                enabled = !isTranslating,
                                onClick = {
                                    if (settings.apiKey.isBlank()) {
                                        errorMsg = t.enterApiKeyFirst
                                        return@Button
                                    }
                                    isTranslating = true
                                    errorMsg = null
                                    val snapshot = pendingCrops.toList()
                                    scope.launch {
                                        try {
                                            val base64List = withContext(Dispatchers.Default) {
                                                snapshot.map { bitmapToJpegBase64(it) }
                                            }
                                            val client = AiClient(settings)
                                            val cards = withContext(Dispatchers.IO) {
                                                client.lookupWordsFromImages(base64List)
                                            }
                                            onFlashcardsAccepted(cards)
                                            pendingCrops.clear()
                                        } catch (e: Exception) {
                                            errorMsg = e.message ?: t.unknownError
                                        } finally {
                                            isTranslating = false
                                        }
                                    }
                                }
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(t.translateN(pendingCrops.size))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    isCapturing = true
                    errorMsg = null
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val rawBitmap = imageProxyToUprightBitmap(image)
                                image.close()
                                val screenAspect = if (previewView.height > 0) {
                                    previewView.width.toFloat() / previewView.height.toFloat()
                                } else {
                                    rawBitmap.width.toFloat() / rawBitmap.height.toFloat()
                                }
                                val screenBitmap = centerCropToAspect(rawBitmap, screenAspect)

                                scope.launch {
                                    try {
                                        val cropBitmap = withContext(Dispatchers.Default) {
                                            val centerX = screenBitmap.width / 2
                                            val centerY = screenBitmap.height / 2
                                            val text = recognizeTextOnDevice(screenBitmap)
                                            val box = findWordBoxNearPoint(text, centerX, centerY)
                                            if (box != null) {
                                                val padW = (box.width() * 0.4f).toInt().coerceAtLeast(4)
                                                val padH = (box.height() * 0.4f).toInt().coerceAtLeast(4)
                                                val left = (box.left - padW).coerceIn(0, screenBitmap.width - 1)
                                                val top = (box.top - padH).coerceIn(0, screenBitmap.height - 1)
                                                val right = (box.right + padW).coerceIn(left + 1, screenBitmap.width)
                                                val bottom = (box.bottom + padH).coerceIn(top + 1, screenBitmap.height)
                                                Bitmap.createBitmap(
                                                    screenBitmap, left, top, right - left, bottom - top
                                                )
                                            } else {
                                                val fw = (screenBitmap.width * 0.35f).toInt().coerceAtLeast(1)
                                                val fh = (screenBitmap.height * 0.12f).toInt().coerceAtLeast(1)
                                                val left = (centerX - fw / 2).coerceIn(0, screenBitmap.width - fw)
                                                val top = (centerY - fh / 2).coerceIn(0, screenBitmap.height - fh)
                                                Bitmap.createBitmap(screenBitmap, left, top, fw, fh)
                                            }
                                        }
                                        pendingCrops.add(cropBitmap)
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: t.unknownError
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            }

                            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                errorMsg = exception.message ?: t.cameraError
                                isCapturing = false
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("📷")
                }
            }
        }
    }
}
