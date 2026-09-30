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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
fun ScannerScreen(settings: AiSettings, onFlashcardAccepted: (Flashcard) -> Unit) {
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
        CameraScanContent(settings = settings, onFlashcardAccepted = onFlashcardAccepted)
    } else {
        PermissionRequiredScreen(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
    }
}

@Composable
private fun PermissionRequiredScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("A szófelismeréshez szükség van a kamera engedélyre.")
            Button(onClick = onRequest) {
                Text("Kamera engedélyezése")
            }
        }
    }
}

@Composable
private fun CameraScanContent(settings: AiSettings, onFlashcardAccepted: (Flashcard) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }

    var testMode by remember { mutableStateOf(true) }
    var testCropPreview by remember { mutableStateOf<ImageBitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var recognizedCard by remember { mutableStateOf<Flashcard?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }

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
                .size(40.dp)
                .border(2.dp, Color.Yellow, CircleShape)
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Célozd a sárga körrel a szót, majd nyomd meg a gombot.",
                color = Color.White
            )
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Teszt mód (AI nélkül)", color = Color.White)
                Switch(checked = testMode, onCheckedChange = {
                    testMode = it
                    testCropPreview = null
                })
            }
            if (testMode && testCropPreview != null) {
                Card(Modifier.padding(top = 8.dp)) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ez a rész menne az AI-nak:", style = MaterialTheme.typography.labelMedium)
                        Image(
                            bitmap = testCropPreview!!,
                            contentDescription = null,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (recognizedCard != null || notFound || errorMsg != null) {
                Card(Modifier.padding(bottom = 16.dp)) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            errorMsg != null -> Text("Hiba: $errorMsg")
                            notFound -> Text("Nem találtam szót a kör helyén. Próbáld közelebbről vagy élesebben.")
                            recognizedCard != null -> {
                                val card = recognizedCard!!
                                Text(card.dictionaryForm, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "(eredeti: ${card.original})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(card.translation, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    Modifier.padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(onClick = {
                                        recognizedCard = null
                                        notFound = false
                                        errorMsg = null
                                    }) { Text("Elvetés") }
                                    Button(onClick = {
                                        onFlashcardAccepted(card)
                                        recognizedCard = null
                                    }) { Text("Hozzáadás a kártyákhoz") }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (!testMode && settings.apiKey.isBlank()) {
                        errorMsg = "Előbb add meg az API kulcsot a Beállításoknál!"
                        recognizedCard = null
                        notFound = false
                        return@Button
                    }
                    isProcessing = true
                    recognizedCard = null
                    notFound = false
                    errorMsg = null
                    testCropPreview = null

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
                                                // Nem talált szót -> egy ésszerű méretű terület a kör körül
                                                val fw = (screenBitmap.width * 0.35f).toInt().coerceAtLeast(1)
                                                val fh = (screenBitmap.height * 0.12f).toInt().coerceAtLeast(1)
                                                val left = (centerX - fw / 2).coerceIn(0, screenBitmap.width - fw)
                                                val top = (centerY - fh / 2).coerceIn(0, screenBitmap.height - fh)
                                                Bitmap.createBitmap(screenBitmap, left, top, fw, fh)
                                            }
                                        }

                                        if (testMode) {
                                            testCropPreview = cropBitmap.asImageBitmap()
                                        } else {
                                            val base64 = bitmapToJpegBase64(cropBitmap)
                                            val client = AiClient(settings)
                                            val card = withContext(Dispatchers.IO) {
                                                client.lookupWordFromImage(base64)
                                            }
                                            if (card.original.isBlank()) {
                                                notFound = true
                                            } else {
                                                recognizedCard = card
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "ismeretlen hiba"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }

                            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                errorMsg = exception.message ?: "kamera hiba"
                                isProcessing = false
                            }
                        }
                    )
                },
                enabled = !isProcessing,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("📷")
                }
            }
        }
    }
}
