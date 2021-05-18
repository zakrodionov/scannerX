package maulik.barcodescanner.analyzer

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
            val croppedByPreview = bitmap.crop(imageProxy.cropRect)
            val rotated = croppedByPreview.rotate(PORTRAIT_DEGREES.toFloat())
            val croppedByBarcodeFinder = rotated.crop(overlay.boxRect!!.toRect(), overlay)
            val image = InputImage.fromBitmap(croppedByBarcodeFinder, PORTRAIT_DEGREES)
            val scanner = BarcodeScanning.getClient()

            cropPreview.post {
                cropPreview.setImageBitmap(croppedByBarcodeFinder)
            }

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
}