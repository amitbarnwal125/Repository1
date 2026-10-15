package com.swara.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("swara_settings", Context.MODE_PRIVATE)

    private val _jamendoClientId = MutableStateFlow(prefs.getString(KEY_JAMENDO, null))
    val jamendoClientId: StateFlow<String?> = _jamendoClientId.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.getBoolean(KEY_WIFI_ONLY, false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    var askedNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIF, false)
        set(v) = prefs.edit().putBoolean(KEY_ASKED_NOTIF, v).apply()

    fun setJamendoClientId(id: String?) {
        val clean = id?.trim()?.ifBlank { null }
        prefs.edit().putString(KEY_JAMENDO, clean).apply()
        _jamendoClientId.value = clean
    }

    fun setWifiOnly(v: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, v).apply()
        _wifiOnly.value = v
    }

    private companion object {
        const val KEY_JAMENDO = "jamendo_client_id"
        const val KEY_WIFI_ONLY = "wifi_only_downloads"
        const val KEY_ASKED_NOTIF = "asked_notifications"
    }
}
