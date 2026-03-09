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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.AlbumDetailDto
import com.music.app.data.remote.NetworkModule
import com.music.app.ui.MusicViewModel

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) {
        viewModel.loadAlbumDetail(albumId)
        viewModel.loadAlbumSongs(albumId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
    ) {
        // 顶部返回按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text("专辑详情", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (uiState.albumDetailLoading) {
            CircularProgressIndicator(
                color = Color(0xFFE53935),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 专辑信息区域
                item {
                    AlbumInfoSection(
                        album = uiState.currentAlbumDetail,
                        onPlayAll = {
                            if (uiState.albumSongs.isNotEmpty()) {
                                viewModel.playSongWithQueue(uiState.albumSongs, 0)
                            }
                        }
                    )
                }

                // 歌曲列表
                if (uiState.albumSongsLoading) {
                    item {
                        CircularProgressIndicator(
                            color = Color(0xFFE53935),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    itemsIndexed(uiState.albumSongs) { index, song ->
                        SongListItem(
                            index = index + 1,
                            title = song.title,
                            artist = song.artistName ?: "未知歌手",
                            onClick = { 
                                // 单曲点击：添加到播放列表（不替换）
                                if (uiState.isLoggedIn) {
                                    viewModel.playSong(song)
                                } else {
                                    viewModel.playSong(song)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumInfoSection(
    album: AlbumDetailDto?,
    onPlayAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album?.coverImage?.let { NetworkModule.staticBaseUrl + it },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0xFF333333))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album?.name ?: "未知专辑",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album?.artistName ?: "未知歌手",
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (!album?.releaseDate.isNullOrBlank()) {
                    Text(
                        text = album.releaseDate,
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "${album?.songCount ?: 0} 首歌曲",
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 播放全部按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onPlayAll,
                modifier = Modifier
                    .background(Color(0xFFE53935), shape = androidx.compose.foundation.shape.CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "播放全部",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 专辑简介
        if (!album?.description.isNullOrBlank()) {
            Text(
                text = "专辑简介",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = album.description,
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SongListItem(
    index: Int,
    title: String,
    artist: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index",
                color = Color(0xFF666666),
                fontSize = 14.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
