package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.music.app.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    onEditPlaylistClick: (Long) -> Unit,
    onAddSongsClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歌单详情", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    // 更多操作菜单
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (uiState.currentPlaylist?.userId == uiState.currentUser?.id) {
                            // 只有创建者可以编辑和删除
                            DropdownMenuItem(
                                text = { Text("编辑歌单") },
                                onClick = {
                                    showMenu = false
                                    onEditPlaylistClick(playlistId)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除歌单") },
                                onClick = {
                                    showMenu = false
                                    viewModel.deletePlaylist(playlistId)
                                    onBackClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("添加歌曲") },
                            onClick = {
                                showMenu = false
                                onAddSongsClick(playlistId)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                            }
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B1B)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF111111))
        ) {
            val playlist = uiState.currentPlaylist
            if (playlist == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.playlistSongsLoading) {
                        Text(text = "加载中...", color = Color.Gray)
                    } else {
                        Text(text = "歌单不存在", color = Color.Gray)
                    }
                }
            } else {
                // 歌单头部信息
                PlaylistHeader(playlist = playlist, viewModel = viewModel, playlistId = playlistId)

                // 歌曲列表
                SongListSection(
                    songs = uiState.currentPlaylistSongs,
                    isLoading = uiState.playlistSongsLoading,
                    onSongClick = onSongClick,
                    onRemoveSong = { songId ->
                        viewModel.removeSongFromPlaylist(playlistId, songId)
                    },
                    isOwner = playlist.userId == uiState.currentUser?.id
                )
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: com.music.app.data.remote.PlaylistDto,
    viewModel: MusicViewModel,
    playlistId: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 封面
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF333333))
                ) {
                    if (!playlist.coverImage.isNullOrBlank()) {
                        AsyncImage(
                            model = playlist.coverImage,
                            contentDescription = playlist.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(50.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 2
                    )
                    if (!playlist.description.isNullOrBlank()) {
                        Text(
                            text = playlist.description,
                            color = Color(0xFFBBBBBB),
                            fontSize = 14.sp,
                            maxLines = 3,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${playlist.songCount ?: 0} 首歌曲",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${playlist.playCount ?: 0} 次播放",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            // 播放按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        // 播放整个歌单
                        if (playlist.songCount ?: 0 > 0) {
                            viewModel.incrementPlayCount(playlistId)
                            // 如果用户已登录，使用播放列表模式
                            if (viewModel.uiState.value.isLoggedIn) {
                                viewModel.playSongWithQueue(viewModel.uiState.value.currentPlaylistSongs.take(50), 0)
                            } else {
                                // 未登录用户播放第一首
                                if (viewModel.uiState.value.currentPlaylistSongs.isNotEmpty()) {
                                    viewModel.playSong(viewModel.uiState.value.currentPlaylistSongs[0])
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SongListSection(
    songs: List<com.music.app.data.remote.SongDto>,
    isLoading: Boolean,
    onSongClick: (Long) -> Unit,
    onRemoveSong: (Long) -> Unit,
    isOwner: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "歌曲列表",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "加载中...", color = Color.Gray)
            }
        } else if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "暂无歌曲",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(songs) { song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song.id) },
                        onRemove = if (isOwner) { { onRemoveSong(song.id) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SongItem(
    song: com.music.app.data.remote.SongDto,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 专辑封面
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF333333))
            ) {
                if (!song.albumCover.isNullOrBlank()) {
                    AsyncImage(
                        model = song.albumCover,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = song.artistName ?: "未知艺术家",
                    color = Color(0xFFBBBBBB),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            // 移除按钮（仅对所有者显示）
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "移除",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}