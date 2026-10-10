package com.rubookscanner.app

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Base64
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JPEG ImageProxy -> felfelé álló (elforgatott) Bitmap. A forgatás szöge megadható, alapból a CameraX értéke. */
fun imageProxyToUprightBitmap(
    image: ImageProxy,
    rotationDegrees: Int = image.imageInfo.rotationDegrees
): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } else {
        raw
    }
}

/**
 * Középre vágja a bitmapet a kívánt (szélesség/magasság) arányra.
 * Ezzel garantálható, hogy a lefotózott kép közepe ugyanaz a fizikai pont legyen,
 * mint amit a felhasználó a kijelzőn középen lát (a piros pötty/sárga keret helye).
 */
fun centerCropToAspect(bitmap: Bitmap, targetAspect: Float): Bitmap {
    if (targetAspect <= 0f) return bitmap
    val currentAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    return when {
        currentAspect > targetAspect -> {
            // a kép szélesebb a kelleténél -> vágjunk a szélességből
            val newWidth = (bitmap.height * targetAspect).toInt().coerceIn(1, bitmap.width)
            val x = (bitmap.width - newWidth) / 2
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        }
        currentAspect < targetAspect -> {
            // a kép magasabb a kelleténél -> vágjunk a magasságból
            val newHeight = (bitmap.width / targetAspect).toInt().coerceIn(1, bitmap.height)
            val y = (bitmap.height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
        else -> bitmap
    }
}

/** Bitmap -> tömörített, base64-kódolt JPEG, hogy ne legyen túl nagy az AI-nak küldött kép. */
fun bitmapToJpegBase64(bitmap: Bitmap, maxDimension: Int = 1280, quality: Int = 85): String {
    val largerSide = maxOf(bitmap.width, bitmap.height)
    val scale = maxDimension.toFloat() / largerSide
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        bitmap
    }
    val stream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

/**
 * Egyszeri (nem folyamatos) szövegfelismerés egy Bitmap-en, csak a szavak HELYÉNEK
 * megtalálásához. A cirill betűket ez a modell nem olvassa megbízhatóan, a tartalmát
 * ne használd fel, csak a boundingBox-okat.
 */
suspend fun recognizeTextOnDevice(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val input = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(input)
        .addOnSuccessListener { result -> cont.resume(result) }
        .addOnFailureListener { e -> cont.resumeWithException(e) }
}

/** Megkeresi a (centerX, centerY) pont alatti, vagy ahhoz legközelebbi felismert szó dobozát. */
fun findWordBoxNearPoint(text: Text, centerX: Int, centerY: Int): Rect? {
    val elements = text.textBlocks.flatMap { block -> block.lines.flatMap { it.elements } }
    if (elements.isEmpty()) return null

    val containing = elements.filter { el -> el.boundingBox?.contains(centerX, centerY) == true }
    if (containing.isNotEmpty()) {
        return containing.minByOrNull { el ->
            val box = el.boundingBox!!
            box.width().toLong() * box.height().toLong()
        }?.boundingBox
    }

    return elements.minByOrNull { el ->
        val box = el.boundingBox
        if (box == null) {
            Long.MAX_VALUE
        } else {
            val dx = (box.centerX() - centerX).toLong()
            val dy = (box.centerY() - centerY).toLong()
            dx * dx + dy * dy
        }
    }?.boundingBox
}

/**
 * Azok a kivágások, amelyek többszavas KIFEJEZÉST tartalmaznak. Csak az AI-nak szóló kérésnél kell
 * (hogy tudja, mely képeken kell a teljes szöveget kiolvasni). Gyenge hivatkozású halmaz, így a
 * törölt képek maguktól kikerülnek belőle.
 */
val phraseCrops: MutableSet<Bitmap> = java.util.Collections.synchronizedSet(
    java.util.Collections.newSetFromMap(java.util.WeakHashMap<Bitmap, Boolean>())
)

/**
 * A téglalaphoz legközelebbi SORBÓL azokat a szavakat adja vissza (balról jobbra), amelyeknek
 * legalább a fele (szélességben) a téglalapba esik. Üres lista, ha nincs ilyen.
 */
fun findWordBoxesInRect(text: Text, rect: Rect, minOverlap: Float = 0.5f): List<Rect> {
    val lines = text.textBlocks.flatMap { it.lines }
    if (lines.isEmpty()) return emptyList()
    val cx = rect.centerX()
    val cy = rect.centerY()

    fun distSq(box: Rect): Long {
        val dx = maxOf(box.left - cx, 0, cx - box.right).toLong()
        val dy = maxOf(box.top - cy, 0, cy - box.bottom).toLong()
        return dx * dx + dy * dy
    }

    val bestLine = lines.minByOrNull { line ->
        line.elements.mapNotNull { it.boundingBox }.minOfOrNull { distSq(it) } ?: Long.MAX_VALUE
    } ?: return emptyList()

    return bestLine.elements.mapNotNull { el ->
        val box = el.boundingBox ?: return@mapNotNull null
        val overlap = minOf(box.right, rect.right) - maxOf(box.left, rect.left)
        if (box.width() > 0 && overlap > 0 && overlap.toFloat() / box.width() >= minOverlap) box else null
    }.sortedBy { it.left }
}

/** A megadott dobozok közös befoglaló doboza. */
fun unionBox(boxes: List<Rect>): Rect {
    val u = Rect(boxes.first())
    boxes.drop(1).forEach { u.union(it) }
    return u
}
