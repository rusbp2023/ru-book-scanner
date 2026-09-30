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
