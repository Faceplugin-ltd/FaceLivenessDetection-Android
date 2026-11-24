package com.faceplugin.faceliveness.kit

import android.graphics.Rect
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK

/**
 * What the app asks the engine for. Pose, eyes and expression are what the active-liveness
 * challenges are judged on; liveness is the passive anti-spoofing score.
 */
data class DetectOptions(
    val liveness: Boolean = false,
    val quality: Boolean = false,
    val pose: Boolean = false,
    val eyes: Boolean = false,
    val expression: Boolean = false,
    val points: Boolean = false,
    val ageGender: Boolean = false,
) {
    fun toSdk(): FaceRecognitionSDK.DetectOptions {
        val options = FaceRecognitionSDK.DetectOptions()
        options.liveness = liveness
        options.quality = quality
        options.pose = pose
        options.eyes = eyes
        options.expression = expression
        options.points = points
        options.ageGender = ageGender
        return options
    }

    companion object {
        /** Passive liveness screen: score, quality and pose for the HUD. */
        val PASSIVE = DetectOptions(liveness = true, quality = true, pose = true)

        /** Active liveness: everything the challenges read, plus the passive score. */
        val ACTIVE = DetectOptions(
            liveness = true,
            quality = true,
            pose = true,
            eyes = true,
            expression = true,
        )

        /** Active liveness while a challenge is running — no passive score, so it stays fast. */
        val ACTIVE_FAST = DetectOptions(pose = true, eyes = true, expression = true)
    }
}

/**
 * One detected face. The engine reports an attribute it was not asked for, or could not measure,
 * as `NaN`; this keeps that so callers can tell "not measured" from zero.
 */
data class DetectedFace(
    val bounds: Rect,
    val score: Float,
    val liveness: Float,
    val quality: Float,
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val eyesOpen: Float,
    val smile: Float,
    val mouthOpen: Float,
) {
    val width: Int get() = bounds.width()
    val height: Int get() = bounds.height()
    val area: Int get() = width * height

    val hasLiveness: Boolean get() = !liveness.isNaN()
    val hasPose: Boolean get() = !yaw.isNaN() && !pitch.isNaN()

    /** Label the engine's own operating point gives this score. */
    val livenessLabel: String
        get() = when {
            liveness.isNaN() -> ""
            liveness > FaceRecognitionSDK.LIVENESS_THRESHOLD -> "Live"
            else -> "Fake"
        }

    companion object {
        fun from(face: FaceRecognitionSDK.Face): DetectedFace = DetectedFace(
            bounds = Rect(face.bounds),
            score = face.score,
            liveness = face.liveness,
            quality = face.quality,
            yaw = face.yaw,
            pitch = face.pitch,
            roll = face.roll,
            eyesOpen = face.eyesOpen,
            smile = face.smile,
            mouthOpen = face.mouthOpen,
        )
    }
}
