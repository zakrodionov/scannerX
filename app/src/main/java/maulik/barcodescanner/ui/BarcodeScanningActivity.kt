package maulik.barcodescanner.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import maulik.barcodescanner.R
import maulik.barcodescanner.analyzer.MLKitBarcodeAnalyzer
import maulik.barcodescanner.analyzer.ScanningResultListener
import maulik.barcodescanner.databinding.ActivityBarcodeScanningBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val ARG_SCANNING_SDK = "scanning_SDK"

class BarcodeScanningActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun start(context: Context, scannerSDK: ScannerSDK) {
            val starter = Intent(context, BarcodeScanningActivity::class.java).apply {
                putExtra(ARG_SCANNING_SDK, scannerSDK)
            }
            context.startActivity(starter)
        }

        private const val TARGET_PREVIEW_WIDTH = 960
        private const val TARGET_PREVIEW_HEIGHT = 1280
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityBarcodeScanningBinding
    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.ivScannerLogo.setImageResource(R.drawable.mlkit_icon)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {

        if (isDestroyed || isFinishing) {
            //This check is to avoid an exception when trying to re-bind use cases but user closes the activity.
            //java.lang.IllegalArgumentException: Trying to create use case mediator with destroyed lifecycle.
            return
        }

        cameraProvider?.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(TARGET_PREVIEW_WIDTH, TARGET_PREVIEW_HEIGHT))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        //switch the analyzers here, i.e. MLKitBarcodeAnalyzer, ZXingBarcodeAnalyzer
        class ScanningListener : ScanningResultListener {
            override fun onScanned(result: String) {
                runOnUiThread {
                    imageAnalysis.clearAnalyzer()
                    cameraProvider?.unbindAll()
                    ScannerResultDialog.newInstance(
                        result,
                        object : ScannerResultDialog.DialogDismissListener {
                            override fun onDismiss() {
                                bindPreview(cameraProvider)
                            }
                        })
                        .show(supportFragmentManager, ScannerResultDialog::class.java.simpleName)
                }
            }
        }

        val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer(ScanningListener(), binding.overlay, binding.ivCropPreview)

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        binding.cameraPreview.previewStreamState

        val viewPort = binding.cameraPreview.viewPort!!
        val useCases = UseCaseGroup.Builder()
            //.setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .build()

        val camera =
            cameraProvider?.bindToLifecycle(this, cameraSelector, useCases)

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            binding.ivFlashControl.visibility = View.VISIBLE

            binding.ivFlashControl.setOnClickListener {
                camera.cameraControl.enableTorch(!flashEnabled)
            }

            camera.cameraInfo.torchState.observe(this) {
                it?.let { torchState ->
                    if (torchState == TorchState.ON) {
                        flashEnabled = true
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_on)
                    } else {
                        flashEnabled = false
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_off)
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    enum class ScannerSDK {
        MLKIT,
        ZXING
    }
}