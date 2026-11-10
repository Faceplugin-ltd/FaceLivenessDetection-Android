package com.faceplugin.faceliveness.kit

import android.graphics.PointF
import android.graphics.RectF
import com.faceplugin.facelivenessdk.FaceBox
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

/** Parses VideoWorker JSON events used by the live camera overlay. */
object FaceJson {
    @JvmStatic
    fun parseVideoWorkerEvent(json: String?): VideoWorkerEvent? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            when (root.optString("event")) {
                "tracking" -> {
                    val faces = parseVideoWorkerFaces(root.optJSONArray("faces"))
                    val fw = doubleValue(root.opt("frame_width")) ?: 0.0
                    val fh = doubleValue(root.opt("frame_height")) ?: 0.0
                    VideoWorkerEvent.Tracking(
                        frameId = intValue(root.opt("frame_id")) ?: 0,
                        faces = faces,
                        singleFace = root.optBoolean("single_face", faces.size == 1),
                        frameWidth = fw.toFloat(),
                        frameHeight = fh.toFloat(),
                    )
                }
                "match" -> VideoWorkerEvent.Match(
                    trackId = intValue(root.opt("track_id")) ?: 0,
                    matched = root.optBoolean("matched", false),
                    personIndex = intValue(root.opt("person_index")),
                    score = doubleValue(root.opt("score")),
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun toFaceBoxes(faces: List<VideoWorkerFace>, includeWeak: Boolean = false): List<FaceBox> =
        faces.filter { includeWeak || !it.weak }.map { toFaceBox(it) }

    @JvmStatic
    fun toFaceBox(face: VideoWorkerFace): FaceBox {
        val box = FaceBox()
        box.x1 = face.region.left.roundToInt()
        box.y1 = face.region.top.roundToInt()
        box.x2 = face.region.right.roundToInt()
        box.y2 = face.region.bottom.roundToInt()
        box.yaw = face.yaw.toFloat()
        box.pitch = face.pitch.toFloat()
        box.roll = face.roll.toFloat()
        box.trackId = face.trackId
        val matchScore = face.match?.score
        if (matchScore != null) {
            box.score = matchScore.toFloat()
        }
        val n = min(face.landmarks.size, box.landmarks_68.size / 2)
        box.landmarkCount = n
        for (i in 0 until n) {
            box.landmarks_68[i * 2] = face.landmarks[i].x
            box.landmarks_68[i * 2 + 1] = face.landmarks[i].y
        }
        return box
    }

    private fun parseVideoWorkerFaces(faces: JSONArray?): List<VideoWorkerFace> {
        if (faces == null) return emptyList()
        return buildList {
            for (i in 0 until faces.length()) {
                val face = faces.optJSONObject(i) ?: continue
                val regionObj = face.optJSONObject("faceRegion") ?: continue
                val x = doubleValue(regionObj.opt("x")) ?: continue
                val y = doubleValue(regionObj.opt("y")) ?: continue
                val w = doubleValue(regionObj.opt("width")) ?: continue
                val h = doubleValue(regionObj.opt("height")) ?: continue
                val matchRaw = face.optJSONObject("match")
                val match = if (matchRaw != null) {
                    VideoWorkerMatch(
                        matched = matchRaw.optBoolean("matched", false),
                        personIndex = intValue(matchRaw.opt("person_index")),
                        score = doubleValue(matchRaw.opt("score")),
                    )
                } else {
                    null
                }
                val pose = face.optJSONObject("facePose")
                add(
                    VideoWorkerFace(
                        trackId = intValue(face.opt("track_id")) ?: 0,
                        region = RectF(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat()),
                        landmarks = parseLandmarks(face.optJSONArray("facePoints")),
                        weak = face.optBoolean("weak", false),
                        match = match,
                        age = doubleValue(face.opt("age")),
                        gender = face.optString("gender").takeIf { it.isNotBlank() },
                        emotion = face.optString("emotion").takeIf { it.isNotBlank() },
                        activeLiveness = parseActiveLiveness(face.optJSONObject("activeLiveness")),
                        yaw = doubleValue(pose?.opt("yaw")) ?: 0.0,
                        pitch = doubleValue(pose?.opt("pitch")) ?: 0.0,
                        roll = doubleValue(pose?.opt("roll")) ?: 0.0,
                    ),
                )
            }
        }
    }

    private fun parseActiveLiveness(raw: JSONObject?): VideoWorkerActiveLiveness? {
        if (raw == null) return null
        return VideoWorkerActiveLiveness(
            verdict = raw.optString("verdict", "not_computed"),
            checkType = raw.optString("checkType", "none"),
            progress = doubleValue(raw.opt("progress")) ?: 0.0,
        )
    }

    private fun parseLandmarks(raw: JSONArray?): List<PointF> {
        if (raw == null) return emptyList()
        return buildList {
            for (i in 0 until raw.length()) {
                val pt = raw.optJSONObject(i) ?: continue
                val x = doubleValue(pt.opt("x")) ?: continue
                val y = doubleValue(pt.opt("y")) ?: continue
                add(PointF(x.toFloat(), y.toFloat()))
            }
        }
    }

    private fun intValue(raw: Any?): Int? = when (raw) {
        null, JSONObject.NULL -> null
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }

    private fun doubleValue(raw: Any?): Double? = when (raw) {
        null, JSONObject.NULL -> null
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }
}
