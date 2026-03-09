package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.data.remote.TokenStore
import com.music.app.ui.MusicViewModel

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onNavigateToFeedback: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cacheSize by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearMusicCacheDialog by remember { mutableStateOf(false) }
    var showClearAccountCacheDialog by remember { mutableStateOf(false) }
    var currentCacheSize by remember { mutableStateOf("0 MB") }
    
    // 背景设置相关状态
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showThemeColorPicker by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    
    // 计算缓存大小
    currentCacheSize = viewModel.getCacheSize()
    
    // 音乐缓存路径
    val musicCachePath = context.cacheDir.absolutePath + "/music_cache"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "设置",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 背景设置
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "背景设置",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "自定义应用背景主题",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = { showBackgroundPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("选择", color = Color.White)
                    }
                }
                
                // 当前背景预览
                Spacer(Modifier.height(12.dp))
                Text(
                    "当前背景预览：",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(viewModel.userSettings.value.backgroundColor ?: Color(0xFF111111))
                        .padding(8.dp)
                ) {
                    Text(
                        "预览区域",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // 主题颜色设置
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "主题颜色",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "选择应用的主要主题颜色，将应用于所有界面元素",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = { showThemeColorPicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = viewModel.userSettings.value.themeColor
                        )
                    ) {
                        Text("选择", color = Color.White)
                    }
                }
                
                // 当前主题颜色预览
                Spacer(Modifier.height(12.dp))
                Text(
                    "当前主题预览：",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(viewModel.userSettings.value.backgroundColor)
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            "主色调按钮",
                            color = viewModel.userSettings.value.themeColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = viewModel.userSettings.value.themeColor
                            )
                        ) {
                            Text("操作按钮", color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "这段文字使用主题色作为强调色",
                            color = viewModel.userSettings.value.themeColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // 均衡器设置
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "音效均衡器",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "十段均衡器，实时作用于当前播放音乐",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = { 
                            if (viewModel.checkLoginRequired(showWarning = true)) {
                                showEqualizerDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("打开", color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // 音乐缓存区块
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "音乐缓存",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 缓存说明
                Text(
                    "播放过的歌曲会自动缓存到本地，下次播放时优先使用缓存内容，节省流量并提升播放速度。",
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                Spacer(Modifier.height(12.dp))
                
                // 缓存路径
                Text(
                    "缓存路径",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    musicCachePath,
                    color = Color(0xFFBDBDBD),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(12.dp))
                
                // 缓存大小 + 清除按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "已缓存大小",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                        Text(
                            currentCacheSize,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        
                        // 添加缓存统计信息
                        val cachedSongsCount = viewModel.getCachedSongsCount()
                        if (cachedSongsCount > 0) {
                            Text(
                                "$cachedSongsCount 首歌曲已缓存",
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { showClearMusicCacheDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text("清除歌曲缓存", color = Color.White)
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // 缓存使用提示
                Text(
                    "提示：缓存会在存储空间不足时自动清理较旧的内容。",
                    color = Color(0xFF888888),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // 账号信息缓存区块
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "账号信息缓存",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // 展示当前登录状态
                val storedUsername = TokenStore.getUsername(context)
                val storedToken = TokenStore.getToken(context)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (storedUsername != null) {
                            Text(
                                "已登录用户",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                            Text(
                                storedUsername,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            "Token 状态",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                        Text(
                            if (storedToken != null) "已缓存（登录状态有效）" else "未登录",
                            color = if (storedToken != null) Color(0xFF4CAF50) else Color(0xFF888888),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (storedToken != null) {
                        Button(
                            onClick = { showClearAccountCacheDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF424242)
                            )
                        ) {
                            Text("清除登录", color = Color.White)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // 反馈记录
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToFeedback?.invoke() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Feedback,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "我的反馈情况",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "查看历史反馈记录和处理状态",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.rotate(180f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 关于信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            "关于",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "音乐播放器 v1.0",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
    
    // 背景选择对话框
    if (showBackgroundPicker) {
        BackgroundPickerDialog(
            currentBackgroundColor = viewModel.userSettings.value.backgroundColor ?: Color(0xFF111111),
            onBackgroundSelected = { color ->
                viewModel.updateUserSettings(viewModel.userSettings.value.copy(backgroundColor = color))
                showBackgroundPicker = false
            },
            onDismiss = { showBackgroundPicker = false }
        )
    }
    
    if (showEqualizerDialog) {
        EqualizerDialog(
            viewModel = viewModel,
            onDismiss = { showEqualizerDialog = false }
        )
    }

    // 清除歌曲缓存确认对话框
    if (showClearMusicCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearMusicCacheDialog = false },
            title = { Text("确认清除歌曲缓存") },
            text = { Text("清除缓存后，已缓存的音乐将需要重新下载。是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        currentCacheSize = viewModel.getCacheSize()
                        showClearMusicCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("确认清除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMusicCacheDialog = false }) {
                    Text("取消", color = Color(0xFFBDBDBD))
                }
            }
        )
    }
    
    // 清除账号缓存（登出）确认对话框
    if (showClearAccountCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearAccountCacheDialog = false },
            title = { Text("确认清除登录") },
            text = { Text("清除登录状态后，需要重新登录才能使用卫藏、歌单等个性化功能。是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showClearAccountCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF424242)
                    )
                ) {
                    Text("确认登出", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAccountCacheDialog = false }) {
                    Text("取消", color = Color(0xFFBDBDBD))
                }
            }
        )
    }
    
    // 主题颜色选择器对话框
    if (showThemeColorPicker) {
        ThemeColorPickerDialog(
            currentThemeColor = viewModel.userSettings.value.themeColor,
            onThemeColorSelected = { color ->
                viewModel.setThemeColor(color)
                showThemeColorPicker = false
            },
            onDismiss = { showThemeColorPicker = false }
        )
    }
}

@Composable
private fun ThemeColorPickerDialog(
    currentThemeColor: Color,
    onThemeColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val themeOptions = listOf(
        "经典红" to Color(0xFFE53935),
        "活力橙" to Color(0xFFFF6D00),
        "阳光黄" to Color(0xFFFFD600),
        "清新绿" to Color(0xFF00C853),
        "海洋蓝" to Color(0xFF2979FF),
        "优雅紫" to Color(0xFFAA00FF),
        "浪漫粉" to Color(0xFFFF4081),
        "薄荷绿" to Color(0xFF1DE9B6)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "选择主题颜色",
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                themeOptions.chunked(4).forEach { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowColors.forEach { (name, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        onThemeColorSelected(color)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            color = color,
                                            shape = RoundedCornerShape(25.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    if (color == currentThemeColor) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // 如果这行不足4个，填充空白
                        repeat(4 - rowColors.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFFBDBDBD))
            }
        }
    )
}

private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

@Composable
private fun BackgroundPickerDialog(
    currentBackgroundColor: Color,
    onBackgroundSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundOptions = listOf(
        Color(0xFF111111) to "经典黑",
        Color(0xFF1A1A2E) to "深海蓝",
        Color(0xFF16213E) to "午夜蓝",
        Color(0xFF0F3460) to "宝石蓝",
        Color(0xFF533483) to "紫罗兰",
        Color(0xFF1D2D50) to "海军蓝",
        Color(0xFF2D4059) to "钢铁灰",
        Color(0xFF444444) to "石墨灰",
        Color(0xFF8B0000) to "深红色",
        Color(0xFF228B22) to "森林绿",
        Color(0xFF4B0082) to "靛蓝色",
        Color(0xFF8B4513) to "马鞍棕",
        Color(0xFF2F4F4F) to "深青灰",
        Color(0xFF556B2F) to "橄榄绿",
        Color(0xFF800000) to "栗色",
        Color(0xFF006400) to "深绿色"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "选择背景颜色",
                color = Color.White
            )
        },
        text = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(backgroundOptions) { (color, name) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            onBackgroundSelected(color)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = color,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp)
                        ) {
                            if (color == currentBackgroundColor) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFFBDBDBD))
            }
        }
    )
}
