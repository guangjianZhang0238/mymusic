package com.music.app.data.remote

import android.content.Context
import android.content.SharedPreferences

/**
 * Token 本地持久化存储
 * 使用 SharedPreferences 保存登录 token、登录时间戳和用户基本信息
 * 支持 15 天免登录：每次登录/刷新 token 会更新登录时间戳，15 天内无需重新登录
 */
object TokenStore {
    private const val PREFS_NAME = "music_app_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"

    /** 15 天免登录有效期（毫秒） */
    private const val LOGIN_VALID_MS = 15L * 24 * 60 * 60 * 1000

    // 内存中的 token，避免频繁读取 SharedPreferences
    @Volatile
    private var cachedToken: String? = null

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 保存 token，并刷新「免登录」期限（本次起 15 天内有效）
     */
    fun saveToken(context: Context, token: String) {
        cachedToken = token
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            .apply()
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
            .remove(KEY_LOGIN_TIMESTAMP)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_NICKNAME)
            .apply()
    }

    /** 获取上次登录/刷新 token 的时间戳（毫秒），无则返回 null */
    fun getLoginTimestamp(context: Context): Long? {
        val ts = getPrefs(context).getLong(KEY_LOGIN_TIMESTAMP, -1L)
        return if (ts <= 0) null else ts
    }

    /** 是否在 15 天免登录有效期内 */
    fun isWithin15Days(context: Context): Boolean {
        val ts = getLoginTimestamp(context) ?: return false
        return (System.currentTimeMillis() - ts) < LOGIN_VALID_MS
    }

    /** 有 token 且在 15 天内，视为已登录可免登 */
    fun isLoggedIn(context: Context): Boolean = getToken(context) != null

    /** 已登录且在 15 天免登期内（用于启动时决定是否直接进首页） */
    fun isLoggedInWithin15Days(context: Context): Boolean =
        getToken(context) != null && isWithin15Days(context)
}
