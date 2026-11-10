package com.faceplugin.faceliveness.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.faceplugin.faceliveness.R
import com.faceplugin.faceliveness.kit.FaceLivenessClient
import com.faceplugin.faceliveness.kit.LicenseStatus
import com.faceplugin.facelivenessdk.FaceLivenessSDK

class MainActivity : AppCompatActivity() {

    companion object {
        /** `FP1.…` from FacePlugin for `applicationId` com.faceplugin.faceliveness (product 1000). */
        private const val LICENSE_KEY =
            "FP1.RlBMMQMAAQDfQZe8TE8hh/zbavUQAgAADRWhDmeyhhWiygwqwSRBO4RcdGBYwI0vl6n1u9G6ln/fjOmqcZ4zyD9NJ6tWzGrprchs+LqKePwq5nGIZ9asFLj8Y9lhK28nv75odoE5NkMxrqDWSoqwqW53Jcu8enPS7WYDHsgG1cFzl6kOdSzP0enyscYPZ3+4C7oRMtkxXFBT7jjXAEbshcEGaokhyBUDB1jRxTv1s+qzRFz90Zxo7p3IY71e5K4iX7v0dimhB4wMl3TF2y1BsEQpN8CTTF5gEbxP1uClS+wWLft1IcBn6i8+vPqltWr7yhLDJp6V59WZ4UDJ2F5eIJ9GtR8XfXdPONTn0eoIgmu1N+N8QBwIpb5VT+zsSyNXPxPXVhdRUCdZbkDY9ssflYiPJneI8y/RYy2x5aB6JsjcWaP424KpMfpkWpRC+pzAoNNN2sE11Td56SGu1APPZgleyKZUhPpsS04EgpSLCjYBzZjc0et9Qs9iGnRJvrpP4vXjf/tn9crUZNQA/X6ISEyqPBdEcgdu4JDh+U18P5d2I39pwXEtk/M98Mn7Pos6GgyQp38WE+nW2B7ge9a0yZIRtR6BueoibBh1MuiboOlkzjVGtX1VwcWJ7E3LGbwth1SqwGdyurBF7MMmzdhBc6pMGXd0zubAeMjihKloh5DIjooJ0hgUjGNntmCQL8hMcmMpYZ13jzul4NxY2hdJXnl26tyG9HujiQAwgYYCQR7UBbHaMW3i0FmjMoMMgnp6Sx2THWIZ7ZO/8oFt3N4dw0xCXSuV/QoZcdpB0vNNW7jBXZtlvC4xi6MM/vt+qYqAAkF9a8Wh1E/2podA8ky71GgAazjeRsYYR4DjtYB8aV/+bxNu00PMbZ3HiVwbNUlPGIXGK7Z8oWd8XYwAjlHvQmCEKg=="
    }

    private lateinit var txtStatusNotification: TextView
    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsActivity.applyEngineDefaults(this)
        setContentView(R.layout.activity_main)
        txtStatusNotification = findViewById(R.id.txtStatusNotification)

        findViewById<ImageView>(R.id.imgHomeLogo).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.company_website_url))))
        }

        findViewById<View>(R.id.cardLiveness).setOnClickListener {
            if (!ensureReady()) return@setOnClickListener
            if (!LicenseStatus.current().liveness) {
                Toast.makeText(this, R.string.license_liveness_unavailable, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<View>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.cardAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        updateStatus(getString(R.string.sdk_loading), R.color.status_info)

        FaceLivenessClient.get(this).activate(LICENSE_KEY) { code ->
            sdkReady = code == FaceLivenessSDK.SDK_SUCCESS
            runOnUiThread {
                if (sdkReady) {
                    val status = LicenseStatus.current()
                    applyLicenseTiles(status)
                    updateStatus(getString(R.string.sdk_ready_with_license, status.label), R.color.status_ok)
                } else {
                    applyLicenseTiles(LicenseStatus.current())
                    updateStatus(licenseMessage(code), R.color.status_error)
                }
            }
        }
    }

    private fun applyLicenseTiles(status: LicenseStatus) {
        val button = findViewById<View>(R.id.cardLiveness)
        button.isEnabled = status.liveness
        button.isClickable = status.liveness
        button.alpha = if (status.liveness) 1f else 0.45f
    }

    private fun updateStatus(message: String, colorResId: Int) {
        txtStatusNotification.text = message
        txtStatusNotification.background?.setTint(ContextCompat.getColor(this, colorResId))
    }

    private fun ensureReady(): Boolean {
        if (sdkReady) return true
        Toast.makeText(this, R.string.sdk_failed, Toast.LENGTH_SHORT).show()
        return false
    }

    private fun licenseMessage(code: Int): String = when (code) {
        FaceLivenessSDK.SDK_LICENSE_INVALID -> getString(R.string.sdk_license_invalid)
        FaceLivenessSDK.SDK_LICENSE_EXPIRED -> getString(R.string.sdk_license_expired)
        FaceLivenessSDK.SDK_NOT_ACTIVATED -> getString(R.string.sdk_not_activated)
        FaceLivenessSDK.SDK_INIT_FAILED -> {
            val detail = FaceLivenessSDK.lastEngineError().orEmpty().trim()
            if (detail.isNotEmpty()) {
                getString(R.string.sdk_init_failed) + " " + detail
            } else {
                getString(R.string.sdk_init_failed)
            }
        }
        else -> getString(R.string.sdk_failed) + ": $code"
    }
}
