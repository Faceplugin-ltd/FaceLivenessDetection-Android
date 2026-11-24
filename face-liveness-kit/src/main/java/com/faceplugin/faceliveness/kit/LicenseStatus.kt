package com.faceplugin.faceliveness.kit

import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK

/** What the activated key unlocks, as [FaceRecognitionSDK.license] reports it. */
data class LicenseStatus(
    val licensed: Boolean,
    val level: Int,
    val levelName: String,
    val recognition: Boolean,
    val liveness: Boolean,
    val label: String,
) {
    companion object {
        fun from(license: FaceRecognitionSDK.License?): LicenseStatus {
            if (license == null) return notLicensed()
            return LicenseStatus(
                licensed = license.licensed,
                level = license.level,
                levelName = license.levelName.orEmpty().ifBlank { "None" },
                recognition = license.recognition,
                liveness = license.liveness,
                label = license.label.orEmpty().ifBlank {
                    if (license.licensed) "Licensed" else "Not licensed"
                },
            )
        }

        fun notLicensed() = LicenseStatus(
            licensed = false,
            level = -1,
            levelName = "None",
            recognition = false,
            liveness = false,
            label = "Not licensed",
        )
    }
}
