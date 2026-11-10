package com.faceplugin.faceliveness.kit

import com.faceplugin.facelivenessdk.FaceLivenessSDK
import org.json.JSONObject

/** Parsed [FaceLivenessSDK.getLicenseStatus] for UI and capability checks. */
data class LicenseStatus(
    val licensed: Boolean,
    val level: Int,
    val levelName: String,
    val recognition: Boolean,
    val liveness: Boolean,
    val label: String,
) {
    companion object {
        fun current(): LicenseStatus {
            return try {
                fromJson(FaceLivenessSDK.getLicenseStatus())
            } catch (_: Throwable) {
                notLicensed()
            }
        }

        fun fromJson(json: String?): LicenseStatus {
            return try {
                val o = JSONObject(json ?: "{}")
                LicenseStatus(
                    licensed = o.optBoolean("licensed", false),
                    level = o.optInt("level", -1),
                    levelName = o.optString("levelName", "None"),
                    recognition = o.optBoolean("recognition", false),
                    liveness = o.optBoolean("liveness", false),
                    label = o.optString("label", "Not licensed"),
                )
            } catch (_: Exception) {
                notLicensed()
            }
        }

        private fun notLicensed() = LicenseStatus(
            licensed = false,
            level = -1,
            levelName = "None",
            recognition = false,
            liveness = false,
            label = "Not licensed",
        )
    }
}
