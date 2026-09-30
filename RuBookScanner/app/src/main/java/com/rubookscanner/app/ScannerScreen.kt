package com.rubookscanner.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

/**
 * Egy jelölt szó doboza, amit az élő elemzés talált a képernyő közepe körül.
 * Csak a POZÍCIÓ megbízható belőle — a cirill betűket az élő (Latin) modell
 * gyakran félreolvassa, ezért a végleges szót és fordítást gombnyomásra
 * mindig egy fotóból, AI-vízióval kérjük le, nem ebből.
 */
private data class TrackedBox(val rect: Rect)

@Composable
private fun CameraScanContent(settings: AiSettings, onFlashcardAccepted: (Flashcard) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var trackedBox by remember { mutableStateOf<TrackedBox?>(null) }
    var lastTrackUpdateMs by remember { mutableStateOf(0L) }

    var isProcessing by remember { mutableStateOf(false) }
    var recognizedCard by remember { mutableStateOf<Flashcard?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cameraController.setEnabledUseCases(
            CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
        )
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            MlKitAnalyzer(
                listOf(textRecognizer),
                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(context)
            ) { result ->
                val now = System.currentTimeMillis()
                if (now - lastTrackUpdateMs < 250) {
                    return@MlKitAnalyzer
                }
                lastTrackUpdateMs = now

                val text = result.getValue(textRecognizer)
                val elements = text?.textBlocks?.flatMap { block -> block.lines.flatMap { it.elements } }
                if (elements.isNullOrEmpty()) {
                    trackedBox = null
                } else {
                    val centerX = previewView.width / 2
                    val centerY = previewView.height / 2
                    val containing = elements.filter { it.boundingBox?.contains(centerX, centerY) == true }
                    val best = if (containing.isNotEmpty()) {
                        containing.minByOrNull { el ->
                            val box = el.boundingBox!!
                            box.width().toLong() * box.height().toLong()
                        }
                    } else {
                        elements.minByOrNull { el ->
                            val box = el.boundingBox
                            if (box == null) {
                                Long.MAX_VALUE
                            } else {
                                val dx = (box.centerX() - centerX).toLong()
                                val dy = (box.centerY() - centerY).toLong()
                                dx * dx + dy * dy
                            }
                        }
                    }
                    val bestBox = best?.boundingBox
                    trackedBox = if (bestBox != null) TrackedBox(bestBox) else null
                }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Halvány, fix piros pötty a képernyő közepén (tájékoztató referenciapont)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(10.dp)
                .background(Color.Red.copy(alpha = 0.5f), CircleShape)
        )

        // Dinamikus, sárga keret a jelenleg középen lévő szó körül
        trackedBox?.let { tracked ->
            val leftDp = with(density) { tracked.rect.left.toDp() }
            val topDp = with(density) { tracked.rect.top.toDp() }
            val widthDp = with(density) { tracked.rect.width().toDp() }
            val heightDp = with(density) { tracked.rect.height().toDp() }
            Box(
                Modifier
                    .offset(x = leftDp, y = topDp)
                    .size(width = widthDp, height = heightDp)
                    .border(2.dp, Color.Yellow, RoundedCornerShape(6.dp))
            )
        }

        Text(
            "Mozgasd a telefont, hogy a sárga keret a kívánt szón legyen, majd nyomd meg a gombot.",
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
                            notFound -> Text("Nem találtam szót a keret helyén. Próbáld közelebbről vagy élesebben.")
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
                                val bitmap = centerCropToAspect(rawBitmap, screenAspect)
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
