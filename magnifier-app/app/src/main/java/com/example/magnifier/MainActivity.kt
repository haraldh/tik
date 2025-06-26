package com.example.magnifier

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.magnifier.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera
    private var isInverted = false
    private var currentZoomRatio = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            isInverted = isChecked
            applyInvertFilter()
        }
    }

    private fun ActivityMainBinding.updateZoomText(zoom: Float) {
        zoomText.text = String.format("%.1fx", zoom)
    }

    private fun applyInvertFilter() {
        if (isInverted) {
            val invertMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            binding.viewFinder.post {
                binding.viewFinder.overlay.clear()
                (binding.viewFinder.getChildAt(0) as? PreviewView)?.let { preview ->
                    preview.foreground?.colorFilter = ColorMatrixColorFilter(invertMatrix)
                    if (preview.foreground == null) {
                        preview.foreground = android.graphics.drawable.ColorDrawable(0).apply {
                            colorFilter = ColorMatrixColorFilter(invertMatrix)
                        }
                    }
                }
            }
        } else {
            binding.viewFinder.post {
                (binding.viewFinder.getChildAt(0) as? PreviewView)?.foreground = null
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

                // Set initial zoom
                camera.cameraControl.setZoomRatio(currentZoomRatio)

            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
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