package com.rubookscanner.app

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JPEG ImageProxy -> felfelé álló (elforgatott) Bitmap. */
fun imageProxyToUprightBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } else {
        raw
    }
}

/** Cirill szövegfelismerés futtatása egy Bitmap-en (suspend, IO/CPU munka, hívd háttérszálról). */
suspend fun recognizeCyrillicText(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val input = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(input)
        .addOnSuccessListener { result -> cont.resume(result) }
        .addOnFailureListener { e -> cont.resumeWithException(e) }
}

/**
 * Megkeresi, melyik felismert szó van a (centerX, centerY) pont alatt vagy ahhoz legközelebb.
 * A koordináták ugyanabban a koordináta-rendszerben értendők, mint amiből a Text jött
 * (vagyis a felfelé álló bitmap pixelkoordinátái).
 */
fun findWordNearPoint(text: Text, centerX: Int, centerY: Int): String? {
    val elements = text.textBlocks.flatMap { block -> block.lines.flatMap { it.elements } }
    if (elements.isEmpty()) return null

    val containing = elements.filter { el -> el.boundingBox?.contains(centerX, centerY) == true }
    if (containing.isNotEmpty()) {
        return containing.minByOrNull { el ->
            val box = el.boundingBox!!
            box.width().toLong() * box.height().toLong()
        }?.text
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
    }?.text
}
