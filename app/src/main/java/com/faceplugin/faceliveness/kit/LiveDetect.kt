package com.faceplugin.faceliveness.kit

import com.faceplugin.facelivenessdk.FaceBox
import com.faceplugin.facelivenessdk.FaceDetectionParam
import kotlin.math.max
import kotlin.math.min

/** Liveness detect params + merge of PB scores onto VideoWorker track boxes. */
object LiveDetect {
    @JvmStatic
    fun livenessOnly(level: Int): FaceDetectionParam {
        val p = FaceDetectionParam()
        p.check_pose = false
        p.check_landmarks = false
        p.check_liveness = true
        p.check_liveness_level = level
        return p
    }

    /** Copy passive-detect fields onto the tracked face box for overlay. */
    @JvmStatic
    fun mergePbDetect(track: List<FaceBox>, pb: List<FaceBox>?) {
        if (pb.isNullOrEmpty()) return
        for (dst in track) {
            val src = bestMatch(dst, pb) ?: continue
            dst.liveness = src.liveness
            dst.livenessLabel = src.livenessLabel
            dst.yaw = src.yaw
            dst.roll = src.roll
            dst.pitch = src.pitch
            dst.face_quality = src.face_quality
            dst.face_luminance = src.face_luminance
            if (src.score >= 0f) {
                dst.score = src.score
            } else if (src.face_quality > 0.001f) {
                dst.score = src.face_quality
            } else if (src.liveness > 0.001f) {
                dst.score = src.liveness
            }
        }
    }

    private fun bestMatch(dst: FaceBox, pb: List<FaceBox>): FaceBox? {
        if (pb.size == 1) return pb[0]
        var best: FaceBox? = null
        var bestIou = 0.1f
        for (src in pb) {
            val iou = iou(dst, src)
            if (iou > bestIou) {
                bestIou = iou
                best = src
            }
        }
        return best ?: pb[0]
    }

    private fun iou(a: FaceBox, b: FaceBox): Float {
        val left = max(a.x1, b.x1)
        val top = max(a.y1, b.y1)
        val right = min(a.x2, b.x2)
        val bottom = min(a.y2, b.y2)
        val inter = max(0, right - left) * max(0, bottom - top)
        val areaA = max(0, a.x2 - a.x1) * max(0, a.y2 - a.y1)
        val areaB = max(0, b.x2 - b.x1) * max(0, b.y2 - b.y1)
        val union = areaA + areaB - inter
        if (union <= 0) return 0f
        return inter.toFloat() / union
    }
}
