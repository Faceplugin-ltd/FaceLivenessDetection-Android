package com.faceplugin.faceliveness.active

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * One active-liveness challenge.
 *
 * The engine reports face attributes per frame — head pose, eye openness, smile, face size — and
 * [ActiveLivenessSession] decides from those when a challenge is satisfied. This enum is the
 * description of it: what to show, and the key the host app stores it under.
 */
enum class ActiveCheck(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val hintRes: Int,
    @DrawableRes val iconRes: Int,
) {
    BLINK(
        "blink",
        R.string.fp_al_check_blink,
        R.string.fp_al_check_blink_hint,
        R.drawable.fp_al_ic_blink,
    ),
    SMILE(
        "smile",
        R.string.fp_al_check_smile,
        R.string.fp_al_check_smile_hint,
        R.drawable.fp_al_ic_smile,
    ),
    TURN_LEFT(
        "turn_left",
        R.string.fp_al_check_turn_left,
        R.string.fp_al_check_turn_left_hint,
        R.drawable.fp_al_ic_turn_left,
    ),
    TURN_RIGHT(
        "turn_right",
        R.string.fp_al_check_turn_right,
        R.string.fp_al_check_turn_right_hint,
        R.drawable.fp_al_ic_turn_right,
    ),
    TURN_UP(
        "turn_up",
        R.string.fp_al_check_turn_up,
        R.string.fp_al_check_turn_up_hint,
        R.drawable.fp_al_ic_turn_up,
    ),
    TURN_DOWN(
        "turn_down",
        R.string.fp_al_check_turn_down,
        R.string.fp_al_check_turn_down_hint,
        R.drawable.fp_al_ic_turn_down,
    ),
    PERSPECTIVE(
        "perspective",
        R.string.fp_al_check_perspective,
        R.string.fp_al_check_perspective_hint,
        R.drawable.fp_al_ic_closer,
    ),
    ;

    /** True for the challenges where turning the other way is a recoverable mistake. */
    val isDirectional: Boolean
        get() = this == TURN_LEFT || this == TURN_RIGHT || this == TURN_UP || this == TURN_DOWN

    companion object {
        @JvmStatic
        fun fromKey(key: String?): ActiveCheck? {
            if (key.isNullOrBlank()) return null
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        }
    }
}
