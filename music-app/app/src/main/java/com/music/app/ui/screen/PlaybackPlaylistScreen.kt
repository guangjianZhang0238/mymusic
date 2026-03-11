package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.data.remote.SongDto
import com.music.app.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackPlaylistScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSongs by remember { mutableStateOf(setOf<Long>()) }
    val isInSelectionMode by remember { derivedStateOf { selectedSongs.isNotEmpty() } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "播放列表 (${uiState.playbackPlaylist.size})", 
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "返回", 
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (uiState.playbackPlaylist.isNotEmpty()) {
                        if (isInSelectionMode) {
                            // 选择模式下的操作按钮
                            TextButton(
                                onClick = {
                                    // 批量删除选中的歌曲
                                    selectedSongs.forEach { songId ->
                                        viewModel.removeFromPlaybackPlaylist(songId)
                                    }
                                    selectedSongs = emptySet()
                                },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFE53935)
                                )
                            ) {
                                Text("删除(${selectedSongs.size})")
                            }
                            TextButton(
                                onClick = { selectedSongs = emptySet() },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Text("取消")
                            }
                        } else {
                            // 普通模式下的按钮
                            TextButton(
                                onClick = { 
                                    selectedSongs = uiState.playbackPlaylist.map { it.id }.toSet()
                                },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Text("选择")
                            }
                            TextButton(
                                onClick = { viewModel.clearPlaybackPlaylist() },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Text("清空")
                            }
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B1B)
                )
            )
        },
        containerColor = Color(0xFF111111)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF111111))
                .padding(16.dp)
        ) {
            if (uiState.playbackPlaylist.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "播放列表为空",
                            color = Color(0xFF888888),
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "播放歌曲会自动添加到播放列表",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // 播放列表内容
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.playbackPlaylist) { index, song ->
                        val isSelected = song.id in selectedSongs
                        PlaybackPlaylistItem(
                            song = song,
                            isCurrent = index == uiState.playbackPlaylistIndex,
                            isPlaying = uiState.isPlaying && index == uiState.playbackPlaylistIndex,
                            isSelected = isSelected,
                            onSelect = {
                                selectedSongs = if (isSelected) {
                                    selectedSongs - song.id
                                } else {
                                    selectedSongs + song.id
                                }
                            },
                            onPlay = { 
                                if (isInSelectionMode) {
                                    // 在选择模式下，点击切换选择状态
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                } else {
                                    // 播放指定歌曲
                                    viewModel.playSongFromPlaylist(index)
                                }
                            },
                            onRemove = { 
                                // 从播放列表移除
                                viewModel.removeFromPlaybackPlaylist(song.id)
                                
                                // 如果移除的是当前播放的歌曲，需要处理播放状态
                                if (index == uiState.playbackPlaylistIndex) {
                                    if (uiState.playbackPlaylist.size > 1) {
                                        // 播放下一首
                                        viewModel.nextSongInPlaylist()
                                    } else {
                                        // 列表为空，停止播放
                                        viewModel.clearPlaybackPlaylist()
                                    }
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
private fun PlaybackPlaylistItem(
    song: SongDto,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isSelected) {
                    onSelect() // 取消选择
                } else {
                    onPlay() // 播放歌曲
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFF3D3D3D)
                isCurrent -> Color(0xFF2D2D2D)
                else -> Color(0xFF1C1C1E)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择框
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // 播放状态指示器
                    if (isCurrent) {
                        if (isPlaying) {
                            // 正在播放的动画指示器（简化版）
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            // 当前歌曲但未播放
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isCurrent) Color(0xFFE53935) else Color.White,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artistNames ?: song.artistName ?: "未知歌手",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 移除按钮
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "移除",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}