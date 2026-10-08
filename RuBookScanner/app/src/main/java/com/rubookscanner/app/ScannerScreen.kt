package com.rubookscanner.app

import android.Manifest
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import com.rubookscanner.app.data.Deck
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.hypot
import kotlin.math.roundToInt
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
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
    onFlashcardsAccepted: (List<Flashcard>) -> Unit,
    onButtonPosChange: (Float, Float) -> Unit,
    decks: List<Deck>,
    activeDeckId: Long?,
    onSelectDeck: (Long) -> Unit,
    onHideInfo: () -> Unit
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
            onFlashcardsAccepted = onFlashcardsAccepted,
            onButtonPosChange = onButtonPosChange,
            decks = decks,
            activeDeckId = activeDeckId,
            onSelectDeck = onSelectDeck,
            onHideInfo = onHideInfo
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
    onFlashcardsAccepted: (List<Flashcard>) -> Unit,
    onButtonPosChange: (Float, Float) -> Unit,
    decks: List<Deck>,
    activeDeckId: Long?,
    onSelectDeck: (Long) -> Unit,
    onHideInfo: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val t = LocalStrings.current
    val previewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }
    var torchOn by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { cameraController.enableTorch(false) }
    }

    var isCapturing by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- freely movable capture button ---
    val density = LocalDensity.current
    var boxW by remember { mutableStateOf(0f) }
    var boxH by remember { mutableStateOf(0f) }
    var panelH by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var localPos by remember { mutableStateOf<Offset?>(null) }
    var longPressed by remember { mutableStateOf(false) }
    val buttonXSetting by rememberUpdatedState(settings.buttonX)
    val buttonYSetting by rememberUpdatedState(settings.buttonY)
    val onPosChange by rememberUpdatedState(onButtonPosChange)
    val haptic = LocalHapticFeedback.current
    val btnPx = with(density) { 80.dp.toPx() }
    val sidePx = with(density) { 8.dp.toPx() }
    val topLimitPx = with(density) { 60.dp.toPx() }
    val bottomMarginPx = with(density) { 16.dp.toPx() }

    LaunchedEffect(settings.buttonX, settings.buttonY) { localPos = null }

    fun xRange(): Float = maxOf(0f, boxW - 2 * sidePx - btnPx)
    fun yRange(): Float = maxOf(0f, boxH - bottomMarginPx - btnPx - topLimitPx)
    /** Lowest allowed top edge: above the collected-words / error panel when it is visible. */
    fun maxYNow(): Float {
        val panelVisible = pendingCrops.isNotEmpty() || errorMsg != null
        val reserve = if (panelVisible) panelH else bottomMarginPx
        return maxOf(topLimitPx, boxH - reserve - btnPx)
    }
    fun renderPos(): Offset {
        if (dragging) return Offset(dragX, dragY.coerceIn(topLimitPx, maxYNow()))
        val frac = localPos ?: Offset(buttonXSetting, buttonYSetting)
        val x = sidePx + frac.x.coerceIn(0f, 1f) * xRange()
        val y = topLimitPx + frac.y.coerceIn(0f, 1f) * yRange()
        return Offset(x, y.coerceIn(topLimitPx, maxYNow()))
    }

    val btnPos = renderPos()
    val btnX = btnPos.x
    val btnY = btnPos.y

    // The aiming rectangle shifts a little away from the button: button on the left -> rectangle moves right.
    val cxFrac = if (boxW > 0f) (btnX + btnPx / 2f) / boxW else 0.5f
    val tShift = (0.5f - cxFrac) / 0.5f
    val sShift = if (abs(tShift) < 0.2f) 0f else sign(tShift) * (abs(tShift) - 0.2f) / 0.8f
    val aimOffsetDp = (32f * sShift).dp
    val aimOffsetAnimated by animateDpAsState(aimOffsetDp, tween(150), label = "aimOffset")
    /** Shift of the rectangle centre relative to the screen width (the crop follows it). */
    fun aimShift(): Float =
        if (previewView.width > 0) with(density) { aimOffsetDp.toPx() } / previewView.width else 0f

    fun finishDrag() {
        longPressed = false
        val xr = xRange()
        val yr = yRange()
        val fx = if (xr > 0f) ((dragX - sidePx) / xr).coerceIn(0f, 1f) else 0.5f
        val fy = if (yr > 0f) ((dragY.coerceIn(topLimitPx, maxYNow()) - topLimitPx) / yr).coerceIn(0f, 1f) else 1f
        localPos = Offset(fx, fy)
        dragging = false
        onPosChange(fx, fy)
    }

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
            val sh = aimShift()
            scanTarget = target ?: ScanHighlight(
                EstimateRect.l + sh, EstimateRect.t, EstimateRect.r + sh, EstimateRect.b
            )
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

    val captureButton: @Composable (Modifier) -> Unit = { mod ->
            WButton(
                onClick = {
                    if (longPressed) {
                        longPressed = false
                        return@WButton
                    }
                    isCapturing = true
                    errorMsg = null
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : androidx.camera.core.ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val displayRot = previewView.display?.rotation
                                    ?: android.view.Surface.ROTATION_0
                                val rot = cameraController.cameraInfo
                                    ?.getSensorRotationDegrees(displayRot)
                                    ?: image.imageInfo.rotationDegrees
                                val rawBitmap = imageProxyToUprightBitmap(image, rot)
                                image.close()
                                val screenAspect = if (previewView.height > 0) {
                                    previewView.width.toFloat() / previewView.height.toFloat()
                                } else {
                                    rawBitmap.width.toFloat() / rawBitmap.height.toFloat()
                                }
                                val screenBitmap = centerCropToAspect(rawBitmap, screenAspect)
                                val shiftFrac = aimShift()

                                scope.launch {
                                    try {
                                        var newHighlight: ScanHighlight? = null
                                        var wordFound = false
                                        val cropBitmap = withContext(Dispatchers.Default) {
                                            val centerX = (screenBitmap.width * (0.5f + shiftFrac)).toInt()
                                                .coerceIn(0, screenBitmap.width - 1)
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
                modifier = mod.size(80.dp),
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

    LaunchedEffect(Unit) {
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // top row: deck picker (left), flashlight (center), info (right)
        DeckPicker(
            decks = decks,
            activeDeckId = activeDeckId,
            onSelect = onSelectDeck,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 16.dp)
                .graphicsLayer { alpha = 0.85f },
            maxWidth = 132.dp
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (torchOn) Color(0xCCFFD600) else Color(0x66000000))
                .border(
                    1.dp,
                    if (torchOn) Color(0xFFFFF59D) else Color(0x66FFFFFF),
                    CircleShape
                )
                .clickable {
                    torchOn = !torchOn
                    cameraController.enableTorch(torchOn)
                },
            contentAlignment = Alignment.Center
        ) { Text("🔦", fontSize = 20.sp) }
        if (settings.showInfo) {
            InfoButton(
                onClick = { showInfo = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp)
            )
        }
        if (showInfo) {
            InfoDialog(
                body = t.infoBody,
                onDismiss = { showInfo = false },
                onDontShowAgain = {
                    showInfo = false
                    onHideInfo()
                }
            )
        }

        // Statikus, üres sárga téglalap a képernyő közepén (vízszintes) — ide célozd a szót.
                // A téglalap csak akkor látszik, amikor nem fut a scan animáció.
        if (!scanActive) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = aimOffsetAnimated)
                    .size(width = 52.dp, height = 26.dp)
                    .border(2.dp, AimColor, RectangleShape)
            )
            // Keresés közben halvány keret pulzál a téglalap körül.
            if (isCapturing) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = aimOffsetAnimated)
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
                .onSizeChanged { panelH = it.height.toFloat() }
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
        }

        // the capture button: long press, then drag it anywhere (outside the top icons and the bottom panel)
        if (boxW > 0f) {
            captureButton(
                Modifier
                    .offset { IntOffset(btnX.roundToInt(), btnY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                val p = renderPos()
                                dragX = p.x
                                dragY = p.y
                                longPressed = true
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragX = (dragX + amount.x).coerceIn(sidePx, maxOf(sidePx, boxW - sidePx - btnPx))
                                dragY = (dragY + amount.y).coerceIn(topLimitPx, maxYNow())
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    }
                    .graphicsLayer {
                        val sc = if (dragging) 1.12f else 1f
                        scaleX = sc
                        scaleY = sc
                        alpha = if (dragging) 0.9f else 1f
                    }
            )
        }
    }
}
