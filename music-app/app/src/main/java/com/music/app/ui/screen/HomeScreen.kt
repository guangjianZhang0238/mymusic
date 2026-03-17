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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.data.remote.SongDto
import com.music.app.ui.MusicViewModel
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigate: (route: String) -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastRefreshTime by remember { mutableStateOf(0L) }
    val refreshCooldown = 2000L // 2秒冷却时间
    val nickname = uiState.currentUser?.nickname ?: uiState.currentUser?.username
    val greeting = remember {
        val hour = runCatching { LocalTime.now().hour }.getOrNull() ?: 12
        when (hour) {
            in 5..11 -> "早上好"
            in 12..17 -> "下午好"
            else -> "晚上好"
        }
    }
    
    // 首次加载：填充主页数据
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (uiState.hotSongs.isEmpty() && !uiState.loading) viewModel.loadHotSongs()
        viewModel.loadPublicPlaylists()
    }
    androidx.compose.runtime.LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            viewModel.loadRecentPlayedSongs()
            viewModel.loadFavoriteSongs()
            viewModel.loadFollowedArtists()
            viewModel.loadUserPlaylists()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (!nickname.isNullOrBlank()) "$greeting，$nickname" else greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "想听点什么？",
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    enabled = false,
                    placeholder = { Text("搜索歌曲 / 歌手 / 专辑", color = Color(0xFF888888)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color(0xFF1C1C1E),
                        disabledBorderColor = Color(0xFF2A2A2A),
                        disabledTextColor = Color.White,
                        disabledPlaceholderColor = Color(0xFF888888)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("search") }
                )
            }
        }

        item {
            SectionTitle("快捷入口")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip("最近播放") { onNavigate("recentPlayed") }
                QuickActionChip("播放队列") { onNavigate("playbackPlaylist") }
                QuickActionChip("我的歌单") { onNavigate("playlists") }
                QuickActionChip("设置") { onNavigate("settings") }
                QuickActionChip("反馈") { onNavigate("myFeedback") }
            }
        }

        item {
            SectionTitle("继续播放")
            val current = uiState.currentSong
            if (current != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlayer() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(current.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = current.artistNames ?: current.artistName ?: "未知歌手",
                            color = Color(0xFFB0B0B0),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "队列中共有 ${uiState.playbackPlaylist.size} 首",
                            color = Color(0xFF7E7E7E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                HintCard(text = "还没有开始播放，去“热门歌曲”挑一首吧。")
            }
        }

        item {
            SectionTitle("最近播放")
            if (!uiState.isLoggedIn) {
                HintCard(text = "登录后可同步最近播放记录。")
            } else if (uiState.recentPlayedSongs.isEmpty()) {
                HintCard(text = "暂无最近播放。")
            } else {
                HorizontalSongRow(
                    songs = uiState.recentPlayedSongs.take(12),
                    onSongClick = { viewModel.playSong(it) }
                )
            }
        }

        item {
            SectionTitle("推荐歌单")
            if (uiState.publicPlaylists.isEmpty()) {
                HintCard(text = "暂无推荐歌单。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.publicPlaylists.take(6).forEach { pl ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("playlistDetail/${pl.id}") },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(pl.name ?: "未命名歌单", color = Color.White, fontWeight = FontWeight.SemiBold)
                                val desc = pl.description?.takeIf { it.isNotBlank() } ?: "点击查看详情"
                                Text(desc, color = Color(0xFFB0B0B0), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("热门歌曲", modifier = Modifier.padding(bottom = 0.dp))
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastRefreshTime > refreshCooldown) {
                            viewModel.refreshHotSongsRandom()
                            lastRefreshTime = now
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("换一批", fontSize = 12.sp)
                }
            }

            if (uiState.loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.hotSongs.take(20).forEach { song ->
                        SongItem(
                            song = song,
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItem(song: SongDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                text = song.artistNames ?: song.artistName ?: "未知歌手",
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Text(
            text = text,
            color = Color(0xFFB0B0B0),
            fontSize = 13.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun QuickActionChip(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HorizontalSongRow(
    songs: List<SongDto>,
    onSongClick: (SongDto) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        songs.forEach { song ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .clickable { onSongClick(song) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        text = song.artistNames ?: song.artistName ?: "未知歌手",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
