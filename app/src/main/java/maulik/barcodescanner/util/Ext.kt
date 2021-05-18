package maulik.barcodescanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.util.Log
import android.view.View
import androidx.core.graphics.toRect

fun Any.debug(string: String) = Log.d("${this.javaClass::getSimpleName}__DEB", "$string")

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}


fun Bitmap.crop(cropRectF: RectF, viewFinder: View): Bitmap = crop(cropRectF.toRect(), viewFinder)

// cropRect is barcode finder frame rect
// viewFinder is camera view finder
fun Bitmap.crop(cropRect: Rect, viewFinder: View): Bitmap {
    val ratioScreenToImageWidth = (width / viewFinder.width.toFloat())
    val ratioScreenToImageHeight = (height / viewFinder.height.toFloat())

    val left = cropRect.left * ratioScreenToImageWidth
    val top = cropRect.top * ratioScreenToImageHeight
    val width = (cropRect.right - cropRect.left) * ratioScreenToImageWidth
    val height = (cropRect.bottom - cropRect.top) * ratioScreenToImageHeight
    return Bitmap.createBitmap(
        this,
        left.toInt(),
        top.toInt(),
        width.toInt(),
        height.toInt()
    )
}

fun Bitmap.crop(cropRect: Rect): Bitmap {
    val left = cropRect.left
    val top = cropRect.top
    val width = cropRect.right - cropRect.left
    val height = cropRect.bottom - cropRect.top
    return Bitmap.createBitmap(
        this,
        left.toInt(),
        top.toInt(),
        width.toInt(),
        height.toInt()
    )
}

fun Image.toBitmap(context: Context): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    YuvToRgbConverter(context).yuvToRgb(this, bitmap)
    return bitmap
}
