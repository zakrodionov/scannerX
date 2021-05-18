package maulik.barcodescanner.analyzer

import android.graphics.Rect
import android.media.Image
import android.util.Log
import android.util.Size
import android.widget.ImageView
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import maulik.barcodescanner.ui.custom.ViewFinderOverlay
import maulik.barcodescanner.util.*

class MLKitBarcodeAnalyzer(
    private val listener: ScanningResultListener,
    private val overlay: ViewFinderOverlay,
    private val cropPreview: ImageView,
) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false


    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        try {
            val imageProxyReadyEpoch = System.currentTimeMillis()
            val rotation = imageProxy.imageInfo.rotationDegrees
            debug("New image from proxy width : ${imageProxy.width} height : ${imageProxy.height} format : ${imageProxy.format} rotation: $rotation")
            val scannerRect = getScannerRectToPreviewViewRelation(
                Size(imageProxy.width, imageProxy.height),
                rotation
            )

            val image = imageProxy.image!!
            val cropRect = image.getCropRectAccordingToRotation(scannerRect, rotation)
            image.cropRect = cropRect

            val byteArray = YuvNV21Util.yuv420toNV21(image)
            val bitmap = BitmapUtil.getBitmap(
                byteArray,
                FrameMetadata(cropRect.width(), cropRect.height(), rotation)
            )
            debug("Bitmap prepared width: ${cropRect.width()} height: ${cropRect.height()}")
            val imagePreparedReadyEpoch = System.currentTimeMillis()


            val imageProcessedEpoch = System.currentTimeMillis()

            debug(
                """
                   Image proxy (${imageProxy.width},${imageProxy.height}) format : ${imageProxy.format} rotation: $rotation 
                   Cropped Image (${bitmap.width},${bitmap.height}) Preparing took: ${imagePreparedReadyEpoch - imageProxyReadyEpoch}ms
                   OCR Processing took : ${imageProcessedEpoch - imagePreparedReadyEpoch}
                """.trimIndent()
            )


            cropPreview.post {
                cropPreview.setImageBitmap(bitmap)
            }

            isScanning = true
            val scanner = BarcodeScanning.getClient()
            scanner.process(InputImage.fromBitmap(bitmap, 0))
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
        } catch (e: Exception) {
            imageProxy.close()
            debug("on error")
        }
    }


    private fun getScannerRectToPreviewViewRelation(
        proxySize: Size,
        rotation: Int
    ): ScannerRectToPreviewViewRelation {
        return when (rotation) {
            0, 180 -> {
                val size = overlay.size
                val width = size.width
                val height = size.height
                val previewHeight = width / (proxySize.width.toFloat() / proxySize.height)
                val heightDeltaTop = (previewHeight - height) / 2

                val scannerRect = overlay.boxRectF!!
                val rectStartX = scannerRect.left
                val rectStartY = heightDeltaTop + scannerRect.top

                ScannerRectToPreviewViewRelation(
                    rectStartX / width,
                    rectStartY / previewHeight,
                    scannerRect.width() / width,
                    scannerRect.height() / previewHeight
                )
            }
            90, 270 -> {
                val size = overlay.size
                val width = size.width
                val height = size.height
                val previewWidth = height / (proxySize.width.toFloat() / proxySize.height)
                val widthDeltaLeft = (previewWidth - width) / 2

                val scannerRect = overlay.boxRectF!!
                val rectStartX = widthDeltaLeft + scannerRect.left
                val rectStartY = scannerRect.top

                ScannerRectToPreviewViewRelation(
                    rectStartX / previewWidth,
                    rectStartY / height,
                    scannerRect.width() / previewWidth,
                    scannerRect.height() / height
                )
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
        }
    }

    data class ScannerRectToPreviewViewRelation(
        val relativePosX: Float,
        val relativePosY: Float,
        val relativeWidth: Float,
        val relativeHeight: Float
    )

    private fun Image.getCropRectAccordingToRotation(
        scannerRect: ScannerRectToPreviewViewRelation,
        rotation: Int
    ): Rect {
        return when (rotation) {
            0 -> {
                val startX = (scannerRect.relativePosX * this.width).toInt()
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startY = (scannerRect.relativePosY * this.height).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            90 -> {
                val startX = (scannerRect.relativePosY * this.width).toInt()
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startY =
                    height - (scannerRect.relativePosX * this.height).toInt() - numberPixelH
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            180 -> {
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startX =
                    (this.width - scannerRect.relativePosX * this.width - numberPixelW).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                val startY =
                    (height - scannerRect.relativePosY * this.height - numberPixelH).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            270 -> {
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startX =
                    (this.width - scannerRect.relativePosY * this.width - numberPixelW).toInt()
                val startY = (scannerRect.relativePosX * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
        }
    }

}