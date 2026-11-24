package com.faceplugin.faceliveness.active

import android.os.Bundle

/**
 * How one active-liveness attempt is run.
 *
 * [pool] is what the sequence may be drawn from, [count] how many challenges one attempt asks for,
 * and [randomize] shuffles the order per attempt so a recorded session cannot be replayed. Every
 * attempt begins with the passive anti-spoofing gate, which the face has to clear at
 * [livenessThreshold] before any challenge is asked.
 *
 * The thresholds are what [ActiveLivenessSession] judges each challenge against; the angles are
 * degrees, as the engine reports head pose. A host app usually builds this from its own settings
 * and hands it to [ActiveLivenessActivity.intent].
 */
data class ActiveLivenessOptions(
    val pool: List<ActiveCheck> = DEFAULT_POOL,
    val count: Int = DEFAULT_COUNT,
    val randomize: Boolean = DEFAULT_RANDOMIZE,
    val livenessThreshold: Float = DEFAULT_LIVENESS_THRESHOLD,
    val passiveSamples: Int = DEFAULT_PASSIVE_SAMPLES,
    val framesWait: Int = DEFAULT_FRAMES_WAIT,
    val yawThreshold: Float = DEFAULT_YAW,
    val pitchThreshold: Float = DEFAULT_PITCH,
    val smileThreshold: Float = DEFAULT_SMILE,
    val blinkThreshold: Float = DEFAULT_BLINK,
    val blinksNumber: Int = DEFAULT_BLINKS_NUMBER,
    val perspectiveThreshold: Float = DEFAULT_PERSPECTIVE,
    val alignAngle: Float = DEFAULT_ALIGN_ANGLE,
    /** Smallest the face may be, as a fraction of the frame's short side, to be scored. */
    val minFaceFraction: Float = DEFAULT_MIN_FACE,
    /** Lowest image quality the engine's own score may report and still be judged. */
    val qualityThreshold: Float = DEFAULT_QUALITY,
    val invertTurn: Boolean = false,
    val invertTilt: Boolean = false,
) {

    /** The challenge sequence for one attempt, in the order it will be asked. */
    fun buildPlan(): List<ActiveCheck> {
        val source = pool.ifEmpty { DEFAULT_POOL }
        val ordered = if (randomize) source.shuffled() else source
        return ordered.take(count.coerceIn(1, MAX_CHECKS))
    }

    /** +1 when the challenge wants a positive yaw, -1 when it wants a negative one. */
    fun yawSign(check: ActiveCheck): Float {
        val base = if (check == ActiveCheck.TURN_RIGHT) 1f else -1f
        return if (invertTurn) -base else base
    }

    /** +1 when the challenge wants a positive pitch, -1 when it wants a negative one. */
    fun pitchSign(check: ActiveCheck): Float {
        val base = if (check == ActiveCheck.TURN_UP) 1f else -1f
        return if (invertTilt) -base else base
    }

    fun toBundle(): Bundle = Bundle().apply {
        putStringArray(K_POOL, pool.map { it.key }.toTypedArray())
        putInt(K_COUNT, count)
        putBoolean(K_RANDOMIZE, randomize)
        putFloat(K_LIVENESS, livenessThreshold)
        putInt(K_SAMPLES, passiveSamples)
        putInt(K_FRAMES, framesWait)
        putFloat(K_YAW, yawThreshold)
        putFloat(K_PITCH, pitchThreshold)
        putFloat(K_SMILE, smileThreshold)
        putFloat(K_BLINK, blinkThreshold)
        putInt(K_BLINKS, blinksNumber)
        putFloat(K_PERSPECTIVE, perspectiveThreshold)
        putFloat(K_ALIGN, alignAngle)
        putFloat(K_MIN_FACE, minFaceFraction)
        putFloat(K_QUALITY, qualityThreshold)
        putBoolean(K_INVERT_TURN, invertTurn)
        putBoolean(K_INVERT_TILT, invertTilt)
    }

    companion object {
        const val MAX_CHECKS = 6

        /** Every challenge but "move closer", which not every placement of the phone allows. */
        @JvmField
        val DEFAULT_POOL = listOf(
            ActiveCheck.BLINK,
            ActiveCheck.SMILE,
            ActiveCheck.TURN_LEFT,
            ActiveCheck.TURN_RIGHT,
            ActiveCheck.TURN_UP,
            ActiveCheck.TURN_DOWN,
        )

        const val DEFAULT_COUNT = 3
        const val DEFAULT_RANDOMIZE = true

        /** The engine's own passive-liveness operating point. */
        const val DEFAULT_LIVENESS_THRESHOLD = 0.5f

        /** Frames the passive gate averages before it decides. */
        const val DEFAULT_PASSIVE_SAMPLES = 3

        /** About 15 analyzed frames a second on a mid-range phone. */
        const val DEFAULT_FRAMES_WAIT = 150
        const val DEFAULT_YAW = 25f
        const val DEFAULT_PITCH = 15f
        const val DEFAULT_SMILE = 0.5f
        const val DEFAULT_BLINK = 0.3f
        const val DEFAULT_BLINKS_NUMBER = 2
        const val DEFAULT_PERSPECTIVE = 0.15f
        const val DEFAULT_ALIGN_ANGLE = 12f

        /** A face smaller than this much of the frame is too far away to score reliably. */
        const val DEFAULT_MIN_FACE = 0.3f

        /** The engine's `ImageQuality` below which a frame is not worth judging. */
        const val DEFAULT_QUALITY = 0.3f

        private const val K_POOL = "pool"
        private const val K_COUNT = "count"
        private const val K_RANDOMIZE = "randomize"
        private const val K_LIVENESS = "liveness"
        private const val K_SAMPLES = "samples"
        private const val K_FRAMES = "frames"
        private const val K_YAW = "yaw"
        private const val K_PITCH = "pitch"
        private const val K_SMILE = "smile"
        private const val K_BLINK = "blink"
        private const val K_BLINKS = "blinks"
        private const val K_PERSPECTIVE = "perspective"
        private const val K_ALIGN = "align"
        private const val K_MIN_FACE = "minFace"
        private const val K_QUALITY = "quality"
        private const val K_INVERT_TURN = "invertTurn"
        private const val K_INVERT_TILT = "invertTilt"

        @JvmStatic
        fun fromBundle(bundle: Bundle?): ActiveLivenessOptions {
            if (bundle == null) return ActiveLivenessOptions()
            val keys = bundle.getStringArray(K_POOL)?.toSet().orEmpty()
            // Keep the canonical enum order so the pool is stable when randomize is off.
            val pool = ActiveCheck.entries.filter { keys.contains(it.key) }
            val defaults = ActiveLivenessOptions()
            return ActiveLivenessOptions(
                pool = pool.ifEmpty { DEFAULT_POOL },
                count = bundle.getInt(K_COUNT, defaults.count),
                randomize = bundle.getBoolean(K_RANDOMIZE, defaults.randomize),
                livenessThreshold = bundle.getFloat(K_LIVENESS, defaults.livenessThreshold),
                passiveSamples = bundle.getInt(K_SAMPLES, defaults.passiveSamples),
                framesWait = bundle.getInt(K_FRAMES, defaults.framesWait),
                yawThreshold = bundle.getFloat(K_YAW, defaults.yawThreshold),
                pitchThreshold = bundle.getFloat(K_PITCH, defaults.pitchThreshold),
                smileThreshold = bundle.getFloat(K_SMILE, defaults.smileThreshold),
                blinkThreshold = bundle.getFloat(K_BLINK, defaults.blinkThreshold),
                blinksNumber = bundle.getInt(K_BLINKS, defaults.blinksNumber),
                perspectiveThreshold = bundle.getFloat(K_PERSPECTIVE, defaults.perspectiveThreshold),
                alignAngle = bundle.getFloat(K_ALIGN, defaults.alignAngle),
                minFaceFraction = bundle.getFloat(K_MIN_FACE, defaults.minFaceFraction),
                qualityThreshold = bundle.getFloat(K_QUALITY, defaults.qualityThreshold),
                invertTurn = bundle.getBoolean(K_INVERT_TURN, defaults.invertTurn),
                invertTilt = bundle.getBoolean(K_INVERT_TILT, defaults.invertTilt),
            )
        }
    }
}
