package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.NetworkModule
import com.music.app.data.remote.UserInfo
import com.music.app.ui.MusicViewModel

@Composable
fun MineScreen(
    viewModel: MusicViewModel,
    onArtistClick: (Long) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRecentPlayedClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn) {
        // 基础数据仅在需要时加载
        if (uiState.artists.isEmpty()) {
            viewModel.loadMineData()
        }

        if (uiState.isLoggedIn) {
            if (uiState.recentPlayedSongs.isEmpty()) viewModel.loadRecentPlayedSongs()
            if (uiState.favoriteSongs.isEmpty()) viewModel.loadFavoriteSongs()
            if (uiState.userPlaylists.isEmpty()) viewModel.loadUserPlaylists()
            if (uiState.userLyricsShares.isEmpty()) viewModel.loadUserLyricsShares()
            if (uiState.followedArtists.isEmpty()) viewModel.loadFollowedArtists()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 用户信息区域
        item {
            UserSection(
                isLoggedIn = uiState.isLoggedIn,
                userInfo = uiState.currentUser,
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick,
                onLogoutClick = { viewModel.logout() },
                onSettingsClick = onSettingsClick
            )
        }

        // 最近播放（仅登录用户可见）
        if (uiState.isLoggedIn) {
            item {
                SectionCard(
                    title = "最近播放",
                    icon = Icons.Default.History,
                    count = uiState.recentPlayedSongs.size,
                    subtitle = if (uiState.recentPlayedSongs.isNotEmpty()) "${uiState.recentPlayedSongs.size}首" else "暂无播放记录",
                    onClick = onRecentPlayedClick
                )
            }
        }

        // 收藏歌曲
        item {
            SectionHeader(
                title = "我的收藏",
                icon = Icons.Default.Favorite,
                count = uiState.favoriteSongs.size
            )
        }

        if (uiState.favoriteSongs.isEmpty()) {
            item {
                Text("暂无收藏歌曲", color = Color(0xFF666666), fontSize = 14.sp)
            }
        } else {
            items(uiState.favoriteSongs.take(5)) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
        }

        // 我的歌单
        item {
            SectionHeader(
                title = "我的歌单",
                icon = Icons.Default.PlaylistPlay,
                count = uiState.userPlaylists.size
            )
        }

        if (uiState.userPlaylists.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("暂无歌单，点击创建", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("创建你的第一个歌单", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(uiState.userPlaylists.take(3)) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(playlist.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${playlist.songCount ?: 0} 首歌曲", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                    }
                }
            }
        }

        // 我的歌词分享
        item {
            SectionHeader(
                title = "歌词分享",
                icon = Icons.Default.Share,
                count = uiState.userLyricsShares.size
            )
        }

        if (uiState.userLyricsShares.isEmpty()) {
            item {
                Text("暂无歌词分享", color = Color(0xFF666666), fontSize = 14.sp)
            }
        } else {
            items(uiState.userLyricsShares.take(10)) { share ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("歌词ID: ${share.lyricsId}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            "类型: ${share.shareType} · ${share.createTime?.substringBefore("T") ?: ""}",
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 关注歌手（仅登录用户可见）
        if (uiState.isLoggedIn) {
            item {
                SectionCard(
                    title = "关注歌手",
                    icon = Icons.Default.Person,
                    count = uiState.followedArtists.size,
                    subtitle = if (uiState.followedArtists.isNotEmpty()) "已关注${uiState.followedArtists.size}位歌手" else "暂无关注歌手",
                    onClick = { /* 可以跳转到关注歌手列表页面 */ }
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color(0xFFBDBDBD), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF666666))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text("($count)", color = Color(0xFFBDBDBD), fontSize = 14.sp)
    }
}

@Composable
private fun SongItem(
    song: com.music.app.data.remote.SongDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumCover?.let { NetworkModule.staticBaseUrl + it },
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artistNames ?: song.artistName ?: "未知歌手",
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UserSection(
    isLoggedIn: Boolean,
    userInfo: UserInfo?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn && userInfo != null) {
                // 已登录状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                userInfo.nickname ?: userInfo.username ?: "用户",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            userInfo.email?.let { email ->
                                Text(email, color = Color(0xFFBDBDBD), fontSize = 12.sp)
                            }
                        }
                    }
                    // 设置按钮
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = Color(0xFFBDBDBD)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = onLogoutClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("退出登录", color = Color.White)
                }
            } else {
                // 未登录状态
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("未登录", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("登录后同步收藏和播放记录", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = onLoginClick,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        )
                    ) {
                        Text("登录", color = Color.White)
                    }
                    androidx.compose.material3.Button(
                        onClick = onRegisterClick,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF424242)
                        )
                    ) {
                        Text("注册", color = Color.White)
                    }
                }
            }
        }
    }
}
