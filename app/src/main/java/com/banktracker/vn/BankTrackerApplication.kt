package com.banktracker.vn

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.banktracker.vn.utils.PreferencesManager

class BankTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences
        val preferencesManager = PreferencesManager(this)

        // Set dark mode based on preferences
        if (preferencesManager.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Show onboarding if first launch
        if (preferencesManager.isFirstLaunch()) {
            // You can add onboarding logic here
            preferencesManager.setFirstLaunchComplete()
        }
    }
}