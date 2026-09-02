package com.pcare.shared

import android.content.Context

/**
 * Persists the JWT, the logged-in user and the server base URL using SharedPreferences.
 * The server URL is user-editable on the login screen so the app can reach the backend on a
 * developer PC (e.g. http://192.168.1.5:8080) or the emulator host (http://10.0.2.2:8080).
 */
class Session(context: Context) {

    private val prefs = context.getSharedPreferences("pcare_session", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, normalize(value)).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    /** UI language code. Only "en" is active today; "hi" (Hindi) is coming soon. */
    var language: String
        get() = prefs.getString(KEY_LANG, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    val isLoggedIn: Boolean get() = !token.isNullOrBlank()

    fun clear() {
        val url = baseUrl
        prefs.edit().clear().putString(KEY_BASE_URL, url).apply()
    }

    private fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http")) u = "http://$u"
        if (!u.endsWith("/")) u = "$u/"
        return u
    }

    companion object {
        // Default points at this PC's LAN IP so a physical phone on the same Wi-Fi connects with
        // no editing. Change on the login screen if the PC's IP differs (e.g. http://10.0.2.2:8080 for emulator).
        const val DEFAULT_BASE_URL = "http://192.168.0.108:8080/"
        private const val KEY_LANG = "language"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
    }
}
