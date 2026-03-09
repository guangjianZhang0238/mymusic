package com.music.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.app.ui.MusicApp
import com.music.app.ui.MusicViewModel
import com.music.app.ui.theme.MusicAppTheme
import com.music.app.data.remote.NetworkModule
import com.music.app.data.remote.LoginStateManager
import com.music.app.utils.MemoryMonitor
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var loginStateManager: LoginStateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化网络模块，提供 Context 供 Token 拦截器使用
        NetworkModule.init(this)
        
        // 初始化登录状态管理器
        loginStateManager = LoginStateManager.getInstance(this)
        loginStateManager.startAutoRefresh()
        
        // 启动时记录内存信息
        Log.d("MainActivity", "应用启动")
        MemoryMonitor.logMemoryInfo(this)
        
        // 立即检查登录状态
        checkInitialLoginStatus()
        
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val userSettings by viewModel.userSettings.collectAsState()
            
            MusicAppTheme(
                primaryColor = userSettings.themeColor
            ) {
                MusicApp()
            }
        }
    }
    
    private fun checkInitialLoginStatus() {
        Thread {
            try {
                val isLoggedIn = com.music.app.data.remote.TokenStore.isLoggedIn(this)
                val username = com.music.app.data.remote.TokenStore.getUsername(this)
                Log.d("MainActivity", "初始登录状态检查 - 已登录: $isLoggedIn, 用户名: $username")
            } catch (e: Exception) {
                Log.e("MainActivity", "初始登录状态检查失败", e)
            }
        }.start()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次进入界面时刷新用户信息
        refreshUserInfo()
        
        // 恢复时检查内存状态
        if (MemoryMonitor.isNearMemoryLimit(this)) {
            Log.w("MainActivity", "内存使用接近上限")
            MemoryMonitor.logMemoryInfo(this)
        }
    }
    
    private fun refreshUserInfo() {
        // 在后台线程中刷新用户信息
        Thread {
            try {
                // 获取全局的 ViewModel 实例来检查登录状态
                // 由于无法直接获取 ViewModel，我们通过 TokenStore 来快速检查
                val isLoggedIn = com.music.app.data.remote.TokenStore.isLoggedIn(this)
                if (isLoggedIn) {
                    Log.d("MainActivity", "检测到已登录状态，保持登录")
                } else {
                    Log.d("MainActivity", "未检测到登录状态")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "刷新用户信息失败", e)
            }
        }.start()
    }
}
