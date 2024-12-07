package com.ovisionik.memotag.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object MemoTagThemeUtils {
    fun applyUserPrefTheme(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val themeValue = sharedPreferences.getString("theme_preference", "system") ?: "system"

        // Fetch current theme mode
        val currentMode = AppCompatDelegate.getDefaultNightMode()

        // Set desired mode based on preferences
        val desiredMode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        // Apply the theme if there's a difference
        if (currentMode != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode)
        }

//        // Recheck after 1 second
//        Handler(Looper.getMainLooper()).postDelayed({
//            val newMode = AppCompatDelegate.getDefaultNightMode()
//            println("Rechecked theme mode after 1 second: $newMode")
//            if (newMode != desiredMode) {
//                println("Theme mode was not updated correctly after 1 second.")
//            } else {
//                println("Theme mode is correctly set.")
//            }
//        }, 1000)
    }
}
