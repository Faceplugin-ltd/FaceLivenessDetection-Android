package com.faceplugin.faceliveness.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Size
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.faceplugin.faceliveness.R
import com.faceplugin.faceliveness.data.AppSettings
import com.faceplugin.faceliveness.kit.DetectedFace
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/** Face boxes and the passive-liveness read-out, drawn over the camera preview. */
class FaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val realPaint = strokePaint(color(R.color.liveness_real))
    private val spoofPaint = strokePaint(color(R.color.liveness_spoof))
    private val neutralPaint = strokePaint(color(R.color.liveness_neutral))

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = color(R.color.fp_text)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            13f,
            resources.displayMetrics,
        )
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xB3000000.toInt()
    }

    private val lineHeight = textPaint.textSize * 1.25f
    private val drawRect = RectF()

    private var frameSize: Size? = null
    private var mirrorX = false
    private var faces: List<DetectedFace> = emptyList()

    fun setFrameSize(size: Size) {
        frameSize = size
    }

    fun setMirrorX(mirror: Boolean) {
        mirrorX = mirror
    }

    fun setFaces(faces: List<DetectedFace>?) {
        this.faces = faces.orEmpty()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frame = frameSize ?: return
        if (faces.isEmpty()) return

        val frameW = frame.width.toFloat()
        val frameH = frame.height.toFloat()
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (frameW <= 0f || frameH <= 0f || viewW <= 0f || viewH <= 0f) return

        // PreviewView is FILL_CENTER, so the frame is scaled up and cropped the same way.
        val scale = maxOf(viewW / frameW, viewH / frameH)
        val dx = (viewW - frameW * scale) / 2f
        val dy = (viewH - frameH * scale) / 2f

        for (face in faces) {
            var left = mapX(face.bounds.left.toFloat(), scale, dx, viewW)
            var right = mapX(face.bounds.right.toFloat(), scale, dx, viewW)
            if (left > right) {
                val tmp = left
                left = right
                right = tmp
            }
            val top = face.bounds.top * scale + dy
            val bottom = face.bounds.bottom * scale + dy

            val known = face.hasLiveness
            val live = known && AppSettings.livenessPassed(
                context,
                face.liveness,
                face.livenessLabel,
            )
            val paint = when {
                !known -> neutralPaint
                live -> realPaint
                else -> spoofPaint
            }

            val corner = min(right - left, bottom - top) * 0.22f
            drawRect.set(left, top, right, bottom)
            canvas.drawRoundRect(drawRect, corner, corner, paint)
            drawInfoBlock(canvas, face, left, top, live, known)
        }
    }

    private fun drawInfoBlock(
        canvas: Canvas,
        face: DetectedFace,
        boxLeft: Float,
        boxTop: Float,
        live: Boolean,
        livenessKnown: Boolean,
    ) {
        val lines = buildLines(face, live, livenessKnown)
        if (lines.isEmpty()) return

        val padX = 10f
        val padY = 7f
        var maxWidth = 0f
        for (line in lines) {
            maxWidth = maxOf(maxWidth, textPaint.measureText(line))
        }
        val blockW = maxWidth + padX * 2f
        val blockH = lines.size * lineHeight + padY * 2f
        var left = boxLeft
        var top = boxTop - blockH - 8f
        if (top < 4f) {
            top = min(boxTop + 8f, height - blockH - 4f)
        }
        if (left + blockW > width - 4f) {
            left = maxOf(4f, width - blockW - 4f)
        }

        drawRect.set(left, top, left + blockW, top + blockH)
        canvas.drawRoundRect(drawRect, 14f, 14f, textBgPaint)
        var textY = top + padY + textPaint.textSize
        for (line in lines) {
            canvas.drawText(line, left + padX, textY, textPaint)
            textY += lineHeight
        }
    }

    private fun buildLines(
        face: DetectedFace,
        live: Boolean,
        livenessKnown: Boolean,
    ): List<String> {
        val lines = ArrayList<String>(3)
        lines += if (livenessKnown) {
            val verdict = if (live) "Real" else "Spoof"
            String.format(Locale.US, "Liveness : %s %d%%", verdict, percent(face.liveness))
        } else {
            "Liveness : —"
        }
        lines += if (!face.quality.isNaN()) {
            String.format(Locale.US, "Quality : %d%%", percent(face.quality))
        } else {
            String.format(Locale.US, "Quality : %d%%", percent(face.score))
        }
        lines += if (face.hasPose) {
            String.format(Locale.US, "Pose : %.0f° / %.0f°", face.yaw, face.pitch)
        } else {
            "Pose : —"
        }
        return lines
    }

    private fun percent(value: Float): Int {
        if (value.isNaN()) return 0
        return (value.coerceIn(0f, 1f) * 100f).roundToInt()
    }

    private fun mapX(frameX: Float, scale: Float, dx: Float, viewW: Float): Float {
        val vx = frameX * scale + dx
        return if (mirrorX) viewW - vx else vx
    }

    private fun strokePaint(paintColor: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeJoin = Paint.Join.ROUND
        color = paintColor
    }

    private fun color(resId: Int): Int = ContextCompat.getColor(context, resId)
}
