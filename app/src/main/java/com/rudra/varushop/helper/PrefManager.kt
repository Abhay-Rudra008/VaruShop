package com.rudra.varushop.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit


class PrefManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_PROFILE_PIC = "profile_pic"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_LANG_CODE = "lang_code"
        private const val KEY_LANG_SELECTED = "lang_selected"
        private const val KEY_USER_ID = "user_id"
    }


    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, "USER")
        set(value) = prefs.edit { putString(KEY_USER_ROLE, value) }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var profilePic: String?
        get() = prefs.getString(KEY_PROFILE_PIC, null)
        set(value) = prefs.edit { putString(KEY_PROFILE_PIC, value) }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit { putInt(KEY_USER_ID, value) }


    fun logout() {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_PROFILE_PIC)
            remove(KEY_USER_ROLE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }


    var nightMode: Int
        get() = prefs.getInt(KEY_NIGHT_MODE, 0)
        set(value) {
            prefs.edit { putInt(KEY_NIGHT_MODE, value) }
            applyNightMode(value)
        }

    private fun applyNightMode(mode: Int) {
        val appMode = when (mode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(appMode)
    }


    var isLangSelected: Boolean
        get() = prefs.getBoolean(KEY_LANG_SELECTED, false)
        set(value) = prefs.edit { putBoolean(KEY_LANG_SELECTED, value) }

    var langCode: String
        get() = prefs.getString(KEY_LANG_CODE, "en") ?: "en"
        set(value) {
            prefs.edit {
                putString(KEY_LANG_CODE, value)
                putBoolean(KEY_LANG_SELECTED, true)
            }
        }


    fun saveSearchQuery(query: String) {
        val searches = getRecentSearches().toMutableList()
        if (searches.contains(query)) searches.remove(query)
        searches.add(0, query) // Add to top
        if (searches.size > 10) searches.removeAt(10) // Keep last 10

        prefs.edit { putStringSet("recent_searches", searches.toSet()) }
    }

    fun getRecentSearches(): List<String> {
        return prefs.getStringSet("recent_searches", emptySet())?.toList() ?: emptyList()
    }

    fun clearRecentSearches() {
        prefs.edit { remove("recent_searches") }
    }
}