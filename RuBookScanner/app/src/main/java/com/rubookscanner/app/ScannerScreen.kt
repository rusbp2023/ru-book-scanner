package com.rubookscanner.app

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
    val imageCapture = remember { ImageCapture.Builder().build() }

    var isProcessing by remember { mutableStateOf(false) }
    var recognizedCard by remember { mutableStateOf<Flashcard?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cameraProvider = awaitCameraProvider(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Piros, áttetsző pötty a képernyő közepén
        Box(
            Modifier
                .align(Alignment.Center)
                .size(22.dp)
                .background(Color.Red.copy(alpha = 0.55f), CircleShape)
        )

        Text(
            "Állj a szó fölé, hogy a piros pötty pontosan rajta legyen, majd nyomd meg a gombot.",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

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
                            notFound -> Text("Nem találtam szót a pötty alatt. Próbáld közelebbről vagy élesebben.")
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
                    if (settings.apiKey.isBlank()) {
                        errorMsg = "Előbb add meg az API kulcsot a Beállításoknál!"
                        recognizedCard = null
                        notFound = false
                        return@Button
                    }
                    isProcessing = true
                    recognizedCard = null
                    notFound = false
                    errorMsg = null
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val bitmap = imageProxyToUprightBitmap(image)
                                image.close()
                                scope.launch {
                                    try {
                                        val base64 = bitmapToJpegBase64(bitmap)
                                        val client = AiClient(settings)
                                        val card = withContext(Dispatchers.IO) {
                                            client.lookupWordFromImage(base64)
                                        }
                                        if (card.original.isBlank()) {
                                            notFound = true
                                        } else {
                                            recognizedCard = card
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "ismeretlen hiba"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
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

private suspend fun awaitCameraProvider(context: android.content.Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
