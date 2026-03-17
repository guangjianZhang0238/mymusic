package com.music.app.data.remote

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserSettingsStore {
    private const val PREFS_NAME = "music_app_prefs"
    private const val KEY_PREFIX_SETTINGS_JSON = "user_settings_json_v1_"
    private const val KEY_PREFIX_SETTINGS_TS = "user_settings_ts_v1_"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun keyJson(userId: Long) = KEY_PREFIX_SETTINGS_JSON + userId
    private fun keyTs(userId: Long) = KEY_PREFIX_SETTINGS_TS + userId

    fun loadRawSettings(context: Context, userId: Long): List<UserSettingDto>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(keyJson(userId), null) ?: return null
        return runCatching { json.decodeFromString<List<UserSettingDto>>(raw) }.getOrNull()
    }

    fun saveRawSettings(context: Context, userId: Long, settings: List<UserSettingDto>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = runCatching { json.encodeToString(settings) }.getOrNull() ?: return
        prefs.edit()
            .putString(keyJson(userId), raw)
            .putLong(keyTs(userId), System.currentTimeMillis())
            .apply()
    }

    fun clearRawSettings(context: Context, userId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(keyJson(userId))
            .remove(keyTs(userId))
            .apply()
    }
}
