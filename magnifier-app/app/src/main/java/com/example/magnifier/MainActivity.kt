package com.example.magnifier

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.magnifier.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: androidx.camera.core.Camera
    private var imageAnalysis: ImageAnalysis? = null
    private var isInverted = false
    private var isLight = false
    private var currentZoomRatio = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle window insets for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.controlsLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                view.paddingBottom + systemBars.bottom + 16 // Preserve existing bottom padding and add extra padding above navigation bar
            )
            insets
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up UI controls
        setupControls()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupControls() {
        // Zoom slider (1x to 10x magnification)
        binding.zoomSlider.max = 90 // 0 to 90, representing 1.0x to 10.0x
        binding.zoomSlider.progress = 0
        binding.updateZoomText(1.0f)

        binding.zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentZoomRatio = 1.0f + (progress / 10.0f)
                if (::camera.isInitialized) {
                    camera.cameraControl.setZoomRatio(currentZoomRatio)
                }
                binding.updateZoomText(currentZoomRatio)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Invert filter toggle
        binding.invertToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Magnifier", "Invert toggle changed to: $isChecked")
            isInverted = isChecked
            applyInvertFilter()
        }

        // Invert filter toggle
        binding.lightToggle.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Magnifier", "Light toggle changed to: $isChecked")
            isLight = isChecked
            applyChangeLight()
        }
    }

    private fun ActivityMainBinding.updateZoomText(zoom: Float) {
        zoomText.text = String.format("%.1fx", zoom)
    }

    private fun applyChangeLight(){
        Log.d("Magnifier", "applyChangeLight called with isLight=$isLight")
        camera.cameraControl.enableTorch(isLight)
    }


    private fun applyInvertFilter() {
        Log.d("Magnifier", "applyInvertFilter called with isInverted=$isInverted")
        // Restart camera with or without image processing
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()

            // Image analysis for color inversion
            if (isInverted) {
                binding.viewFinder.visibility = android.view.View.INVISIBLE
                binding.processedImageView.visibility = android.view.View.VISIBLE

                imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImage(imageProxy)
                        }
                    }
            } else {
                binding.viewFinder.visibility = android.view.View.VISIBLE
                binding.processedImageView.visibility = android.view.View.GONE
                preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                imageAnalysis = null
            }

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val useCases = mutableListOf(preview as UseCase)
                imageAnalysis?.let { useCases.add(it) }

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, *useCases.toTypedArray()
                )

                // Set initial zoom
                camera.cameraControl.setZoomRatio(currentZoomRatio)
                camera.cameraControl.enableTorch(isLight)

            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
                Log.e("Magnifier", "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val planes = imageProxy.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width

        val bitmap = Bitmap.createBitmap(
            imageProxy.width + rowPadding / pixelStride,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        // Rotate the bitmap to correct orientation
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)

        // Apply color inversion
        val invertedBitmap = invertColors(rotatedBitmap)

        // Update UI on main thread
        runOnUiThread {
            binding.processedImageView.setImageBitmap(invertedBitmap)
        }

        imageProxy.close()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    private fun invertColors(bitmap: Bitmap): Bitmap {
        val inverted = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(inverted)
        val paint = Paint()

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return inverted
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required for this app",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
