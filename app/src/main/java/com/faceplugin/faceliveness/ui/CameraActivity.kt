package com.faceplugin.faceliveness.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.faceplugin.faceliveness.R
import com.faceplugin.faceliveness.data.AppSettings
import com.faceplugin.faceliveness.kit.CameraFrameUtils
import com.faceplugin.faceliveness.kit.CameraPreview
import com.faceplugin.faceliveness.kit.DetectOptions
import com.faceplugin.faceliveness.kit.DetectedFace
import com.faceplugin.faceliveness.kit.FaceEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Passive liveness: every camera frame is scored on its own, with no action asked of the user.
 * The engine reads the anti-spoofing score, quality and head pose from the single frame.
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var faceView: FaceView
    private lateinit var txtHint: TextView
    private lateinit var cameraExecutor: ExecutorService

    private val busy = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile
    private var hasFaces = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewFinder = findViewById(R.id.preview)
        viewFinder.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        faceView = findViewById(R.id.faceView)
        txtHint = findViewById(R.id.txtCameraHint)
        cameraExecutor = Executors.newFixedThreadPool(1)
        faceView.setMirrorX(AppSettings.cameraLens(this) == CameraSelector.LENS_FACING_FRONT)

        findViewById<ImageButton>(R.id.btnCloseCamera).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSwitchCamera).setOnClickListener { toggleCamera() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        } else {
            viewFinder.post { setUpCamera() }
        }
        updateHint()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewFinder.post { setUpCamera() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    private fun toggleCamera() {
        val next = if (AppSettings.cameraLens(this) == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        AppSettings.setCameraLens(this, next)
        faceView.setMirrorX(next == CameraSelector.LENS_FACING_FRONT)
        faceView.setFaces(null)
        hasFaces = false
        updateHint()
        cameraProvider?.unbindAll()
        viewFinder.post { setUpCamera() }
    }

    private fun setUpCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindCameraUseCases()
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        try {
            CameraPreview.bind(
                this,
                provider,
                viewFinder,
                AppSettings.cameraLens(this),
                cameraExecutor,
                ::analyzeImage,
            )
        } catch (_: Exception) {
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(imageProxy: ImageProxy) {
        var frame: Bitmap? = null
        try {
            if (FaceEngine.isReady) {
                val back = AppSettings.cameraLens(this) == CameraSelector.LENS_FACING_BACK
                frame = CameraFrameUtils.fromImageProxy(imageProxy, back)
            }
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
        val bitmap = frame ?: return
        if (!busy.compareAndSet(false, true)) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        val size = Size(bitmap.width, bitmap.height)
        FaceEngine.async {
            val faces: List<DetectedFace> = try {
                FaceEngine.detect(bitmap, DetectOptions.PASSIVE)
            } catch (_: Exception) {
                emptyList()
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            hasFaces = faces.isNotEmpty()
            runOnUiThread {
                faceView.setFrameSize(size)
                faceView.setFaces(faces)
                updateHint()
            }
            busy.set(false)
        }
    }

    private fun updateHint() {
        txtHint.setText(
            if (hasFaces) R.string.liveness_status_analyzing else R.string.camera_hint,
        )
    }

    private companion object {
        const val REQ_CAMERA = 1
    }
}
