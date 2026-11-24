package com.faceplugin.faceliveness.active

import android.os.Bundle

/**
 * What one attempt came to, handed back to the host app by
 * [ActiveLivenessActivity].
 *
 * [passed] is the only thing most callers need: it is true when the passive anti-spoofing gate
 * cleared *and* every challenge was completed. [failure] says why it did not.
 */
data class ActiveLivenessResult(
    val passed: Boolean,
    val failure: ActiveLivenessSession.Failure,
    /** Averaged passive anti-spoofing score, or -1 when the engine never returned one. */
    val passiveScore: Float,
    val passivePassed: Boolean,
    /** The challenges asked, in the order they were asked. */
    val challenges: List<ActiveCheck>,
    /** One outcome per entry of [challenges]. */
    val outcomes: List<ActiveLivenessSession.StepState>,
    val durationMs: Long,
) {

    /** The challenge that ended the attempt, when one did. */
    val failedCheck: ActiveCheck?
        get() = outcomes.indexOfFirst { it == ActiveLivenessSession.StepState.FAILED }
            .takeIf { it >= 0 }
            ?.let { challenges.getOrNull(it) }

    fun toBundle(): Bundle = Bundle().apply {
        putBoolean(K_PASSED, passed)
        putString(K_FAILURE, failure.name)
        putFloat(K_SCORE, passiveScore)
        putBoolean(K_PASSIVE_PASSED, passivePassed)
        putStringArray(K_CHALLENGES, challenges.map { it.key }.toTypedArray())
        putStringArray(K_OUTCOMES, outcomes.map { it.name }.toTypedArray())
        putLong(K_DURATION, durationMs)
    }

    companion object {
        private const val K_PASSED = "passed"
        private const val K_FAILURE = "failure"
        private const val K_SCORE = "score"
        private const val K_PASSIVE_PASSED = "passivePassed"
        private const val K_CHALLENGES = "challenges"
        private const val K_OUTCOMES = "outcomes"
        private const val K_DURATION = "duration"

        @JvmStatic
        fun fromBundle(bundle: Bundle?): ActiveLivenessResult? {
            if (bundle == null) return null
            val challenges = bundle.getStringArray(K_CHALLENGES).orEmpty()
                .mapNotNull { ActiveCheck.fromKey(it) }
            val outcomes = bundle.getStringArray(K_OUTCOMES).orEmpty().map { name ->
                runCatching { ActiveLivenessSession.StepState.valueOf(name) }
                    .getOrDefault(ActiveLivenessSession.StepState.PENDING)
            }
            return ActiveLivenessResult(
                passed = bundle.getBoolean(K_PASSED, false),
                failure = runCatching {
                    ActiveLivenessSession.Failure.valueOf(bundle.getString(K_FAILURE).orEmpty())
                }.getOrDefault(ActiveLivenessSession.Failure.NONE),
                passiveScore = bundle.getFloat(K_SCORE, -1f),
                passivePassed = bundle.getBoolean(K_PASSIVE_PASSED, false),
                challenges = challenges,
                outcomes = outcomes,
                durationMs = bundle.getLong(K_DURATION, 0L),
            )
        }

        @JvmStatic
        fun from(
            state: ActiveLivenessSession.State,
            plan: List<ActiveCheck>,
        ): ActiveLivenessResult = ActiveLivenessResult(
            passed = state.phase == ActiveLivenessSession.Phase.PASSED,
            failure = state.failure,
            passiveScore = state.passiveScore,
            passivePassed = state.passive == ActiveLivenessSession.StepState.PASSED,
            challenges = plan,
            outcomes = state.steps,
            durationMs = state.elapsedMs,
        )
    }
}
