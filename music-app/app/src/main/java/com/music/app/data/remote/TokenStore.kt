package com.music.app.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Token 本地持久化存储
 * 使用 SharedPreferences 保存登录 token 和用户基本信息
 */
object TokenStore {
    private const val PREFS_NAME = "music_app_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"

    // 内存中的 token，避免频繁读取 SharedPreferences
    @Volatile
    private var cachedToken: String? = null

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String) {
        cachedToken = token
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        if (cachedToken == null) {
            cachedToken = getPrefs(context).getString(KEY_TOKEN, null)
        }
        return cachedToken
    }

    fun saveUserInfo(context: Context, userId: Long?, username: String?, nickname: String?) {
        getPrefs(context).edit()
            .putLong(KEY_USER_ID, userId ?: -1L)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun getUsername(context: Context): String? =
        getPrefs(context).getString(KEY_USERNAME, null)

    fun getNickname(context: Context): String? =
        getPrefs(context).getString(KEY_NICKNAME, null)

    fun getUserId(context: Context): Long? {
        val id = getPrefs(context).getLong(KEY_USER_ID, -1L)
        return if (id == -1L) null else id
    }

    fun clearToken(context: Context) {
        cachedToken = null
        getPrefs(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_NICKNAME)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean = getToken(context) != null
}
