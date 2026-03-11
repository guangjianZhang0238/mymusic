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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.ArtistDetailDto
import com.music.app.data.remote.NetworkModule
import com.music.app.ui.MusicViewModel

@Composable
fun ArtistDetailScreen(
    artistId: Long,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistId) {
        viewModel.loadArtistDetail(artistId)
        viewModel.loadArtistSongs(artistId)
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
            Text("歌手详情", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (uiState.artistDetailLoading) {
            CircularProgressIndicator(
                color = Color(0xFFE53935),
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 歌手信息区域
                item {
                    ArtistInfoSection(
                        artist = uiState.currentArtistDetail,
                        onPlayAll = {
                            if (uiState.artistSongs.isNotEmpty()) {
                                viewModel.playSongWithQueue(uiState.artistSongs, 0)
                            }
                        }
                    )
                }

                // 歌曲列表标题
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "热门歌曲",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "播放量最高前 ${uiState.artistSongs.size} 首",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }
                }

                // 歌曲列表
                if (uiState.artistSongsLoading) {
                    item {
                        CircularProgressIndicator(
                            color = Color(0xFFE53935),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    itemsIndexed(uiState.artistSongs.take(20)) { index, song ->
                        SongListItem(
                            index = index + 1,
                            title = song.title,
                            artist = song.artistNames ?: song.artistName ?: "未知歌手",
                            playCount = song.playCount,
                            onClick = { 
                                // 单曲点击：添加到播放列表（不替换）
                                viewModel.playSong(song)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistInfoSection(
    artist: ArtistDetailDto?,
    onPlayAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像
        AsyncImage(
            model = artist?.avatar?.let { NetworkModule.staticBaseUrl + it },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF333333))
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = artist?.name ?: "未知歌手",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        if (!artist?.nameEn.isNullOrBlank()) {
            Text(
                text = artist!!.nameEn,
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 统计信息
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatItem(label = "歌曲", value = artist?.songCount ?: 0)
            StatItem(label = "专辑", value = artist?.albumCount ?: 0)
        }

        // 地区
        if (!artist?.region.isNullOrBlank()) {
            Text(
                text = artist!!.region,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 播放全部按钮
        IconButton(
            onClick = onPlayAll,
            modifier = Modifier
                .padding(top = 16.dp)
                .background(Color(0xFFE53935), shape = CircleShape)
                .size(48.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "播放全部",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // 歌手简介
        if (!artist?.description.isNullOrBlank()) {
            Text(
                text = "歌手简介",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = artist!!.description,
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = Color(0xFF888888),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SongListItem(
    index: Int,
    title: String,
    artist: String,
    playCount: Int? = null,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artist,
                        color = Color(0xFFBDBDBD),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (playCount != null && playCount > 0) {
                        Text(
                            text = " · ${formatPlayCount(playCount)}次播放",
                            color = Color(0xFF666666),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatPlayCount(count: Int): String {
    return when {
        count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
        count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
        else -> count.toString()
    }
}
