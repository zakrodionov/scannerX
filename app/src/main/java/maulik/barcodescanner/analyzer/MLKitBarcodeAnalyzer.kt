package maulik.barcodescanner.analyzer

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import maulik.barcodescanner.ui.custom.ViewFinderOverlay
import maulik.barcodescanner.util.crop
import maulik.barcodescanner.util.debug
import maulik.barcodescanner.util.rotate
import maulik.barcodescanner.util.toBitmap

class MLKitBarcodeAnalyzer(private val listener: ScanningResultListener, private val  overlay: ViewFinderOverlay) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanning) {

            val bitmap = mediaImage.toBitmap(overlay.context)
            val cropped = bitmap.crop(imageProxy.cropRect)
            val rotated = cropped.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            val image = InputImage.fromBitmap(rotated, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

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
                    // Task failed with an exception
                    // ...
                    isScanning = false
                    imageProxy.close()
                }
        }
    }
}