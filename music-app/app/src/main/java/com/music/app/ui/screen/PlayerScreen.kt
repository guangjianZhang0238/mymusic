package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.NetworkModule
import com.music.app.player.PlayMode
import com.music.app.ui.MusicViewModel
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun PlayerScreen(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var commentText by remember { mutableStateOf("") }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // 歌词自动滚动
    LaunchedEffect(uiState.currentLyricIndex, uiState.lyrics.isEmpty()) {
        if (uiState.lyrics.isNotEmpty() && uiState.currentLyricIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = uiState.currentLyricIndex.coerceAtLeast(0),
                    scrollOffset = -200 // 让当前歌词显示在屏幕中上部
                )
            }
        }
    }

    // 切换到评论标签时加载评论
    LaunchedEffect(selectedTabIndex, uiState.currentSong?.id) {
        if (selectedTabIndex == 1) {
            uiState.currentSong?.let { song ->
                viewModel.loadSongComments(song.id)
            }
        }
    }

    val progress = uiState.progressMs.toFloat().coerceAtLeast(0f)
    val buffered = uiState.bufferedPositionMs.toFloat().coerceAtLeast(0f)
    val bufferedPercent = uiState.bufferedPercent.coerceIn(0, 100)
    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp)
    ) {
        AsyncImage(
            model = uiState.currentSong?.albumCover?.let { NetworkModule.staticBaseUrl + it },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Text(
            text = uiState.currentSong?.title ?: "暂无歌曲",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = uiState.currentSong?.artistNames ?: uiState.currentSong?.artistName ?: "",
            color = Color(0xFFBDBDBD),
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        BufferedProgressSlider(
            progress = progress,
            buffered = buffered,
            bufferedPercent = bufferedPercent,
            duration = duration,
            onSeekTo = { viewModel.seekTo(it.toLong()) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(uiState.progressMs), color = Color(0xFFB0B0B0))
            Text(formatMs(uiState.durationMs), color = Color(0xFFB0B0B0))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.togglePlayMode() }) {
                Icon(
                    when (uiState.playMode) {
                        PlayMode.SEQUENCE -> Icons.Default.Repeat
                        PlayMode.LOOP_ONE -> Icons.Default.RepeatOn
                        PlayMode.SHUFFLE -> Icons.Default.Shuffle
                    },
                    contentDescription = when (uiState.playMode) {
                        PlayMode.SEQUENCE -> "顺序播放"
                        PlayMode.LOOP_ONE -> "单曲循环"
                        PlayMode.SHUFFLE -> "随机播放"
                    },
                    tint = if (uiState.playMode == PlayMode.SEQUENCE) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { viewModel.previousSong() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { viewModel.nextSong() }) {
                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    if (uiState.isCurrentSongFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (uiState.isCurrentSongFavorited) "取消收藏" else "收藏",
                    tint = if (uiState.isCurrentSongFavorited) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            
            // 反馈按钮
            IconButton(
                onClick = { showFeedbackDialog = true },
                enabled = uiState.currentSong != null && uiState.isLoggedIn
            ) {
                Icon(
                    Icons.Default.Feedback,
                    contentDescription = "歌曲反馈",
                    tint = if (uiState.currentSong != null && uiState.isLoggedIn) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                )
            }
        }

        // 标签页
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("歌词", color = Color.White) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("评论", color = Color.White) }
            )
        }

        when (selectedTabIndex) {
            0 -> LyricsTab(uiState, viewModel, listState)
            1 -> CommentsTab(
                uiState,
                viewModel,
                commentText,
                onCommentTextChange = { commentText = it },
                onPostComment = {
                    val song = uiState.currentSong
                    if (commentText.isNotBlank() && song != null) {
                        viewModel.addComment(song.id, commentText)
                        commentText = ""
                    }
                }
            )
        }
    }
    
    // 反馈弹窗
    if (showFeedbackDialog) {
        SongFeedbackDialog(
            viewModel = viewModel,
            onDismiss = { 
                showFeedbackDialog = false
                // 重置反馈状态
                viewModel.resetFeedbackState()
            }
        )
    }
}

@Composable
private fun BufferedProgressSlider(
    progress: Float,
    buffered: Float,
    bufferedPercent: Int,
    duration: Float,
    onSeekTo: (Float) -> Unit,
    trackHeight: Dp = 4.dp
) {
    val safeDuration = max(duration, 1f)
    val progressFraction = (progress / safeDuration).coerceIn(0f, 1f)
    val bufferedFractionFromPosition = (buffered / safeDuration).coerceIn(0f, 1f)
    val bufferedFractionFromPercent = (bufferedPercent / 100f).coerceIn(0f, 1f)
    val bufferedFraction = max(bufferedFractionFromPosition, bufferedFractionFromPercent)

    val playedColor = MaterialTheme.colorScheme.primary
    val bufferedColor = Color.White.copy(alpha = 0.45f)
    val baseColor = Color.White.copy(alpha = 0.15f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            val centerY = size.height / 2f
            val stroke = trackHeight.toPx().coerceAtLeast(1f)
            val start = 0f
            val end = size.width

            // base track
            drawLine(
                color = baseColor,
                start = androidx.compose.ui.geometry.Offset(start, centerY),
                end = androidx.compose.ui.geometry.Offset(end, centerY),
                strokeWidth = stroke
            )

            // buffered track with subtle shadow glow
            val playedEnd = end * progressFraction
            var bufferedEnd = end * max(bufferedFraction, progressFraction)
            // 如果缓冲只比已播放多一点点，给一个“最小可见宽度”，避免肉眼看不出来
            val minExtraPx = 8.dp.toPx()
            if (bufferedEnd > playedEnd && bufferedEnd - playedEnd < minExtraPx) {
                bufferedEnd = (playedEnd + minExtraPx).coerceAtMost(end)
            }
            if (bufferedEnd > 0f) {
                // 用“双层描边”模拟阴影/缓存光晕，避免依赖 nativeCanvas（不同 Compose 版本兼容性更好）
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(start, centerY),
                    end = androidx.compose.ui.geometry.Offset(bufferedEnd, centerY),
                    strokeWidth = stroke * 2.2f
                )
                drawLine(
                    color = bufferedColor,
                    start = androidx.compose.ui.geometry.Offset(start, centerY),
                    end = androidx.compose.ui.geometry.Offset(bufferedEnd, centerY),
                    strokeWidth = stroke
                )
            }

            // played track
            if (playedEnd > 0f) {
                drawLine(
                    color = playedColor,
                    start = androidx.compose.ui.geometry.Offset(start, centerY),
                    end = androidx.compose.ui.geometry.Offset(playedEnd, centerY),
                    strokeWidth = stroke
                )
            }
        }

        // Transparent tracks; keep thumb + gestures from Slider
        Slider(
            value = progress.coerceIn(0f, safeDuration),
            onValueChange = { onSeekTo(it.coerceIn(0f, safeDuration)) },
            valueRange = 0f..safeDuration,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ColumnScope.LyricsTab(
    uiState: com.music.app.ui.MusicUiState,
    viewModel: MusicViewModel,
    listState: LazyListState
) {
    var showNoLyricsFeedbackSent by remember { mutableStateOf(false) }

    // 歌词偏移校准
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { viewModel.adjustLyricsOffset(-0.5f) }) {
            Text("-0.5s", color = Color(0xFFBDBDBD), fontSize = 12.sp)
        }
        Text(
            text = "偏移: ${String.format("%.1f", uiState.lyricsOffset)}s",
            color = Color(0xFFBDBDBD),
            fontSize = 12.sp
        )
        TextButton(onClick = { viewModel.adjustLyricsOffset(0.5f) }) {
            Text("+0.5s", color = Color(0xFFBDBDBD), fontSize = 12.sp)
        }
        // 分享按钮
        IconButton(
            onClick = {
                viewModel.createLyricsShareForCurrentSong("text")
            },
            enabled = uiState.currentSong != null && uiState.isLoggedIn
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = "分享歌词",
                tint = if (uiState.currentSong != null && uiState.isLoggedIn) MaterialTheme.colorScheme.primary else Color(0xFF666666)
            )
        }
    }

    if (uiState.lyrics.isEmpty()) {
        // 无歌词：显示反馈按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "暂无歌词 (歌词数: ${uiState.lyrics.size})",
                color = Color(0xFF888888),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "登录状态: ${uiState.isLoggedIn}, 有歌曲: ${uiState.currentSong != null}",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (uiState.isLoggedIn && uiState.currentSong != null) {
                if (showNoLyricsFeedbackSent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "反馈已提交，感谢您的建议！",
                            color = Color(0xFF4CAF50),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "我们会在24小时内为您匹配歌词",
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                        OutlinedButton(
                            onClick = {
                                uiState.currentSong?.let { 
                                    viewModel.submitNoLyricsFeedback(it.id)
                                    showNoLyricsFeedbackSent = true
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Feedback,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("没有歌词？点击反馈", fontSize = 13.sp)
                        }
                }
            } else {
                Text(
                    text = "请先登录才能提交反馈",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(uiState.lyrics) { index, line ->
                val isCurrent = index == uiState.currentLyricIndex
                Text(
                    text = line.text.ifBlank { "..." },
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFFCCCCCC),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (isCurrent) 16.sp else 14.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CommentsTab(
    uiState: com.music.app.ui.MusicUiState,
    viewModel: MusicViewModel,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onPostComment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        // 发表评论区域
        if (uiState.isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = onCommentTextChange,
                    placeholder = { Text("说点什么...", color = Color(0xFF888888)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF222222),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color(0xFF444444)
                    )
                )
                IconButton(
                    onClick = onPostComment,
                    enabled = commentText.isNotBlank() && !uiState.commentPosting
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                    )
                }
            }
        } else {
            Text(
                text = "登录后可发表评论",
                color = Color(0xFFBDBDBD),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // 评论列表
        if (uiState.commentsLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else if (uiState.currentSongComments.isEmpty()) {
            Text(
                text = "暂无评论",
                color = Color(0xFF888888),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
            ) {
                itemsIndexed(uiState.currentSongComments) { index, comment ->
                    CommentItem(
                        comment = comment,
                        isOwnComment = comment.userId == uiState.currentUser?.id,
                        onDelete = { viewModel.deleteComment(comment.id!!) },
                        onLike = { viewModel.likeComment(comment.id!!) },
                        onUnlike = { viewModel.unlikeComment(comment.id!!) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: com.music.app.data.remote.SongCommentDto,
    isOwnComment: Boolean,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onUnlike: () -> Unit
) {
    var liked by remember { mutableStateOf(false) } // 简化：未实现持久化点赞状态

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 用户头像
                AsyncImage(
                    model = comment.avatar?.let { NetworkModule.staticBaseUrl + it },
                    contentDescription = null,
                    modifier = Modifier
                        .height(32.dp)
                        .padding(end = 8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.nickname ?: comment.username ?: "用户",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = comment.createTime?.substringBefore("T") ?: "",
                        color = Color(0xFF888888),
                        fontSize = 10.sp
                    )
                }
                if (isOwnComment) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
            Text(
                text = comment.content,
                color = Color(0xFFCCCCCC),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        liked = !liked
                        if (liked) onLike() else onUnlike()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) MaterialTheme.colorScheme.primary else Color(0xFF888888)
                    )
                }
                Text(
                    text = "${comment.likeCount}",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
            // TODO: 反馈弹窗功能待完善
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
