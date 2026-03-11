package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.ui.MusicViewModel

@Composable
fun AlbumSongsScreen(albumId: Long, viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) {
        viewModel.loadAlbumSongs(albumId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp)
    ) {
        Text(
            text = uiState.currentAlbumName.ifBlank { "专辑歌曲" },
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "点击歌曲将按专辑队列播放",
            color = Color(0xFFBDBDBD),
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        if (uiState.albumSongsLoading) {
            CircularProgressIndicator(color = Color(0xFFE53935))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.albumSongs) { index, song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                // 单曲点击：添加到播放列表（不替换）
                                viewModel.playSong(song)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(song.artistNames ?: song.artistName ?: "未知歌手", color = Color(0xFFBDBDBD))
                        }
                    }
                }
            }
        }
    }
}
