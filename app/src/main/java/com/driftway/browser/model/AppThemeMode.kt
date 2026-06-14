package com.driftway.browser.model

import androidx.appcompat.app.AppCompatDelegate

enum class AppThemeMode(
    val storageKey: String,
    val nightMode: Int
) {
    AUTO(
        storageKey = "auto",
        nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ),
    LIGHT(
        storageKey = "light",
        nightMode = AppCompatDelegate.MODE_NIGHT_NO
    ),
    DARK(
        storageKey = "dark",
        nightMode = AppCompatDelegate.MODE_NIGHT_YES
    );

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            // Driftway is an AMOLED-dark brand (the home is always dark) — default the whole app
            // to DARK so the menu/settings/dialogs stay cohesive instead of following a light system.
            return entries.firstOrNull { it.storageKey == key } ?: DARK
        }
    }
}
