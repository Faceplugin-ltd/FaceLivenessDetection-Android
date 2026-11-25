package com.faceplugin.faceliveness.active

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.faceplugin.faceliveness.kit.CameraFrameUtils
import com.faceplugin.faceliveness.kit.CameraPreview
import com.faceplugin.faceliveness.kit.DetectOptions
import com.faceplugin.faceliveness.kit.DetectedFace
import com.faceplugin.faceliveness.kit.FaceEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Guided active-liveness screen.
 *
 * One attempt runs in three stages, in this order: the face is found and aligned, the **passive
 * anti-spoofing check** scores it, and only then are the challenges asked. A printed photo or a
 * replayed video is turned away at the gate, before it is ever invited to blink.
 *
 * Launch it for a result:
 *
 * ```kotlin
 * val launcher = registerForActivityResult(StartActivityForResult()) { r ->
 *     val result = ActiveLivenessActivity.result(r.data)
 *     if (result?.passed == true) { /* live person */ }
 * }
 * launcher.launch(ActiveLivenessActivity.intent(this, options))
 * ```
 *
 * [FaceEngine.start] has to have succeeded before this screen opens.
 */
class ActiveLivenessActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlay: FaceFrameOverlayView
    private lateinit var hudInstruction: View
    private lateinit var stepDots: LinearLayout
    private lateinit var imgChallenge: ImageView
    private lateinit var txtChallengeTitle: TextView
    private lateinit var txtChallengeHint: TextView
    private lateinit var txtStepCounter: TextView
    private lateinit var panelPrepare: View
    private lateinit var panelResult: View
    private lateinit var resultSteps: LinearLayout

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var options: ActiveLivenessOptions
    private val main = Handler(Looper.getMainLooper())
    private val busy = AtomicBoolean(false)

    private var cameraProvider: ProcessCameraProvider? = null
    private var session: ActiveLivenessSession? = null
    private var plan: List<ActiveCheck> = emptyList()
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    @Volatile
    private var attemptRunning = false

    private var renderedInstruction: String? = null
    private var renderedSteps: List<ActiveLivenessSession.StepState> = emptyList()
    private var lastPassedCount = 0

    private val timeoutRunnable = Runnable {
        session?.takeIf { !it.isFinished }?.let {
            it.timeOut()
            renderState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fp_al_activity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        options = ActiveLivenessOptions.fromBundle(intent.getBundleExtra(EXTRA_OPTIONS))
        lensFacing = intent.getIntExtra(EXTRA_LENS, CameraSelector.LENS_FACING_FRONT)

        viewFinder = findViewById(R.id.fp_al_preview)
        viewFinder.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        overlay = findViewById(R.id.fp_al_overlay)
        hudInstruction = findViewById(R.id.fp_al_hudInstruction)
        stepDots = findViewById(R.id.fp_al_stepDots)
        imgChallenge = findViewById(R.id.fp_al_imgChallenge)
        txtChallengeTitle = findViewById(R.id.fp_al_txtChallengeTitle)
        txtChallengeHint = findViewById(R.id.fp_al_txtChallengeHint)
        txtStepCounter = findViewById(R.id.fp_al_txtStepCounter)
        panelPrepare = findViewById(R.id.fp_al_panelPrepare)
        panelResult = findViewById(R.id.fp_al_panelResult)
        resultSteps = findViewById(R.id.fp_al_resultSteps)

        cameraExecutor = Executors.newFixedThreadPool(1)

        findViewById<ImageButton>(R.id.fp_al_btnClose).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.fp_al_btnSwitchCamera).setOnClickListener { toggleCamera() }
        findViewById<View>(R.id.fp_al_btnStart).setOnClickListener { startAttempt() }
        findViewById<View>(R.id.fp_al_btnRetry).setOnClickListener { showPrepare() }
        findViewById<View>(R.id.fp_al_btnDone).setOnClickListener { finish() }

        showPrepare()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        } else {
            viewFinder.post { setUpCamera() }
        }
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

    override fun onPause() {
        super.onPause()
        if (attemptRunning) {
            stopAttempt()
            session?.cancel()
            renderState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        main.removeCallbacksAndMessages(null)
        attemptRunning = false
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    // region attempt lifecycle

    private fun showPrepare() {
        stopAttempt()
        plan = options.buildPlan()
        session = ActiveLivenessSession(plan, options)
        lastPassedCount = 0

        panelResult.visibility = View.GONE
        panelPrepare.visibility = View.VISIBLE
        hudInstruction.visibility = View.INVISIBLE
        txtStepCounter.visibility = View.INVISIBLE
        overlay.setProgress(0f, animate = false)
        overlay.setAccentColor(color(R.color.fp_al_ring))

        // How many actions, not which ones: naming the sequence in advance is exactly what
        // options.randomize exists to prevent. The result card lists every step afterwards.
        findViewById<TextView>(R.id.fp_al_txtPrepareBody).text =
            getString(R.string.fp_al_prepare_body, plan.size)
    }

    private fun startAttempt() {
        panelPrepare.visibility = View.GONE
        panelResult.visibility = View.GONE
        hudInstruction.visibility = View.VISIBLE
        txtStepCounter.visibility = View.VISIBLE
        buildStepDots()
        renderedInstruction = null
        renderedSteps = emptyList()
        session?.start()
        attemptRunning = true
        renderState()
        main.removeCallbacks(timeoutRunnable)
        main.postDelayed(timeoutRunnable, watchdogMs())
    }

    private fun stopAttempt() {
        main.removeCallbacks(timeoutRunnable)
        attemptRunning = false
    }

    /**
     * Last-resort guard for frames that stop arriving. The session owns the real budgets: a
     * wall-clock one for aligning, and [ActiveLivenessOptions.framesWait] analyzed frames for the
     * gate and for each challenge. How many frames a second the engine manages depends on the
     * phone, so this allows a slow 5 frames a second before giving up on the attempt.
     */
    private fun watchdogMs(): Long {
        val perStage = options.framesWait * SLOW_FRAME_MS
        return (ALIGN_GRACE_MS + (plan.size + 1) * perStage).coerceAtMost(WATCHDOG_CAP_MS)
    }

    // endregion

    // region camera

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
            CameraPreview.bind(this, provider, viewFinder, lensFacing, cameraExecutor, ::analyzeImage)
        } catch (_: Exception) {
        }
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraProvider?.unbindAll()
        showPrepare()
        viewFinder.post { setUpCamera() }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(imageProxy: ImageProxy) {
        var frame: Bitmap? = null
        try {
            if (attemptRunning && FaceEngine.isReady) {
                val back = lensFacing == CameraSelector.LENS_FACING_BACK
                frame = CameraFrameUtils.fromImageProxy(imageProxy, back)
            }
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
        val bitmap = frame ?: return
        val active = session
        if (!attemptRunning || active == null || active.isFinished ||
            !busy.compareAndSet(false, true)
        ) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        // Scoring liveness costs more than pose and expression, so only the gate asks for it.
        val detectOptions = if (active.needsLiveness) {
            DetectOptions.ACTIVE
        } else {
            DetectOptions.ACTIVE_FAST
        }

        val frameWidth = bitmap.width
        val frameHeight = bitmap.height

        FaceEngine.async {
            val faces: List<DetectedFace> = try {
                FaceEngine.detect(bitmap, detectOptions)
            } catch (_: Exception) {
                emptyList()
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            if (!active.isFinished && attemptRunning) {
                active.onFrame(faces, frameWidth, frameHeight)
                main.post { renderState() }
            }
            busy.set(false)
        }
    }

    // endregion

    // region rendering

    private fun renderState() {
        val active = session ?: return
        val state = active.snapshot()

        overlay.setProgress(state.progress)
        renderStepDots(state)

        when (state.phase) {
            ActiveLivenessSession.Phase.IDLE -> Unit
            ActiveLivenessSession.Phase.ALIGN -> {
                overlay.setAccentColor(color(R.color.fp_al_ring))
                txtStepCounter.text = getString(R.string.fp_al_step_of, 1, state.total)
                setInstruction(
                    noticeIcon(state.notice),
                    noticeText(state.notice, R.string.fp_al_state_align),
                    restartHint(state),
                )
            }
            ActiveLivenessSession.Phase.PASSIVE -> {
                overlay.setAccentColor(color(R.color.fp_al_ring))
                txtStepCounter.text = getString(R.string.fp_al_prepare_passive)
                setInstruction(
                    if (state.notice == ActiveLivenessSession.Notice.NONE) {
                        R.drawable.fp_al_ic_shield
                    } else {
                        noticeIcon(state.notice)
                    },
                    noticeText(state.notice, R.string.fp_al_state_passive),
                    restartHint(state),
                )
            }
            ActiveLivenessSession.Phase.RUNNING -> {
                overlay.setAccentColor(color(R.color.fp_al_ring))
                txtStepCounter.text = getString(
                    R.string.fp_al_step_of,
                    (state.activeIndex + 1).coerceAtLeast(1),
                    state.total,
                )
                renderChallenge(state)
                // A restart takes the count back down; follow it so the next pass still pulses.
                if (state.passedCount < lastPassedCount) lastPassedCount = state.passedCount
                if (state.passedCount > lastPassedCount) {
                    overlay.pulse(color(R.color.fp_al_pass))
                    haptic()
                }
                lastPassedCount = state.passedCount
            }
            ActiveLivenessSession.Phase.PASSED -> {
                overlay.setAccentColor(color(R.color.fp_al_pass))
                overlay.pulse(color(R.color.fp_al_pass))
                showResult(state)
            }
            ActiveLivenessSession.Phase.FAILED -> {
                overlay.setAccentColor(color(R.color.fp_al_fail))
                showResult(state)
            }
        }
    }

    private fun renderChallenge(state: ActiveLivenessSession.State) {
        val check = state.current
        val blocked = state.notice != ActiveLivenessSession.Notice.NONE &&
            state.notice != ActiveLivenessSession.Notice.WRONG_WAY
        when {
            blocked -> setInstruction(
                noticeIcon(state.notice),
                noticeText(
                    state.notice,
                    R.string.fp_al_state_starting,
                    noFace = R.string.fp_al_state_lost,
                ),
                "",
            )
            check != null -> setInstruction(
                check.iconRes,
                getString(check.titleRes),
                if (state.notice == ActiveLivenessSession.Notice.WRONG_WAY) {
                    getString(R.string.fp_al_state_wrong_way)
                } else {
                    getString(check.hintRes)
                },
            )
            else -> setInstruction(
                R.drawable.fp_al_ic_face,
                getString(R.string.fp_al_state_starting),
                "",
            )
        }
    }

    /** Says the attempt went back to the start, so a reset ring does not look like a glitch. */
    private fun restartHint(state: ActiveLivenessSession.State): String =
        if (state.restarts > 0) getString(R.string.fp_al_restarted) else ""


    /**
     * What to say about the frame. [fallback] is used when there is nothing wrong with it, and
     * [noFace] lets the align stage say "looking for your face" where a running attempt says the
     * face was lost.
     */
    private fun noticeText(
        notice: ActiveLivenessSession.Notice,
        fallback: Int,
        noFace: Int = R.string.fp_al_state_searching,
    ): String = when (notice) {
        ActiveLivenessSession.Notice.NO_FACE -> getString(noFace)
        ActiveLivenessSession.Notice.MANY_FACES -> getString(R.string.fp_al_state_multiple)
        ActiveLivenessSession.Notice.TOO_FAR -> getString(R.string.fp_al_state_too_far)
        ActiveLivenessSession.Notice.TOO_CLOSE -> getString(R.string.fp_al_state_too_close)
        ActiveLivenessSession.Notice.OFF_CENTER -> getString(R.string.fp_al_state_off_center)
        ActiveLivenessSession.Notice.LOOK_STRAIGHT -> getString(R.string.fp_al_state_look_straight)
        ActiveLivenessSession.Notice.LOW_QUALITY -> getString(R.string.fp_al_state_low_quality)
        ActiveLivenessSession.Notice.NOT_SCORABLE -> getString(R.string.fp_al_state_not_scorable)
        else -> getString(fallback)
    }

    /** The icon that goes with a notice: the face outline unless the challenge itself is showing. */
    private fun noticeIcon(notice: ActiveLivenessSession.Notice): Int = when (notice) {
        ActiveLivenessSession.Notice.TOO_FAR -> R.drawable.fp_al_ic_closer
        ActiveLivenessSession.Notice.LOW_QUALITY,
        ActiveLivenessSession.Notice.NOT_SCORABLE,
        -> R.drawable.fp_al_ic_shield
        else -> R.drawable.fp_al_ic_face
    }

    /** Frames arrive continuously, so only touch the views when the text changes. */
    private fun setInstruction(iconRes: Int, title: String, hint: String) {
        val key = "$iconRes|$title|$hint"
        if (key == renderedInstruction) return
        renderedInstruction = key
        imgChallenge.setImageResource(iconRes)
        txtChallengeTitle.text = title
        txtChallengeHint.text = hint
        txtChallengeHint.visibility = if (hint.isEmpty()) View.GONE else View.VISIBLE
    }

    /** One dot for the anti-spoofing gate, then one per challenge. */
    private fun buildStepDots() {
        stepDots.removeAllViews()
        repeat(plan.size + 1) { index ->
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dp(26), dp(5))
            if (index > 0) params.marginStart = dp(6)
            dot.layoutParams = params
            dot.background = pillDrawable(PENDING_DOT)
            stepDots.addView(dot)
        }
    }

    private fun renderStepDots(state: ActiveLivenessSession.State) {
        val all = listOf(state.passive) + state.steps
        if (stepDots.childCount != all.size) return
        if (all == renderedSteps) return
        renderedSteps = all
        all.forEachIndexed { index, step ->
            stepDots.getChildAt(index).background = pillDrawable(dotColor(step))
        }
    }

    private fun dotColor(step: ActiveLivenessSession.StepState): Int = when (step) {
        ActiveLivenessSession.StepState.PASSED -> color(R.color.fp_al_pass)
        ActiveLivenessSession.StepState.FAILED -> color(R.color.fp_al_fail)
        ActiveLivenessSession.StepState.ACTIVE -> color(R.color.fp_al_ring)
        ActiveLivenessSession.StepState.PENDING -> PENDING_DOT
    }

    private fun showResult(state: ActiveLivenessSession.State) {
        if (panelResult.visibility == View.VISIBLE) return
        stopAttempt()
        hudInstruction.visibility = View.INVISIBLE
        txtStepCounter.visibility = View.INVISIBLE
        panelResult.visibility = View.VISIBLE
        haptic()
        publishResult(state)

        val passed = state.phase == ActiveLivenessSession.Phase.PASSED
        val img = findViewById<ImageView>(R.id.fp_al_imgResult)
        img.setImageResource(if (passed) R.drawable.fp_al_ic_pass else R.drawable.fp_al_ic_fail)
        img.imageTintList = ColorStateList.valueOf(
            color(if (passed) R.color.fp_al_pass else R.color.fp_al_fail),
        )
        findViewById<TextView>(R.id.fp_al_txtResultTitle).setText(
            if (passed) R.string.fp_al_result_pass_title else R.string.fp_al_result_fail_title,
        )
        findViewById<TextView>(R.id.fp_al_txtResultBody).text = when {
            passed -> getString(R.string.fp_al_result_pass_body, state.total)
            state.failure == ActiveLivenessSession.Failure.SPOOF ->
                getString(R.string.fp_al_result_fail_spoof)
            state.failure == ActiveLivenessSession.Failure.CANCELLED ->
                getString(R.string.fp_al_result_fail_cancelled)
            state.failedCheck != null ->
                getString(R.string.fp_al_result_fail_check, getString(state.failedCheck.titleRes))
            else -> getString(R.string.fp_al_result_fail_timeout)
        }
        findViewById<TextView>(R.id.fp_al_chipDuration).text =
            getString(R.string.fp_al_result_duration, state.elapsedMs / 1000f)
        val score = state.passiveScore
        findViewById<TextView>(R.id.fp_al_chipPassive).text = if (score >= 0f) {
            getString(
                R.string.fp_al_result_passive_score,
                (score.coerceIn(0f, 1f) * 100f).roundToInt(),
            )
        } else {
            getString(R.string.fp_al_result_passive_pending)
        }

        resultSteps.removeAllViews()
        resultSteps.addView(
            row(
                resultSteps,
                R.drawable.fp_al_ic_shield,
                getString(R.string.fp_al_prepare_passive),
                stateLabel(state.passive),
                dotColor(state.passive),
            ),
        )
        plan.forEachIndexed { index, check ->
            val step = state.steps.getOrNull(index) ?: ActiveLivenessSession.StepState.PENDING
            resultSteps.addView(
                row(
                    resultSteps,
                    check.iconRes,
                    getString(check.titleRes),
                    stateLabel(step),
                    dotColor(step),
                ),
            )
        }
    }

    private fun stateLabel(step: ActiveLivenessSession.StepState): String = when (step) {
        ActiveLivenessSession.StepState.PASSED -> getString(R.string.fp_al_result_step_passed)
        ActiveLivenessSession.StepState.FAILED -> getString(R.string.fp_al_result_step_failed)
        else -> getString(R.string.fp_al_result_step_skipped)
    }

    private fun publishResult(state: ActiveLivenessSession.State) {
        val result = ActiveLivenessResult.from(state, plan)
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_RESULT, result.toBundle()),
        )
    }

    private fun row(
        parent: LinearLayout,
        iconRes: Int,
        title: String,
        stateText: String,
        stateColor: Int,
    ): View {
        val view = LayoutInflater.from(this).inflate(R.layout.fp_al_item_challenge, parent, false)
        view.findViewById<ImageView>(R.id.fp_al_imgStep).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.fp_al_txtStep).text = title
        view.findViewById<TextView>(R.id.fp_al_txtStepState).apply {
            text = stateText
            setTextColor(stateColor)
        }
        return view
    }

    // endregion

    private fun pillDrawable(fill: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(3).toFloat()
        setColor(fill)
    }

    private fun haptic() {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun color(resId: Int): Int = ContextCompat.getColor(this, resId)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val REQ_CAMERA = 1
        private const val ALIGN_GRACE_MS = 25_000L
        private const val SLOW_FRAME_MS = 200L
        private const val WATCHDOG_CAP_MS = 300_000L
        private const val PENDING_DOT = 0x40FFFFFF

        private const val EXTRA_OPTIONS = "com.faceplugin.faceliveness.active.OPTIONS"
        private const val EXTRA_LENS = "com.faceplugin.faceliveness.active.LENS"
        private const val EXTRA_RESULT = "com.faceplugin.faceliveness.active.RESULT"

        /** Intent that opens the screen with [options], to be launched for a result. */
        @JvmStatic
        @JvmOverloads
        fun intent(
            context: Context,
            options: ActiveLivenessOptions = ActiveLivenessOptions(),
            lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
        ): Intent = Intent(context, ActiveLivenessActivity::class.java)
            .putExtra(EXTRA_OPTIONS, options.toBundle())
            .putExtra(EXTRA_LENS, lensFacing)

        /** The attempt's outcome from the activity result, or `null` if it never got that far. */
        @JvmStatic
        fun result(data: Intent?): ActiveLivenessResult? =
            ActiveLivenessResult.fromBundle(data?.getBundleExtra(EXTRA_RESULT))
    }
}
