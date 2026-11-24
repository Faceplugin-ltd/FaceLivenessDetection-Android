package com.faceplugin.faceliveness.active

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat

/**
 * Camera scrim with an oval cut-out for the face, plus the progress ring the active-liveness
 * sequence fills in. The cut-out is also the target the user is asked to fill, so
 * [faceFrame] is exposed for laying text out underneath it.
 */
class FaceFrameOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.fp_al_scrim)
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = ContextCompat.getColor(context, R.color.fp_al_glass)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(5f)
        strokeCap = Paint.Cap.ROUND
        color = 0x33FFFFFF
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(5f)
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.fp_al_ring)
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(14f)
        color = Color.TRANSPARENT
    }

    private val oval = RectF()
    private val ring = RectF()
    private val cutout = Path()

    private var progress = 0f
    private var progressAnimator: ValueAnimator? = null
    private var haloAlpha = 0f
    private var haloAnimator: ValueAnimator? = null

    /** Oval the face should fill, in this view's coordinates. */
    val faceFrame: RectF get() = RectF(oval)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutFrame(w.toFloat(), h.toFloat())
    }

    private fun layoutFrame(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val width = (w * 0.74f).coerceAtMost(h * 0.52f)
        val height = width * 1.30f
        val cx = w / 2f
        val cy = h * 0.42f
        oval.set(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
        val inset = dp(9f)
        ring.set(
            oval.left - inset,
            oval.top - inset,
            oval.right + inset,
            oval.bottom + inset,
        )
    }

    /** Ring fill, 0f–1f. */
    fun setProgress(value: Float, animate: Boolean = true) {
        val target = value.coerceIn(0f, 1f)
        progressAnimator?.cancel()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        progressAnimator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 260L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setAccentColor(color: Int) {
        if (progressPaint.color == color) return
        progressPaint.color = color
        invalidate()
    }

    /** One soft pulse around the frame — used when a challenge is completed. */
    fun pulse(color: Int) {
        haloAnimator?.cancel()
        haloPaint.color = color
        haloAnimator = ValueAnimator.ofFloat(0.55f, 0f).apply {
            duration = 620L
            addUpdateListener {
                haloAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        haloAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (oval.isEmpty) layoutFrame(width.toFloat(), height.toFloat())
        if (oval.isEmpty) return

        cutout.reset()
        cutout.addOval(oval, Path.Direction.CW)
        cutout.fillType = Path.FillType.INVERSE_EVEN_ODD
        canvas.drawPath(cutout, scrimPaint)
        canvas.drawOval(oval, edgePaint)

        if (haloAlpha > 0f) {
            haloPaint.alpha = (haloAlpha * 255f).toInt().coerceIn(0, 255)
            canvas.drawOval(ring, haloPaint)
        }

        canvas.drawArc(ring, -90f, 360f, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(ring, -90f, 360f * progress, false, progressPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
