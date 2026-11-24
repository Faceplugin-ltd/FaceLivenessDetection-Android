package com.faceplugin.faceliveness.kit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.core.ImageProxy
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import kotlin.math.max
import kotlin.math.roundToInt

object CameraFrameUtils {
    /** Live frames: long side <= 640. */
    const val PREVIEW_MAX_SIDE = 640

    /**
     * One camera frame as an upright bitmap: NV21 → [FaceRecognitionSDK.bitmapFromNv21] (90° for
     * the back lens, 270° for the front) → long side <= [PREVIEW_MAX_SIDE].
     *
     * The rotation is not mirrored, so a face's yaw is its real head rotation; the preview mirrors
     * the front lens for display only.
     */
    @JvmStatic
    @SuppressLint("UnsafeOptInUsageError")
    fun fromImageProxy(imageProxy: ImageProxy, backCamera: Boolean): Bitmap? {
        val image = imageProxy.image ?: return null
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val degrees = if (backCamera) 90 else 270
        val bitmap = FaceRecognitionSDK.bitmapFromNv21(nv21, image.width, image.height, degrees)
            ?: return null
        val frame = prepareForEngine(bitmap, PREVIEW_MAX_SIDE)
        if (frame !== bitmap && !bitmap.isRecycled) bitmap.recycle()
        return frame
    }

    fun prepareForEngine(src: Bitmap, maxSide: Int = PREVIEW_MAX_SIDE): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src
        val longSide = max(w, h)
        if (longSide <= maxSide) return src
        val scale = maxSide.toFloat() / longSide
        val tw = max(1, (w * scale).roundToInt())
        val th = max(1, (h * scale).roundToInt())
        return Bitmap.createScaledBitmap(src, tw, th, true)
    }

    @JvmStatic
    fun copyArgb(src: Bitmap): Bitmap {
        if (src.config == Bitmap.Config.ARGB_8888 && !src.isRecycled) {
            return src.copy(Bitmap.Config.ARGB_8888, false)
        }
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, 0f, 0f, null)
        return out
    }
}
