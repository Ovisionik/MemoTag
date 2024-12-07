package com.ovisionik.memotag

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class UserSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from the XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Handle a preference directly in this fragment
        val themePreference = findPreference<Preference>("theme_preference")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            // Apply the theme change dynamically or notify the user
            applyTheme(newValue.toString())
            true // Return true to save the new value
        }

        val databasePreference = findPreference<Preference>("use_external_database")
        databasePreference?.setOnPreferenceChangeListener { _, newValue ->
            // Handle the external database toggle
            handleDatabasePreferenceChange(newValue as Boolean)
            true // Return true to save the new value
        }
    }

    private fun applyTheme(themeValue: String) {
        when (themeValue) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun handleDatabasePreferenceChange(useExternal: Boolean) {
        if (useExternal) {
            // Logic to enable the external database
        } else {
            // Logic to switch to internal database
        }
    }
}