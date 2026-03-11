package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import android.util.Log
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastRefreshTime by remember { mutableStateOf(0L) }
    val refreshCooldown = 2000L // 2秒冷却时间
    
    // 添加调试日志
    Log.d("HomeScreen", "热门歌曲数量: ${uiState.hotSongs.size}")
    Log.d("HomeScreen", "加载状态: ${uiState.loading}")
    
    // 首次加载时获取热门歌曲
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (uiState.hotSongs.isEmpty() && !uiState.loading) {
            viewModel.loadHotSongs()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp)
    ) {
        Text(
            text = "推荐音乐",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "网易云风格基础页（已接热门歌曲）",
                color = Color(0xFFAAAAAA),
                modifier = Modifier.weight(1f)
            )
            
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
                modifier = Modifier
                    .height(32.dp)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "换一批",
                    fontSize = 12.sp
                )
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.hotSongs) { song ->
                    SongItem(song = song, onClick = { 
                        // 单曲点击：添加到播放列表（不替换）
                        if (uiState.isLoggedIn) {
                            viewModel.playSong(song)
                        } else {
                            viewModel.playSong(song)
                        }
                    })
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
