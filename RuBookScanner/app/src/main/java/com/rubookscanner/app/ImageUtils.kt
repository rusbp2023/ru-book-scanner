package com.rubookscanner.app

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

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
