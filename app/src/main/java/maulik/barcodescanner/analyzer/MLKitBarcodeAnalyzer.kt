package maulik.barcodescanner.analyzer

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import maulik.barcodescanner.ui.custom.ViewFinderOverlay
import maulik.barcodescanner.util.crop
import maulik.barcodescanner.util.debug
import maulik.barcodescanner.util.rotate
import maulik.barcodescanner.util.toBitmap
import java.io.ByteArrayOutputStream

const val PORTRAIT_DEGREES = 90

class MLKitBarcodeAnalyzer(
    private val listener: ScanningResultListener,
    private val overlay: ViewFinderOverlay,
    private val cropPreview: ImageView,
) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false


    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanning) {

            val bitmap = mediaImage.toBitmap(overlay.context)
            val croppedByViewPort = bitmap.crop(imageProxy.cropRect)
            val rotated = croppedByViewPort.rotate(PORTRAIT_DEGREES.toFloat())
            val croppedByBarcodeFinder = cropBitmap(rotated, overlay, overlay.boxRect!!.toRect())
            val image = InputImage.fromBitmap(croppedByBarcodeFinder, 90)
            val scanner = BarcodeScanning.getClient()

            cropPreview.post {
                cropPreview.setImageBitmap(croppedByBarcodeFinder)
            } // TODO

            debug(imageProxy.cropRect.toShortString())
            debug("bitmap --- w - ${bitmap.width} --- h - ${bitmap.height} ")
            debug("image --- w - ${image.width} --- h - ${image.height} ")

            isScanning = true
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes?.firstOrNull().let { barcode ->
                        val rawValue = barcode?.rawValue
                        rawValue?.let {
                            Log.d("Barcode", it)
                            listener.onScanned(it)
                        }
                    }
                    isScanning = false
                    imageProxy.close()
                }
                .addOnFailureListener {
                    isScanning = false
                    imageProxy.close()
                }
        }
    }

    // bitmap - image from camera
    // frame - camera preview (usually phone screen resolution)
    // cropArea - crop view finder (Qr or BarCode)
    fun cropBitmap(bitmap: Bitmap, frame: View, cropArea: Rect): Bitmap {
        val frameHeight = frame.height
        val frameWidth = frame.width
        val cropHeight = cropArea.height()
        val cropWidth = cropArea.width()
        val cropLeft = cropArea.left
        val cropTop = cropArea.top
        val imageHeight = bitmap.height
        val imageWidth = bitmap.width
        val widthFinal = cropWidth * imageWidth / frameWidth
        val heightFinal = cropHeight * imageHeight / frameHeight
        val leftFinal = cropLeft * imageWidth / frameWidth
        val topFinal = cropTop * imageHeight / frameHeight
        val bitmapFinal = Bitmap.createBitmap(
            bitmap,
            leftFinal, topFinal, widthFinal, heightFinal
        )

        return bitmapFinal
    }

    // bitmap - image from camera
    // frame - camera preview (usually phone screen resolution)
    // cropArea - crop view finder (Qr or BarCode)
    fun cropBitmap(bitmap: Bitmap, frame: View, cropArea: View): Bitmap {
        val frameHeight = frame.height
        val frameWidth = frame.width
        val cropHeight = cropArea.height
        val cropWidth = cropArea.width
        val cropLeft = cropArea.left
        val cropTop = cropArea.top
        val imageHeight = bitmap.height
        val imageWidth = bitmap.width
        val widthFinal = cropWidth * imageWidth / frameWidth
        val heightFinal = cropHeight * imageHeight / frameHeight
        val leftFinal = cropLeft * imageWidth / frameWidth
        val topFinal = cropTop * imageHeight / frameHeight
        val bitmapFinal = Bitmap.createBitmap(
            bitmap,
            leftFinal, topFinal, widthFinal, heightFinal
        )

        return bitmapFinal
    }

}