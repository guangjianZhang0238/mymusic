package com.music.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.NetworkModule
import com.music.app.ui.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CommentsDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var commentText by remember { mutableStateOf("") }

    // 打开弹窗时自动加载当前歌曲评论
    androidx.compose.runtime.LaunchedEffect(uiState.currentSong?.id) {
        uiState.currentSong?.id?.let { songId ->
            viewModel.loadSongComments(songId)
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF111111)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.currentSongComments.isNotEmpty())
                            "歌曲评论 (${uiState.currentSongComments.size})"
                        else
                            "歌曲评论",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 评论列表
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    when {
                        uiState.commentsLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color(0xFFE53935)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "加载评论中...",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        uiState.currentSongComments.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "暂无评论",
                                color = Color(0xFFAAAAAA),
                                fontSize = 16.sp
                            )
                        }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                items(uiState.currentSongComments) { comment ->
                                    CommentItem(
                                        comment = comment,
                                        isOwnComment = comment.userId == uiState.currentUser?.id,
                                        onDelete = {
                                            comment.id?.let { id -> viewModel.deleteComment(id) }
                                        },
                                        onLike = {
                                            comment.id?.let { id -> viewModel.likeComment(id) }
                                        },
                                        onUnlike = {
                                            comment.id?.let { id -> viewModel.unlikeComment(id) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 发表评论输入框
                if (uiState.isLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF181818), shape = CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("发表你的评论...", color = Color(0xFFAAAAAA)) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedPlaceholderColor = Color(0xFFAAAAAA),
                                unfocusedPlaceholderColor = Color(0xFFAAAAAA)
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank() && uiState.currentSong?.id != null) {
                                    viewModel.addComment(uiState.currentSong!!.id, commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank() && !uiState.commentPosting
                        ) {
                            if (uiState.commentPosting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFE53935)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "发送",
                                    tint = if (commentText.isNotBlank()) Color(0xFFE53935) else Color(0xFF666666)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                        border = BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "登录后才能发表评论",
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp
                            )
                        }
                    }
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
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 用户信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Surface(
                    modifier = Modifier
                        .size(32.dp),
                    shape = CircleShape,
                    color = Color(0xFF444444)
                ) {
                    AsyncImage(
                        model = comment.avatar?.let { NetworkModule.staticBaseUrl + it },
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = comment.nickname ?: comment.username ?: "匿名用户",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatTime(comment.createTime),
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
                if (isOwnComment) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = Color(0xFFE53935), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content,
                color = Color(0xFFDDDDDD),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 点赞信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                            tint = if (liked) Color(0xFFE53935) else Color(0xFF888888)
                        )
                    }
                    Text(
                        text = "${comment.likeCount}",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(timeStr: String?): String {
    if (timeStr.isNullOrEmpty()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr)
        val now = System.currentTimeMillis()
        val diff = now - (date?.time ?: 0)

        when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        timeStr
    }
}