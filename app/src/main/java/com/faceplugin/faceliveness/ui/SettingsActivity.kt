package com.faceplugin.faceliveness.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.faceplugin.faceliveness.R
import com.faceplugin.faceliveness.active.ActiveCheck
import com.faceplugin.faceliveness.data.AppSettings
import com.google.android.material.appbar.MaterialToolbar

/** The preference screen. Everything it stores is read back through [AppSettings]. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
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

            val cameraLensPref = findPreference<ListPreference>(AppSettings.KEY_CAMERA_LENS)
            val livenessThresholdPref =
                findPreference<EditTextPreference>(AppSettings.KEY_LIVENESS_THRESHOLD)
            val checksPref = findPreference<MultiSelectListPreference>(AppSettings.KEY_CHECKS)
            val countPref = findPreference<ListPreference>(AppSettings.KEY_COUNT)
            val randomizePref = findPreference<SwitchPreferenceCompat>(AppSettings.KEY_RANDOMIZE)

            livenessThresholdPref?.onPreferenceChangeListener = fractionListener()

            checksPref?.let { pref ->
                pref.summaryProvider = Preference.SummaryProvider<MultiSelectListPreference> { p ->
                    describeChecks(p.values)
                }
                pref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        @Suppress("UNCHECKED_CAST")
                        val picked = newValue as? Set<String> ?: emptySet()
                        if (picked.isEmpty()) {
                            toast(getString(R.string.active_checks_empty))
                            false
                        } else {
                            true
                        }
                    }
            }

            findPreference<EditTextPreference>(AppSettings.KEY_FRAMES_WAIT)
                ?.onPreferenceChangeListener = intListener(30, 600)
            findPreference<EditTextPreference>(AppSettings.KEY_YAW)
                ?.onPreferenceChangeListener = floatListener(5f, 60f)
            findPreference<EditTextPreference>(AppSettings.KEY_PITCH)
                ?.onPreferenceChangeListener = floatListener(5f, 45f)
            findPreference<EditTextPreference>(AppSettings.KEY_SMILE)
                ?.onPreferenceChangeListener = fractionListener()
            findPreference<EditTextPreference>(AppSettings.KEY_BLINK)
                ?.onPreferenceChangeListener = fractionListener()
            findPreference<EditTextPreference>(AppSettings.KEY_BLINKS_NUMBER)
                ?.onPreferenceChangeListener = intListener(1, 6)
            findPreference<EditTextPreference>(AppSettings.KEY_PERSPECTIVE)
                ?.onPreferenceChangeListener = fractionListener()
            findPreference<EditTextPreference>(AppSettings.KEY_ALIGN_ANGLE)
                ?.onPreferenceChangeListener = floatListener(3f, 45f)
            findPreference<EditTextPreference>(AppSettings.KEY_MIN_FACE)
                ?.onPreferenceChangeListener = floatListener(0.05f, 0.9f)
            findPreference<EditTextPreference>(AppSettings.KEY_QUALITY)
                ?.onPreferenceChangeListener = fractionListener()

            findPreference<Preference>("restore_default_settings")?.setOnPreferenceClickListener {
                cameraLensPref?.value = AppSettings.DEFAULT_CAMERA_LENS
                livenessThresholdPref?.text = AppSettings.DEFAULT_LIVENESS_THRESHOLD
                checksPref?.values = AppSettings.DEFAULT_CHECK_KEYS
                countPref?.value = AppSettings.DEFAULT_COUNT
                randomizePref?.isChecked = AppSettings.DEFAULT_RANDOMIZE
                setText(AppSettings.KEY_FRAMES_WAIT, AppSettings.DEFAULT_FRAMES_WAIT)
                setText(AppSettings.KEY_YAW, AppSettings.DEFAULT_YAW)
                setText(AppSettings.KEY_PITCH, AppSettings.DEFAULT_PITCH)
                setText(AppSettings.KEY_SMILE, AppSettings.DEFAULT_SMILE)
                setText(AppSettings.KEY_BLINK, AppSettings.DEFAULT_BLINK)
                setText(AppSettings.KEY_BLINKS_NUMBER, AppSettings.DEFAULT_BLINKS_NUMBER)
                setText(AppSettings.KEY_PERSPECTIVE, AppSettings.DEFAULT_PERSPECTIVE)
                setText(AppSettings.KEY_ALIGN_ANGLE, AppSettings.DEFAULT_ALIGN_ANGLE)
                setText(AppSettings.KEY_MIN_FACE, AppSettings.DEFAULT_MIN_FACE)
                setText(AppSettings.KEY_QUALITY, AppSettings.DEFAULT_QUALITY)
                setSwitch(AppSettings.KEY_INVERT_TURN, AppSettings.DEFAULT_INVERT)
                setSwitch(AppSettings.KEY_INVERT_TILT, AppSettings.DEFAULT_INVERT)
                toast(getString(R.string.restored_default_settings))
                true
            }
        }

        private fun setText(key: String, value: String) {
            findPreference<EditTextPreference>(key)?.text = value
        }

        private fun setSwitch(key: String, value: Boolean) {
            findPreference<SwitchPreferenceCompat>(key)?.isChecked = value
        }

        /** Challenge keys → the labels shown on the preference, in the canonical order. */
        private fun describeChecks(values: Set<String>?): String {
            val picked = ActiveCheck.entries.filter { values?.contains(it.key) == true }
            if (picked.isEmpty()) return getString(R.string.active_checks_empty)
            return picked.joinToString(" · ") { getString(it.titleRes) }
        }

        private fun fractionListener() = floatListener(0f, 1f)

        private fun floatListener(min: Float, max: Float) =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = (newValue as? String)?.trim()?.toFloatOrNull()
                if (value == null || value < min || value > max) {
                    toast(getString(R.string.invalid_value))
                    false
                } else {
                    true
                }
            }

        private fun intListener(min: Int, max: Int) =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = (newValue as? String)?.trim()?.toIntOrNull()
                if (value == null || value < min || value > max) {
                    toast(getString(R.string.invalid_value))
                    false
                } else {
                    true
                }
            }

        private fun toast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
