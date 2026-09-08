package com.faceplugin.faceliveness.kit

import android.graphics.PointF
import android.graphics.RectF

data class VideoWorkerActiveLiveness(
    val verdict: String,
    val checkType: String,
    val progress: Double,
)

data class VideoWorkerMatch(
    val matched: Boolean,
    val personIndex: Int?,
    val score: Double?,
)

data class VideoWorkerFace(
    val trackId: Int,
    val region: RectF,
    val landmarks: List<PointF>,
    val weak: Boolean,
    val match: VideoWorkerMatch?,
    val age: Double?,
    val gender: String?,
    val emotion: String?,
    val activeLiveness: VideoWorkerActiveLiveness?,
    val yaw: Double = 0.0,
    val pitch: Double = 0.0,
    val roll: Double = 0.0,
)

sealed class VideoWorkerEvent {
    data class Tracking(
        val frameId: Int,
        val faces: List<VideoWorkerFace>,
        val singleFace: Boolean,
        val frameWidth: Float,
        val frameHeight: Float,
    ) : VideoWorkerEvent()

    data class Match(
        val trackId: Int,
        val matched: Boolean,
        val personIndex: Int?,
        val score: Double?,
    ) : VideoWorkerEvent()
}
