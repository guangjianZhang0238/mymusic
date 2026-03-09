package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.data.remote.FeedbackDto
import com.music.app.ui.MusicViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFeedbackScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 加载我的反馈
    LaunchedEffect(Unit) {
        viewModel.loadMyFeedbacks()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "我的反馈情况", 
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B1B)
                )
            )
        },
        containerColor = Color(0xFF111111)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF111111))
                .padding(16.dp)
        ) {
            if (uiState.feedbackLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFE53935),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.feedbacks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Pending,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "暂无反馈记录",
                        color = Color(0xFFBDBDBD),
                        fontSize = 16.sp
                    )
                    Text(
                        "您的反馈对我们很重要，欢迎随时提出建议",
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.feedbacks) { feedback ->
                        FeedbackItem(feedback = feedback)
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackItem(feedback: FeedbackDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getFeedbackTypeName(feedback.type),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = feedback.keyword ?: "无关键词",
                        color = Color(0xFFBDBDBD),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                StatusBadge(status = feedback.status ?: "PENDING")
            }
            
            // 反馈内容
            Text(
                text = feedback.content,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            
            // 时间信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(feedback.createTime),
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
                
                feedback.songId?.let { songId ->
                    Text(
                        text = "关联歌曲: $songId",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
            }
            
            // 处理意见（如果有）
            feedback.handleNote?.let { note ->
                if (note.isNotBlank()) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "处理意见:",
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = note,
                            color = Color(0xFFBDBDBD),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        feedback.handleTime?.let { handleTime ->
                            Text(
                                text = "处理时间: ${formatDate(handleTime)}",
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (icon, text, backgroundColor) = when (status.uppercase()) {
        "RESOLVED" -> Triple(
            Icons.Default.CheckCircle,
            "已解决",
            Color(0xFF4CAF50)
        )
        "FUTURE" -> Triple(
            Icons.Default.Schedule,
            "后续版本解决",
            Color(0xFF2196F3)
        )
        "UNABLE" -> Triple(
            Icons.Default.Error,
            "无法解决",
            Color(0xFFF44336)
        )
        else -> Triple(
            Icons.Default.Pending,
            "待处理",
            Color(0xFFFF9800)
        )
    }
    
    val textColor = Color.White
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getFeedbackTypeName(type: String): String {
    return when (type.uppercase()) {
        "LYRICS_ERROR" -> "歌词错误"
        "LYRICS_OFFSET" -> "歌词偏移"
        "SONG_MISSING" -> "歌曲缺失"
        "OTHER" -> "其他问题"
        else -> type
    }
}

private fun formatDate(dateTimeStr: String?): String {
    if (dateTimeStr.isNullOrEmpty()) return "未知时间"
    
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // API 26及以上使用新的时间API
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val dateTime = LocalDateTime.parse(dateTimeStr.replace("T", " ").take(19))
            dateTime.format(formatter)
        } else {
            // API 26以下使用旧的时间API
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr.take(19))
            date?.let { outputFormat.format(it) } ?: dateTimeStr
        }
    } catch (e: Exception) {
        dateTimeStr
    }
}