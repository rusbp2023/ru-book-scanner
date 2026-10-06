package com.rubookscanner.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.Flashcard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A kivágott téglalap a képernyő méretéhez viszonyítva (0..1 közötti arányok). */
private data class ScanHighlight(val l: Float, val t: Float, val r: Float, val b: Float)

/** Tartalék téglalap a képernyő közepén (hibánál ezen rezeg az animáció). */
private val EstimateRect = ScanHighlight(0.325f, 0.44f, 0.675f, 0.56f)

/** A scannelés kimenetele: OK = szó megtalálva, MISS = nem talált szót (tartalék kivágás), ERROR = hiba. */
private enum class ScanResult { OK, MISS, ERROR }

/** Szín párok a végső körbefutó fényhez: (izzás, fényes). */
private val OkColors = Color(0xFF66BB6A) to Color(0xFFC8E6C9)
private val MissColors = Color(0xFFFFB74D) to Color(0xFFFFE0B2)

/** A középső célzó négyzet színe. */
private val AimColor = Color(0xFFFFF176)

/** Egy fényporszem: melyik sarokból indul, merre, milyen gyorsan, mekkora. */
private data class Particle(
    val corner: Int,
    val angle: Float,
    val speed: Float,
    val radius: Float,
    val green: Boolean
)

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
            WButton(onClick = onRequest) {
                WLabel(t.allowCamera)
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

    val listState = rememberLazyListState()
    LaunchedEffect(pendingCrops.size) {
        if (pendingCrops.isNotEmpty()) {
            listState.animateScrollToItem(pendingCrops.size - 1)
        }
    }

    // --- Scan animáció állapota ---
    var scanActive by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf(false) }
    var scanTarget by remember { mutableStateOf(EstimateRect) }
    var runnerColors by remember { mutableStateOf(OkColors) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    val frame = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val runner = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    
    // A célzó négyzet körüli pulzáló keret, amíg a keresés tart.
    val pulseTransition = rememberInfiniteTransition(label = "searchPulse")
    val pulse = pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val particles = remember {
        val bases = listOf(-2.356f, -0.785f, 2.356f, 0.785f) // kifelé mutató irányok a 4 sarokból
        List(28) { i ->
            Particle(
                corner = i % 4,
                angle = bases[i % 4] + (Random.nextFloat() - 0.5f) * 2.4f,
                speed = 0.5f + Random.nextFloat(),
                radius = 1.5f + Random.nextFloat() * 2f,
                green = i % 2 == 0
            )
        }
    }

    /** Ha megvan a valódi szóhely: felizzik a keret, letapogatás, körbefutó jelzés, részecskék. */
    fun finishScanAnim(target: ScanHighlight?, result: ScanResult) {
        animJob?.cancel()
        scanActive = true
        animJob = scope.launch {
            scanTarget = target ?: EstimateRect
            scanError = result == ScanResult.ERROR
            frame.snapTo(0f)
            sweep.snapTo(0f)
            runner.snapTo(0f)
            burst.snapTo(0f)
            shake.snapTo(0f)
            fade.snapTo(1f)
            if (result == ScanResult.ERROR) {
                frame.snapTo(1f)
                shake.animateTo(1f, tween(380, easing = LinearEasing))
                fade.animateTo(0f, tween(160))
                scanActive = false
                scanError = false
                return@launch
            }
            runnerColors = if (result == ScanResult.OK) OkColors else MissColors
            launch { frame.animateTo(1f, tween(100)) }
            sweep.animateTo(1f, tween(200, easing = LinearEasing))
            val burstJob = if (result == ScanResult.OK) {
                launch { burst.animateTo(1f, tween(450, easing = LinearEasing)) }
            } else {
                null
            }
            runner.animateTo(1.3f, tween(180, easing = LinearEasing))
            fade.animateTo(0f, tween(160))
            burstJob?.join()
            scanActive = false
        }
    }

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

        // Statikus, üres sárga téglalap a képernyő közepén (vízszintes) — ide célozd a szót.
                // A téglalap csak akkor látszik, amikor nem fut a scan animáció.
        if (!scanActive) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 52.dp, height = 26.dp)
                    .border(2.dp, AimColor, RectangleShape)
            )
            // Keresés közben halvány keret pulzál a téglalap körül.
            if (isCapturing) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 52.dp, height = 26.dp)
                        .graphicsLayer {
                            val s = 1.3f + 0.7f * pulse.value
                            scaleX = s
                            scaleY = s
                            alpha = 0.7f - 0.5f * pulse.value
                        }
                        .border(2.dp, AimColor, RectangleShape)
                )
            }
        }

        if (scanActive) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val hh = size.height
                // hiba esetén a téglalap oldalirányban rezeg
                val dx = if (scanError) {
                    sin(shake.value * PI.toFloat() * 7f) * (1f - shake.value) * 12.dp.toPx()
                } else {
                    0f
                }
                val l = scanTarget.l * w + dx
                val tp = scanTarget.t * hh
                val r = scanTarget.r * w + dx
                val b = scanTarget.b * hh
                val glow = if (scanError) Color(0xFFEF5350) else Color(0xFFFFEB3B)
                val bright = if (scanError) Color(0xFFFFCDD2) else Color(0xFFFFF9C4)
                val a = fade.value

                // 1) a téglalap keretének felizzása
                val f = frame.value
                if (f > 0f) {
                    drawRect(glow, Offset(l, tp), Size(r - l, b - tp), alpha = 0.10f * f * a)
                    drawRect(
                        glow, Offset(l, tp), Size(r - l, b - tp),
                        alpha = 0.35f * f * a, style = Stroke(width = 8.dp.toPx())
                    )
                    drawRect(
                        bright, Offset(l, tp), Size(r - l, b - tp),
                        alpha = f * a, style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 2) halvány kék letapogató vonal felülről lefelé
                val s = sweep.value
                if (s > 0f) {
                    val y = tp + (b - tp) * s
                    val trailTop = maxOf(tp, y - 36.dp.toPx())
                    if (y - trailTop > 1f) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, glow),
                                startY = trailTop,
                                endY = y
                            ),
                            topLeft = Offset(l, trailTop),
                            size = Size(r - l, y - trailTop),
                            alpha = 0.45f * a
                        )
                    }
                    drawLine(bright, Offset(l, y), Offset(r, y), strokeWidth = 2.dp.toPx(), alpha = 0.9f * a)
                }

                // 3) eredmény-jelzés: zöld (siker) vagy narancs (nem talált szót) fény fut körbe a kereten
                val q = runner.value
                if (q > 0f) {
                    val (runGlow, runBright) = runnerColors
                    val rectPath = Path().apply { addRect(Rect(l, tp, r, b)) }
                    val pm = PathMeasure()
                    pm.setPath(rectPath, false)
                    val len = pm.length
                    val head = minOf(q, 1f) * len
                    val tail = maxOf(0f, q - 0.3f) * len
                    if (head > tail) {
                        val seg = Path()
                        pm.getSegment(tail, head, seg, true)
                        drawPath(
                            seg, runGlow, alpha = 0.45f * a,
                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            seg, runBright, alpha = a,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    val pulse = sin(PI.toFloat() * (q / 1.3f)).coerceAtLeast(0f)
                    drawRect(
                        runGlow, Offset(l, tp), Size(r - l, b - tp),
                        alpha = 0.5f * pulse * a, style = Stroke(width = 4.dp.toPx())
                    )
                }

                // 4) fényporszemek a téglalap sarkaiból
                val bp = burst.value
                if (bp > 0f && bp < 1f) {
                    val e = 1f - (1f - bp) * (1f - bp)
                    val corners = listOf(Offset(l, tp), Offset(r, tp), Offset(l, b), Offset(r, b))
                    val dist = 46.dp.toPx()
                    particles.forEach { pt ->
                        val c = corners[pt.corner]
                        val pos = Offset(
                            c.x + cos(pt.angle) * pt.speed * dist * e,
                            c.y + sin(pt.angle) * pt.speed * dist * e
                        )
                        drawCircle(
                            color = if (pt.green) Color(0xFFA5D6A7) else Color(0xFFFFF9C4),
                            radius = pt.radius.dp.toPx() * (1f - 0.6f * bp),
                            center = pos,
                            alpha = 1f - bp
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
                            state = listState,
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
                                    ) { Text("✕", color = AimColor) }
                                }
                            }
                        }
                        Row(
                            Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WButton(
                                onClick = { pendingCrops.clear() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                WLabel(t.clearList)
                            }
                            WButton(
                                enabled = !isTranslating,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    if (settings.apiKey.isBlank()) {
                                        errorMsg = t.enterApiKeyFirst
                                        return@WButton
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
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    WLabel(t.translateN(pendingCrops.size))
                                }
                            }
                        }
                    }
                }
            }

            WButton(
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
                                        var newHighlight: ScanHighlight? = null
                                        var wordFound = false
                                        val cropBitmap = withContext(Dispatchers.Default) {
                                            val centerX = screenBitmap.width / 2
                                            val centerY = screenBitmap.height / 2
                                            val text = recognizeTextOnDevice(screenBitmap)
                                            val box = findWordBoxNearPoint(text, centerX, centerY)
                                            if (box != null) {
                                                wordFound = true
                                                val padW = (box.width() * 0.4f).toInt().coerceAtLeast(4)
                                                val padH = (box.height() * 0.4f).toInt().coerceAtLeast(4)
                                                val left = (box.left - padW).coerceIn(0, screenBitmap.width - 1)
                                                val top = (box.top - padH).coerceIn(0, screenBitmap.height - 1)
                                                val right = (box.right + padW).coerceIn(left + 1, screenBitmap.width)
                                                val bottom = (box.bottom + padH).coerceIn(top + 1, screenBitmap.height)
                                                val mx = (box.width() * 0.06f).toInt().coerceAtLeast(2)
                                                val my = (box.height() * 0.10f).toInt().coerceAtLeast(2)
                                                newHighlight = ScanHighlight(
                                                    (box.left - mx).coerceAtLeast(0).toFloat() / screenBitmap.width,
                                                    (box.top - my).coerceAtLeast(0).toFloat() / screenBitmap.height,
                                                    (box.right + mx).coerceAtMost(screenBitmap.width).toFloat() / screenBitmap.width,
                                                    (box.bottom + my).coerceAtMost(screenBitmap.height).toFloat() / screenBitmap.height
                                                )
                                                Bitmap.createBitmap(
                                                    screenBitmap, left, top, right - left, bottom - top
                                                )
                                            } else {
                                                val fw = (screenBitmap.width * 0.35f).toInt().coerceAtLeast(1)
                                                val fh = (screenBitmap.height * 0.12f).toInt().coerceAtLeast(1)
                                                val left = (centerX - fw / 2).coerceIn(0, screenBitmap.width - fw)
                                                val top = (centerY - fh / 2).coerceIn(0, screenBitmap.height - fh)
                                                newHighlight = ScanHighlight(
                                                    left.toFloat() / screenBitmap.width,
                                                    top.toFloat() / screenBitmap.height,
                                                    (left + fw).toFloat() / screenBitmap.width,
                                                    (top + fh).toFloat() / screenBitmap.height
                                                )
                                                Bitmap.createBitmap(screenBitmap, left, top, fw, fh)
                                            }
                                        }
                                        pendingCrops.add(cropBitmap)
                                        finishScanAnim(
                                            newHighlight,
                                            if (wordFound) ScanResult.OK else ScanResult.MISS
                                        )
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: t.unknownError
                                        finishScanAnim(null, ScanResult.ERROR)
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            }

                            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                errorMsg = exception.message ?: t.cameraError
                                isCapturing = false
                                finishScanAnim(null, ScanResult.ERROR)
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    Text("📷", fontSize = 38.sp)
                }
            }
        }
    }
}
