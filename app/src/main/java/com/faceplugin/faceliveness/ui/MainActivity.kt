package com.faceplugin.faceliveness.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.faceplugin.faceliveness.R
import com.faceplugin.faceliveness.active.ActiveLivenessActivity
import com.faceplugin.faceliveness.active.ActiveLivenessResult
import com.faceplugin.faceliveness.data.AppSettings
import com.faceplugin.faceliveness.kit.FaceEngine
import com.faceplugin.faceliveness.kit.LicenseStatus
import com.faceplugin.facerecognitionsdk.FaceRecognitionSDK
import kotlin.math.roundToInt

/** Home: engine status, the two liveness demos, settings and about. */
class MainActivity : AppCompatActivity() {

    companion object {
        /** `FP1.…` from FacePlugin, bound to `applicationId` com.faceplugin.faceliveness. */
        private const val LICENSE_KEY =
            "FP1.RlBMMQMAAQARORvv0c1FobtWuyMQAgAAkX9YcEheIYzvFG39GkmX+M0o07lI/zACAYbcKsl0p2LBGXcRMWOaYAWAcXIjskNsTibM5SQfgrFLov/gSARKk2Cz4wdJfmlx5IBL21ur8E746qvBNBMfZfVSdnmJd2CfzoqgDlXmr4Ug7anbjoWCV9ITr477NyMAQoFBom+z2nH0kNFWGwCpqA0nYvY0+RBQpkGJ1HtYG5s/rh7UyTf2rgZBUXRWKb1yrIKfjulBehBhscY7KdiK9NaPFiWEwcne2vTqQ9UD/ZhvCk3UdKc2sX6h5T9tupwonCQn+CphrsXPZQscqvHtXtL9HbBRUzoz2kvUJvY9saphPOVpNlmD0nToUxUaqQ0PD6cItFEuxLa+DDK726lI7oL/SrpoLqxZ2w2bV3Pj1epiLVNEIC26YTlHz/tL/EKD/5tkrbFAFKXBJs3y6WaU0CYS3f+7Rr1tO6e5wLOw2mGXKNgBlxVUY+1W1oPu7lFL7gKsw2IuwUOIYLcY7tlt8roq6YL5ZGVbkwXFjmqEV5ToigE+UsywgweLF8ogkzo0aDolYB16+zGBDx2vBymPw3cFLndKyWWCxNh5sBDo92qcK/bK4q1sajRbS1XV2CrTbc2Zxt2KqjRs22dXeoOc/htIgmPot+Y4L/cxNqmPpXeRNlCGzMY8W8o8lhjlkYS+/Gr1XrfQA+9ryl1oP/DDLQYq3/UiqK8liwAwgYgCQgDPYWzI4el51CURcd5RRxDvGnZCXGSUJYQh3ps1FbfM9QC/mjVk3ApNH3Oqe7fli1Q6G5pgRInHXAI4LYxyUlo6UAJCARdFXfMKyyoh8pf2wd2tB7kVF0DJ414wvGcmjkMI336OYB1O6D8cbumlzQOa5BLTeOL9wAqXtaitVcptBZb1sIpC"
    }

    private lateinit var txtStatusNotification: TextView
    private var sdkReady = false
    private var livenessAllowed = false

    /** The active-liveness screen hands its verdict back, the way a host app would consume it. */
    private val activeLiveness = registerForActivityResult(StartActivityForResult()) { outcome ->
        ActiveLivenessActivity.result(outcome.data)?.let(::reportActiveResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.applyDefaults(this)
        setContentView(R.layout.activity_main)
        txtStatusNotification = findViewById(R.id.txtStatusNotification)

        findViewById<ImageView>(R.id.imgHomeLogo).setOnClickListener {
            openUrl(getString(R.string.company_website_url))
        }

        findViewById<View>(R.id.cardActive).setOnClickListener {
            if (!ensureLivenessAllowed()) return@setOnClickListener
            activeLiveness.launch(
                ActiveLivenessActivity.intent(
                    this,
                    AppSettings.activeLiveness(this),
                    AppSettings.cameraLens(this),
                ),
            )
        }
        findViewById<View>(R.id.cardLiveness).setOnClickListener {
            if (!ensureLivenessAllowed()) return@setOnClickListener
            startActivity(Intent(this, CameraActivity::class.java))
        }
        findViewById<View>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.cardAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        findViewById<View>(R.id.cardDocs).setOnClickListener {
            openUrl(getString(R.string.docs_url))
        }

        updateStatus(getString(R.string.sdk_loading), R.color.status_info)

        FaceEngine.start(this, LICENSE_KEY) { code ->
            sdkReady = code == FaceRecognitionSDK.SDK_SUCCESS
            FaceEngine.async {
                val status = FaceEngine.licenseStatus()
                runOnUiThread {
                    applyLicenseTiles(status)
                    if (sdkReady) {
                        updateStatus(
                            getString(R.string.sdk_ready_with_license, status.label),
                            R.color.status_ok,
                        )
                    } else {
                        updateStatus(licenseMessage(code), R.color.status_error)
                    }
                }
            }
        }
    }

    /** The demo only reports the verdict; a real integration would act on it. */
    private fun reportActiveResult(result: ActiveLivenessResult) {
        val message = when {
            result.passed -> getString(R.string.active_result_passed)
            !result.passivePassed -> getString(R.string.active_result_spoof)
            result.failedCheck != null ->
                getString(R.string.active_result_failed_check, getString(result.failedCheck!!.titleRes))
            else -> getString(R.string.active_result_failed)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun applyLicenseTiles(status: LicenseStatus) {
        livenessAllowed = status.liveness
        for (id in intArrayOf(R.id.cardActive, R.id.cardLiveness)) {
            val tile = findViewById<View>(id)
            tile.isEnabled = status.liveness
            tile.isClickable = status.liveness
            tile.alpha = if (status.liveness) 1f else 0.45f
        }
    }

    /** Status pill: the accent carries the state, so the fill stays a tint of it. */
    private fun updateStatus(message: String, colorResId: Int) {
        val accent = ContextCompat.getColor(this, colorResId)
        txtStatusNotification.text = message
        txtStatusNotification.setTextColor(accent)
        txtStatusNotification.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(ColorUtils.setAlphaComponent(accent, 0x26))
            setStroke(
                resources.displayMetrics.density.roundToInt(),
                ColorUtils.setAlphaComponent(accent, 0x66),
            )
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureLivenessAllowed(): Boolean {
        if (!sdkReady) {
            Toast.makeText(this, R.string.sdk_failed, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!livenessAllowed) {
            Toast.makeText(this, R.string.license_liveness_unavailable, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun licenseMessage(code: Int): String = when (code) {
        FaceRecognitionSDK.SDK_LICENSE_INVALID -> getString(R.string.sdk_license_invalid)
        FaceRecognitionSDK.SDK_LICENSE_EXPIRED -> getString(R.string.sdk_license_expired)
        FaceRecognitionSDK.SDK_NOT_ACTIVATED -> getString(R.string.sdk_not_activated)
        FaceRecognitionSDK.SDK_INIT_FAILED -> {
            val detail = FaceEngine.lastLicenseError().trim()
            if (detail.isNotEmpty()) {
                getString(R.string.sdk_init_failed) + " " + detail
            } else {
                getString(R.string.sdk_init_failed)
            }
        }
        else -> getString(R.string.sdk_failed) + ": $code"
    }
}
