package com.faceplugin.faceliveness.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.faceplugin.faceliveness.R

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_CAMERA_LENS = "front"
        const val DEFAULT_LIVENESS_THRESHOLD = "0.5"
        /** 0 = high-accuracy 2d_ensemble_light (only pack shipped in this product). */
        const val DEFAULT_LIVENESS_LEVEL = "0"

        private const val PREFS_SCHEMA = "prefs_schema"
        private const val PREFS_SCHEMA_LIVENESS = 2

        @JvmStatic
        fun applyEngineDefaults(context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getInt(PREFS_SCHEMA, 0) >= PREFS_SCHEMA_LIVENESS) return
            prefs.edit()
                .putString("camera_lens", DEFAULT_CAMERA_LENS)
                .putString("liveness_threshold", DEFAULT_LIVENESS_THRESHOLD)
                .putString("liveness_level", DEFAULT_LIVENESS_LEVEL)
                .putInt(PREFS_SCHEMA, PREFS_SCHEMA_LIVENESS)
                .apply()
        }

        @JvmStatic
        fun getLivenessThreshold(context: Context): Float {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getString("liveness_threshold", DEFAULT_LIVENESS_THRESHOLD)!!.toFloat()
        }

        @JvmStatic
        fun getCameraLens(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (sharedPreferences.getString("camera_lens", DEFAULT_CAMERA_LENS) == "back") {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        }

        @JvmStatic
        fun getLivenessLevel(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return if (sharedPreferences.getString("liveness_level", DEFAULT_LIVENESS_LEVEL) == "0") 0 else 1
        }

        @JvmStatic
        fun livenessPassed(context: Context, score: Float, label: String?): Boolean {
            val lower = label.orEmpty().lowercase()
            if (lower.contains("spoof") || lower.contains("fake")) return false
            return score >= getLivenessThreshold(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val cameraLensPref = findPreference<ListPreference>("camera_lens")
            val livenessThresholdPref = findPreference<EditTextPreference>("liveness_threshold")
            val buttonRestorePref = findPreference<Preference>("restore_default_settings")

            livenessThresholdPref?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    try {
                        val value = (newValue as String).toFloat()
                        if (value < 0f || value > 1f) {
                            Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                            false
                        } else {
                            true
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                        false
                    }
                }

            buttonRestorePref?.setOnPreferenceClickListener {
                cameraLensPref?.value = DEFAULT_CAMERA_LENS
                livenessThresholdPref?.text = DEFAULT_LIVENESS_THRESHOLD
                Toast.makeText(activity, getString(R.string.restored_default_settings), Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}
