package com.music.app.data.remote

import android.content.Context
import com.music.app.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * 登录状态管理器
 * 负责自动刷新用户登录状态和续期
 */
class LoginStateManager(private val context: Context) {
    private val repository = MusicRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        // 15天有效期（毫秒）
        const val TOKEN_EXPIRATION_TIME = 15L * 24 * 60 * 60 * 1000
        
        // 刷新间隔（1小时）
        const val REFRESH_INTERVAL = 60L * 60 * 1000
        
        // 实例
        @Volatile
        private var INSTANCE: LoginStateManager? = null
        
        fun getInstance(context: Context): LoginStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoginStateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 启动自动刷新任务
     */
    fun startAutoRefresh() {
        coroutineScope.launch {
            while (true) {
                try {
                    refreshLoginStatusIfNeeded()
                    delay(REFRESH_INTERVAL)
                } catch (e: Exception) {
                    // 忽略异常，继续下次循环
                    delay(REFRESH_INTERVAL)
                }
            }
        }
    }
    
    /**
     * 刷新登录状态（如果需要）
     * 超过 15 天免登期则清除本地 token，下次启动需重新登录
     */
    private suspend fun refreshLoginStatusIfNeeded() {
        val token = TokenStore.getToken(context)
        if (token != null) {
            if (!TokenStore.isWithin15Days(context)) {
                TokenStore.clearToken(context)
                return
            }
            // 检查token是否即将过期（剩余1天时刷新）
            val userId = TokenStore.getUserId(context)
            if (userId != null) {
                val response = repository.getCurrentUser()
                if (response != null && response.token != null) {
                    // 更新token和用户信息
                    TokenStore.saveToken(context, response.token)
                    response.userInfo?.let { user ->
                        TokenStore.saveUserInfo(context, user.id, user.username, user.nickname)
                    }
                } else {
                    // token已失效，清除本地存储
                    TokenStore.clearToken(context)
                }
            }
        }
    }
    
    /**
     * 手动刷新用户信息（每次进入界面时调用）
     */
    suspend fun refreshUserInfo() {
        val token = TokenStore.getToken(context)
        if (token != null) {
            try {
                val response = repository.getCurrentUser()
                if (response != null && response.userInfo != null) {
                    // 更新用户信息
                    response.userInfo.let { user ->
                        TokenStore.saveUserInfo(context, user.id, user.username, user.nickname)
                    }
                    // 如果服务器返回了新token，也更新token
                    if (response.token != null) {
                        TokenStore.saveToken(context, response.token)
                    }
                } else {
                    // 用户信息获取失败，清除登录状态
                    TokenStore.clearToken(context)
                }
            } catch (e: Exception) {
                // 网络异常，保留现有状态
            }
        }
    }
    
    /**
     * 检查token是否有效
     */
    fun isTokenValid(): Boolean {
        val token = TokenStore.getToken(context)
        val userId = TokenStore.getUserId(context)
        return token != null && userId != null
    }
    
    /**
     * 获取剩余有效时间（毫秒）
     */
    fun getRemainingTime(): Long {
        // 简化的实现：假设token创建时间为当前时间减去TOKEN_EXPIRATION_TIME的一半
        // 实际应用中应该在token中包含创建时间戳
        return TOKEN_EXPIRATION_TIME / 2
    }
    
    /**
     * 检查是否需要续期（剩余时间少于1天）
     */
    fun needRenewal(): Boolean {
        return getRemainingTime() < 24 * 60 * 60 * 1000
    }
}