package com.faceplugin.faceliveness.active

import android.os.SystemClock
import com.faceplugin.faceliveness.kit.DetectedFace
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Runs one active-liveness attempt.
 *
 * The engine scores a frame; it does not sequence anything. So this class owns the order of the
 * attempt:
 *
 * 1. **Align** — wait for exactly one face, framed well enough to be scored.
 * 2. **Passive** — score that face for spoofing and average
 *    [ActiveLivenessOptions.passiveSamples] frames. Below
 *    [ActiveLivenessOptions.livenessThreshold] the attempt ends here and no challenge is asked:
 *    a printed photo or a replayed video should never get to perform actions.
 * 3. **Challenges** — ask for each challenge in [plan] in turn, deciding from the per-frame
 *    attributes ([DetectedFace]) when one is satisfied, timed out or failed.
 *
 * **Losing the face starts the attempt over.** Gone for longer than about a second and everything
 * resets to stage one — the gate included — so no attempt can be half-completed by one face and
 * finished by another. [State.restarts] counts how often that has happened.
 *
 * While a face is present but cannot be scored, [State.notice] says what is wrong with it: too far,
 * too close, off centre, turned away, or too poor an image for the engine to judge. The screen shows
 * that instead of asking the user to keep holding still.
 *
 * Feed it with [onFrame] and read [snapshot] for what the screen should show. The caller decides
 * what to ask the engine for per frame: [needsLiveness] says when the passive score is still
 * wanted, so the challenge frames can skip that cost.
 *
 * Not thread-safe — call both from the same thread.
 */
class ActiveLivenessSession(
    val plan: List<ActiveCheck>,
    private val options: ActiveLivenessOptions,
) {

    enum class Phase { IDLE, ALIGN, PASSIVE, RUNNING, PASSED, FAILED }

    enum class StepState { PENDING, ACTIVE, PASSED, FAILED }

    enum class Failure { NONE, SPOOF, CHECK, TIMEOUT, CANCELLED }

    /**
     * What is wrong with the frame, if anything. Everything but [WRONG_WAY] is about the face
     * being unusable rather than the challenge being unmet.
     */
    enum class Notice {
        NONE,

        /** No face in the frame. Held long enough, this restarts the attempt. */
        NO_FACE,

        /** More than one face: the attempt waits rather than guessing which one to follow. */
        MANY_FACES,

        /** The face is too small a part of the frame for the engine to score it. */
        TOO_FAR,

        /** The face runs past the edge of the frame. */
        TOO_CLOSE,

        /** The face is inside the frame but well off centre. */
        OFF_CENTER,

        /** The face is turned or tilted too far to start from. */
        LOOK_STRAIGHT,

        /** The image is too poor — usually light — for the engine to judge. */
        LOW_QUALITY,

        /** Well framed, but the engine returned no liveness score for it. */
        NOT_SCORABLE,

        /** A head turn or tilt went the opposite way to the one asked for. */
        WRONG_WAY,
    }

    data class State(
        val phase: Phase,
        val passive: StepState,
        val passiveScore: Float,
        val steps: List<StepState>,
        val activeIndex: Int,
        val current: ActiveCheck?,
        val progress: Float,
        val passedCount: Int,
        val total: Int,
        val failedCheck: ActiveCheck?,
        val failure: Failure,
        val notice: Notice,
        val restarts: Int,
        val elapsedMs: Long,
    )

    private val steps = MutableList(plan.size) { StepState.PENDING }
    private var phase = Phase.IDLE
    private var passive = StepState.PENDING
    private var activeIndex = -1
    private var failedCheck: ActiveCheck? = null
    private var failure = Failure.NONE
    private var notice = Notice.NONE
    private var startedAt = 0L
    private var finishedAt = 0L

    // Face loss
    private var lastFaceAt = 0L
    private var restarts = 0

    // Passive gate
    private var passiveSum = 0f
    private var passiveCount = 0
    private var passiveScore = -1f
    private var framesOnPassive = 0

    // Align stage, measured from the last (re)start rather than from the attempt
    private var alignStartedAt = 0L

    // Per-challenge progress
    private var checkProgress = 0f
    private var framesOnCheck = 0
    private var holdFrames = 0

    // BLINK: count closed → open transitions
    private var blinks = 0
    private var eyesWereClosed = false

    // PERSPECTIVE: face width when the challenge started
    private var baselineWidth = 0f

    val isFinished: Boolean get() = phase == Phase.PASSED || phase == Phase.FAILED

    /**
     * Whether the next frame still needs the passive anti-spoofing score. Scoring liveness costs
     * more than reading pose and expression, so the challenge stage leaves it off.
     */
    val needsLiveness: Boolean get() = phase == Phase.ALIGN || phase == Phase.PASSIVE

    fun start() {
        phase = if (plan.isEmpty()) Phase.FAILED else Phase.ALIGN
        failedCheck = null
        failure = if (plan.isEmpty()) Failure.CHECK else Failure.NONE
        startedAt = SystemClock.elapsedRealtime()
        finishedAt = 0L
        restarts = 0
        lastFaceAt = startedAt
        resetProgress()
    }

    fun cancel() {
        if (isFinished) return
        failure = Failure.CANCELLED
        finish(Phase.FAILED)
    }

    /** Overall guard, on top of the per-stage budgets. */
    fun timeOut() {
        if (isFinished) return
        failure = Failure.TIMEOUT
        finish(Phase.FAILED)
    }

    /**
     * Feeds one analyzed frame.
     *
     * @param faces every face the engine found in the frame
     * @param frameWidth width of the analyzed frame, for judging how the face is framed
     * @param frameHeight height of the analyzed frame
     */
    fun onFrame(faces: List<DetectedFace>, frameWidth: Int, frameHeight: Int) {
        if (isFinished || phase == Phase.IDLE) return
        val now = SystemClock.elapsedRealtime()

        // Aligning is not counted per frame, so it gets a wall-clock budget of its own. It runs
        // from the last (re)start, so a restart is given the same chance as the first try.
        if (phase == Phase.ALIGN && now - alignStartedAt > ALIGN_BUDGET_MS) {
            failure = Failure.TIMEOUT
            finish(Phase.FAILED)
            return
        }

        if (faces.isEmpty()) {
            notice = Notice.NO_FACE
            holdFrames = 0
            // A blink of the tracker is not a lost face; a second of nothing is.
            if (phase != Phase.ALIGN && now - lastFaceAt > FACE_LOST_GRACE_MS) {
                restart()
            }
            return
        }
        lastFaceAt = now

        if (faces.size > 1) {
            notice = Notice.MANY_FACES
            holdFrames = 0
            return
        }
        val face = faces[0]

        when (phase) {
            Phase.ALIGN -> onAlignFrame(face, frameWidth, frameHeight)
            Phase.PASSIVE -> onPassiveFrame(face, frameWidth, frameHeight)
            Phase.RUNNING -> onChallengeFrame(face)
            else -> Unit
        }
    }

    /** Everything the attempt has achieved so far, back to stage one, with the same plan. */
    private fun restart() {
        restarts++
        phase = Phase.ALIGN
        resetProgress()
    }

    private fun onAlignFrame(face: DetectedFace, frameWidth: Int, frameHeight: Int) {
        val framing = framingNotice(face, frameWidth, frameHeight)
        notice = framing
        if (framing != Notice.NONE) return
        phase = Phase.PASSIVE
        passive = StepState.ACTIVE
        framesOnPassive = 0
        // The frame that passed alignment already carries a score when liveness was asked for.
        onPassiveFrame(face, frameWidth, frameHeight)
    }

    /** The anti-spoofing gate: average a few well-framed frames, then decide once. */
    private fun onPassiveFrame(face: DetectedFace, frameWidth: Int, frameHeight: Int) {
        framesOnPassive++

        val framing = framingNotice(face, frameWidth, frameHeight)
        if (framing != Notice.NONE) {
            // Say what is wrong instead of counting an unusable frame towards the verdict.
            notice = framing
            if (framesOnPassive >= options.framesWait) {
                failure = Failure.TIMEOUT
                finish(Phase.FAILED)
            }
            return
        }

        if (!face.hasLiveness) {
            // Well framed, and still no score: the engine could not judge this frame.
            notice = Notice.NOT_SCORABLE
            if (framesOnPassive >= options.framesWait) {
                passive = StepState.FAILED
                failure = Failure.SPOOF
                finish(Phase.FAILED)
            }
            return
        }

        notice = Notice.NONE
        passiveSum += face.liveness
        passiveCount++
        passiveScore = passiveSum / passiveCount

        if (passiveCount >= max(options.passiveSamples, 1)) {
            if (passiveScore >= options.livenessThreshold) {
                passive = StepState.PASSED
                phase = Phase.RUNNING
                advanceTo(0, face)
            } else {
                passive = StepState.FAILED
                failure = Failure.SPOOF
                finish(Phase.FAILED)
            }
        }
    }

    private fun onChallengeFrame(face: DetectedFace) {
        val check = plan.getOrNull(activeIndex) ?: return
        framesOnCheck++
        notice = Notice.NONE

        val result = evaluate(check, face)
        checkProgress = result.progress.coerceIn(0f, 1f)
        if (result.wrongWay) notice = Notice.WRONG_WAY

        if (result.satisfied) {
            holdFrames++
            if (holdFrames >= HOLD_FRAMES) {
                steps[activeIndex] = StepState.PASSED
                val next = activeIndex + 1
                if (next >= plan.size) {
                    finish(Phase.PASSED)
                } else {
                    advanceTo(next, face)
                }
                return
            }
        } else {
            holdFrames = 0
        }

        if (framesOnCheck >= options.framesWait) {
            failedCheck = check
            steps[activeIndex] = StepState.FAILED
            failure = Failure.CHECK
            finish(Phase.FAILED)
        }
    }

    fun snapshot(): State {
        val passed = steps.count { it == StepState.PASSED }
        // The gate is the first segment of the ring, the challenges share the rest.
        val segments = plan.size + 1
        val ring = when (phase) {
            Phase.PASSED -> 1f
            Phase.IDLE, Phase.ALIGN -> 0f
            Phase.PASSIVE -> {
                val wanted = max(options.passiveSamples, 1)
                (passiveCount.toFloat() / wanted / segments).coerceIn(0f, 1f / segments)
            }
            else -> {
                val base = (1f + passed) / segments
                (base + checkProgress / segments).coerceIn(0f, 1f).coerceAtLeast(base)
            }
        }
        val end = if (finishedAt > 0L) finishedAt else SystemClock.elapsedRealtime()
        return State(
            phase = phase,
            passive = passive,
            passiveScore = passiveScore,
            steps = steps.toList(),
            activeIndex = activeIndex,
            current = plan.getOrNull(activeIndex),
            progress = ring,
            passedCount = passed,
            total = plan.size,
            failedCheck = failedCheck,
            failure = failure,
            notice = notice,
            restarts = restarts,
            elapsedMs = if (startedAt > 0L) end - startedAt else 0L,
        )
    }

    // region framing

    /**
     * Whether this face can be judged at all, and if not, what the user should change.
     *
     * The order is the order a user would fix things in: get into the frame, get the distance
     * right, face the camera, then the light.
     */
    private fun framingNotice(face: DetectedFace, frameWidth: Int, frameHeight: Int): Notice {
        if (frameWidth > 0 && frameHeight > 0) {
            val bounds = face.bounds
            val shortSide = min(frameWidth, frameHeight).toFloat()
            val faceSide = max(face.width, face.height).toFloat()

            // Running past an edge means part of the face is simply not in the picture.
            val margin = shortSide * EDGE_MARGIN_FRACTION
            val cropped = bounds.left < -margin || bounds.top < -margin ||
                bounds.right > frameWidth + margin || bounds.bottom > frameHeight + margin
            if (cropped || faceSide > shortSide * MAX_FACE_FRACTION) return Notice.TOO_CLOSE

            if (faceSide < shortSide * options.minFaceFraction) return Notice.TOO_FAR

            val dx = abs(bounds.exactCenterX() - frameWidth / 2f) / frameWidth
            val dy = abs(bounds.exactCenterY() - frameHeight / 2f) / frameHeight
            if (dx > MAX_OFF_CENTER || dy > MAX_OFF_CENTER) return Notice.OFF_CENTER
        }

        if (!isAligned(face)) return Notice.LOOK_STRAIGHT

        if (!face.quality.isNaN() && face.quality < options.qualityThreshold) {
            return Notice.LOW_QUALITY
        }

        return Notice.NONE
    }

    /** The face has to be reasonably square to the camera before the attempt starts. */
    private fun isAligned(face: DetectedFace): Boolean {
        if (!face.hasPose) return true
        val limit = max(options.alignAngle, 1f)
        return abs(face.yaw) <= limit && abs(face.pitch) <= limit &&
            (face.roll.isNaN() || abs(face.roll) <= limit)
    }

    // endregion

    // region evaluation

    private data class Result(
        val satisfied: Boolean,
        val progress: Float,
        val wrongWay: Boolean = false,
    )

    private fun evaluate(check: ActiveCheck, face: DetectedFace): Result = when (check) {
        ActiveCheck.SMILE -> ratio(face.smile, options.smileThreshold)
        ActiveCheck.BLINK -> blink(face)
        ActiveCheck.TURN_LEFT, ActiveCheck.TURN_RIGHT ->
            angle(face.yaw, options.yawThreshold, options.yawSign(check))
        ActiveCheck.TURN_UP, ActiveCheck.TURN_DOWN ->
            angle(face.pitch, options.pitchThreshold, options.pitchSign(check))
        ActiveCheck.PERSPECTIVE -> perspective(face)
    }

    /** A plain "value reached threshold" check, for attributes the engine scores 0..1. */
    private fun ratio(value: Float, threshold: Float): Result {
        if (value.isNaN()) return Result(false, checkProgress)
        val target = max(threshold, 0.01f)
        return Result(value >= threshold, value / target)
    }

    /**
     * A head turn or tilt. [sign] is the direction this challenge asks for; going past the
     * threshold the other way is reported as [Notice.WRONG_WAY] rather than progress, so the user
     * is nudged instead of silently failing.
     */
    private fun angle(value: Float, threshold: Float, sign: Float): Result {
        if (value.isNaN()) return Result(false, checkProgress)
        val target = max(threshold, 1f)
        val towards = value * sign
        if (towards <= 0f && abs(value) >= target * WRONG_WAY_FRACTION) {
            return Result(false, 0f, wrongWay = true)
        }
        return Result(towards >= target, (towards / target).coerceAtLeast(0f))
    }

    /** Eyes closed then open again, [ActiveLivenessOptions.blinksNumber] times. */
    private fun blink(face: DetectedFace): Result {
        val eyesOpen = face.eyesOpen
        if (!eyesOpen.isNaN()) {
            val closed = eyesOpen <= options.blinkThreshold
            val open = eyesOpen >= options.blinkThreshold + BLINK_HYSTERESIS
            if (closed) {
                eyesWereClosed = true
            } else if (open && eyesWereClosed) {
                eyesWereClosed = false
                blinks++
            }
        }
        val target = max(options.blinksNumber, 1)
        return Result(blinks >= target, blinks.toFloat() / target)
    }

    /** The face fills more of the frame than it did when this challenge started. */
    private fun perspective(face: DetectedFace): Result {
        val width = face.width.toFloat()
        if (width <= 0f) return Result(false, checkProgress)
        if (baselineWidth <= 0f) {
            baselineWidth = width
            return Result(false, 0f)
        }
        val growth = width / baselineWidth - 1f
        val target = max(options.perspectiveThreshold, 0.01f)
        return Result(growth >= target, (growth / target).coerceAtLeast(0f))
    }

    // endregion

    private fun advanceTo(index: Int, face: DetectedFace) {
        activeIndex = index
        steps[index] = StepState.ACTIVE
        resetCheckState()
        // "Move closer" measures against the face size it starts from.
        if (plan[index] == ActiveCheck.PERSPECTIVE) {
            baselineWidth = face.width.toFloat()
        }
    }

    /** Back to stage one: no gate samples, no passed challenges, the align budget running again. */
    private fun resetProgress() {
        for (i in steps.indices) steps[i] = StepState.PENDING
        passive = StepState.PENDING
        activeIndex = -1
        notice = Notice.NONE
        passiveSum = 0f
        passiveCount = 0
        passiveScore = -1f
        framesOnPassive = 0
        alignStartedAt = SystemClock.elapsedRealtime()
        resetCheckState()
    }

    private fun resetCheckState() {
        checkProgress = 0f
        framesOnCheck = 0
        holdFrames = 0
        blinks = 0
        eyesWereClosed = false
        baselineWidth = 0f
    }

    private fun finish(result: Phase) {
        if (result == Phase.FAILED) {
            if (passive == StepState.ACTIVE) passive = StepState.FAILED
            if (activeIndex in steps.indices && steps[activeIndex] != StepState.PASSED) {
                steps[activeIndex] = StepState.FAILED
            }
        }
        if (result == Phase.PASSED) {
            passive = StepState.PASSED
            for (i in steps.indices) steps[i] = StepState.PASSED
            checkProgress = 1f
        }
        phase = result
        finishedAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        /** How long the face has to come into the frame and face the camera. */
        const val ALIGN_BUDGET_MS = 20_000L

        /** A face gone for longer than this starts the attempt over. */
        const val FACE_LOST_GRACE_MS = 1_000L

        /** Frames the condition has to hold, so one noisy frame cannot pass a challenge. */
        const val HOLD_FRAMES = 2

        /** Eyes count as open again this far above the closed threshold. */
        const val BLINK_HYSTERESIS = 0.2f

        /** Turning this far the wrong way earns a nudge. */
        const val WRONG_WAY_FRACTION = 0.6f

        /** Past this much of the frame's short side the face is too close to fit. */
        const val MAX_FACE_FRACTION = 0.92f

        /** How far outside the frame a face box may sit before it counts as cropped. */
        const val EDGE_MARGIN_FRACTION = 0.02f

        /** How far the face centre may sit from the frame centre, as a fraction of the frame. */
        const val MAX_OFF_CENTER = 0.22f
    }
}
