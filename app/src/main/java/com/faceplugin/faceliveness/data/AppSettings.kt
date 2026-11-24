package com.faceplugin.faceliveness.data

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.core.CameraSelector
import androidx.preference.PreferenceManager
import com.faceplugin.faceliveness.active.ActiveCheck
import com.faceplugin.faceliveness.active.ActiveLivenessOptions

/**
 * Every preference this demo stores, in one place.
 *
 * The libraries take their configuration as arguments, so this is the only part of the app that
 * knows about `SharedPreferences`: it reads what the user chose and builds
 * [ActiveLivenessOptions] for the active-liveness screen.
 *
 * Defaults are the engine's own operating points — passive liveness 0.5, head turn 25°, tilt 15°.
 */
object AppSettings {

    const val KEY_CAMERA_LENS = "camera_lens"
    const val KEY_LIVENESS_THRESHOLD = "liveness_threshold"

    const val KEY_CHECKS = "active_checks"
    const val KEY_COUNT = "active_check_count"
    const val KEY_RANDOMIZE = "active_randomize"
    const val KEY_FRAMES_WAIT = "active_frames_wait"
    const val KEY_YAW = "active_yaw"
    const val KEY_PITCH = "active_pitch"
    const val KEY_SMILE = "active_smile"
    const val KEY_BLINK = "active_blink"
    const val KEY_BLINKS_NUMBER = "active_blinks_number"
    const val KEY_PERSPECTIVE = "active_perspective"
    const val KEY_ALIGN_ANGLE = "active_align_angle"
    const val KEY_MIN_FACE = "active_min_face"
    const val KEY_QUALITY = "active_quality"
    const val KEY_INVERT_TURN = "active_invert_turn"
    const val KEY_INVERT_TILT = "active_invert_tilt"

    const val DEFAULT_CAMERA_LENS = "front"
    val DEFAULT_LIVENESS_THRESHOLD = ActiveLivenessOptions.DEFAULT_LIVENESS_THRESHOLD.toString()
    val DEFAULT_COUNT = ActiveLivenessOptions.DEFAULT_COUNT.toString()
    const val DEFAULT_RANDOMIZE = ActiveLivenessOptions.DEFAULT_RANDOMIZE
    val DEFAULT_FRAMES_WAIT = ActiveLivenessOptions.DEFAULT_FRAMES_WAIT.toString()
    val DEFAULT_YAW = trimFloat(ActiveLivenessOptions.DEFAULT_YAW)
    val DEFAULT_PITCH = trimFloat(ActiveLivenessOptions.DEFAULT_PITCH)
    val DEFAULT_SMILE = ActiveLivenessOptions.DEFAULT_SMILE.toString()
    val DEFAULT_BLINK = ActiveLivenessOptions.DEFAULT_BLINK.toString()
    val DEFAULT_BLINKS_NUMBER = ActiveLivenessOptions.DEFAULT_BLINKS_NUMBER.toString()
    val DEFAULT_PERSPECTIVE = ActiveLivenessOptions.DEFAULT_PERSPECTIVE.toString()
    val DEFAULT_ALIGN_ANGLE = trimFloat(ActiveLivenessOptions.DEFAULT_ALIGN_ANGLE)
    val DEFAULT_MIN_FACE = ActiveLivenessOptions.DEFAULT_MIN_FACE.toString()
    val DEFAULT_QUALITY = ActiveLivenessOptions.DEFAULT_QUALITY.toString()
    const val DEFAULT_INVERT = false

    val DEFAULT_CHECK_KEYS: Set<String> =
        ActiveLivenessOptions.DEFAULT_POOL.map { it.key }.toSet()

    private const val PREFS_SCHEMA = "prefs_schema"
    /** Bumped with the move to the facerecognitionsdk runtime. */
    private const val PREFS_SCHEMA_LX = 5

    /**
     * Seeds the defaults this build expects. The thresholds belong to the engine, so a build that
     * changes engines re-seeds them even where an older set is already stored.
     */
    @JvmStatic
    fun applyDefaults(context: Context) {
        val prefs = prefs(context)
        if (prefs.getInt(PREFS_SCHEMA, 0) >= PREFS_SCHEMA_LX) return
        prefs.edit()
            .putString(KEY_CAMERA_LENS, DEFAULT_CAMERA_LENS)
            .putString(KEY_LIVENESS_THRESHOLD, DEFAULT_LIVENESS_THRESHOLD)
            .putStringSet(KEY_CHECKS, DEFAULT_CHECK_KEYS)
            .putString(KEY_COUNT, DEFAULT_COUNT)
            .putBoolean(KEY_RANDOMIZE, DEFAULT_RANDOMIZE)
            .putString(KEY_FRAMES_WAIT, DEFAULT_FRAMES_WAIT)
            .putString(KEY_YAW, DEFAULT_YAW)
            .putString(KEY_PITCH, DEFAULT_PITCH)
            .putString(KEY_SMILE, DEFAULT_SMILE)
            .putString(KEY_BLINK, DEFAULT_BLINK)
            .putString(KEY_BLINKS_NUMBER, DEFAULT_BLINKS_NUMBER)
            .putString(KEY_PERSPECTIVE, DEFAULT_PERSPECTIVE)
            .putString(KEY_ALIGN_ANGLE, DEFAULT_ALIGN_ANGLE)
            .putString(KEY_MIN_FACE, DEFAULT_MIN_FACE)
            .putString(KEY_QUALITY, DEFAULT_QUALITY)
            .putBoolean(KEY_INVERT_TURN, DEFAULT_INVERT)
            .putBoolean(KEY_INVERT_TILT, DEFAULT_INVERT)
            .putInt(PREFS_SCHEMA, PREFS_SCHEMA_LX)
            .apply()
    }

    @JvmStatic
    fun cameraLens(context: Context): Int =
        if (prefs(context).getString(KEY_CAMERA_LENS, DEFAULT_CAMERA_LENS) == "back") {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

    @JvmStatic
    fun setCameraLens(context: Context, lensFacing: Int) {
        val value = if (lensFacing == CameraSelector.LENS_FACING_BACK) "back" else "front"
        prefs(context).edit().putString(KEY_CAMERA_LENS, value).apply()
    }

    @JvmStatic
    fun livenessThreshold(context: Context): Float =
        prefs(context).float(KEY_LIVENESS_THRESHOLD, DEFAULT_LIVENESS_THRESHOLD)

    /** Whether a passive score counts as a live face at the user's threshold. */
    @JvmStatic
    fun livenessPassed(context: Context, score: Float, label: String?): Boolean {
        val lower = label.orEmpty().lowercase()
        if (lower.contains("spoof") || lower.contains("fake")) return false
        return score >= livenessThreshold(context)
    }

    /** What the active-liveness screen should run, as the user has it configured. */
    @JvmStatic
    fun activeLiveness(context: Context): ActiveLivenessOptions {
        val prefs = prefs(context)
        val keys = prefs.getStringSet(KEY_CHECKS, DEFAULT_CHECK_KEYS).orEmpty()
        // Keep the canonical enum order so the pool is stable when randomize is off.
        val pool = ActiveCheck.entries.filter { keys.contains(it.key) }
        return ActiveLivenessOptions(
            pool = pool.ifEmpty { ActiveLivenessOptions.DEFAULT_POOL },
            count = prefs.int(KEY_COUNT, DEFAULT_COUNT),
            randomize = prefs.getBoolean(KEY_RANDOMIZE, DEFAULT_RANDOMIZE),
            livenessThreshold = livenessThreshold(context),
            framesWait = prefs.int(KEY_FRAMES_WAIT, DEFAULT_FRAMES_WAIT).coerceIn(30, 600),
            yawThreshold = prefs.float(KEY_YAW, DEFAULT_YAW),
            pitchThreshold = prefs.float(KEY_PITCH, DEFAULT_PITCH),
            smileThreshold = prefs.float(KEY_SMILE, DEFAULT_SMILE),
            blinkThreshold = prefs.float(KEY_BLINK, DEFAULT_BLINK),
            blinksNumber = prefs.int(KEY_BLINKS_NUMBER, DEFAULT_BLINKS_NUMBER).coerceIn(1, 6),
            perspectiveThreshold = prefs.float(KEY_PERSPECTIVE, DEFAULT_PERSPECTIVE),
            alignAngle = prefs.float(KEY_ALIGN_ANGLE, DEFAULT_ALIGN_ANGLE),
            minFaceFraction = prefs.float(KEY_MIN_FACE, DEFAULT_MIN_FACE),
            qualityThreshold = prefs.float(KEY_QUALITY, DEFAULT_QUALITY),
            invertTurn = prefs.getBoolean(KEY_INVERT_TURN, DEFAULT_INVERT),
            invertTilt = prefs.getBoolean(KEY_INVERT_TILT, DEFAULT_INVERT),
        )
    }

    private fun prefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private fun SharedPreferences.int(key: String, fallback: String): Int =
        getString(key, fallback)?.trim()?.toIntOrNull() ?: fallback.toInt()

    private fun SharedPreferences.float(key: String, fallback: String): Float =
        getString(key, fallback)?.trim()?.toFloatOrNull() ?: fallback.toFloat()

    /** `25.0` reads badly in a settings field; `25` does. */
    private fun trimFloat(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
}
