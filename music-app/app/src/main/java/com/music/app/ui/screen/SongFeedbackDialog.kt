package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.ui.MusicViewModel

@Composable
fun SongFeedbackDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFeedbackType by remember { mutableStateOf<String?>(null) }
    var customReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val feedbackTypes = listOf(
        "歌词错误" to "LYRICS_ERROR",
        "缺少歌词" to "NO_LYRICS",
        "歌词时间不匹配" to "LYRICS_TIMING_MISMATCH",
        "歌曲错误" to "SONG_ERROR",
        "其他" to "OTHER"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "歌曲反馈",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color(0xFF888888)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 当前歌曲信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "反馈歌曲:",
                            color = Color(0xFFBDBDBD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.currentSong?.title ?: "未知歌曲",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.currentSong?.artistName ?: "",
                            color = Color(0xFFBDBDBD),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 反馈类型选择
                Text(
                    text = "请选择反馈类型:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    feedbackTypes.forEach { (displayName, typeValue) ->
                        FeedbackTypeItem(
                            displayName = displayName,
                            isSelected = selectedFeedbackType == typeValue,
                            onClick = { selectedFeedbackType = typeValue }
                        )
                    }
                }

                // 自定义原因输入框（当选择"其他"时显示）
                if (selectedFeedbackType == "OTHER") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("请输入具体原因", color = Color(0xFFBDBDBD)) },
                        placeholder = { Text("请详细描述您遇到的问题...", color = Color(0xFF888888)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFE53935),
                            unfocusedIndicatorColor = Color(0xFF444444),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color(0xFFBDBDBD)
                        ),
                        supportingText = {
                            Text(
                                text = "${customReason.length}/200",
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        isError = customReason.isBlank(),
                        singleLine = false,
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 提交按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF888888)
                        )
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            if (selectedFeedbackType != null && 
                                (selectedFeedbackType != "OTHER" || customReason.isNotBlank())) {
                                isSubmitting = true
                                val content = if (selectedFeedbackType == "OTHER") {
                                    customReason
                                } else {
                                    feedbackTypes.find { it.second == selectedFeedbackType }?.first ?: ""
                                }
                                
                                uiState.currentSong?.id?.let { songId ->
                                    viewModel.submitSongFeedback(
                                        songId = songId,
                                        type = selectedFeedbackType!!,
                                        content = content,
                                        contact = null
                                    )
                                }
                            }
                        },
                        enabled = selectedFeedbackType != null && 
                                 (selectedFeedbackType != "OTHER" || customReason.isNotBlank()) && 
                                 !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            disabledContainerColor = Color(0xFF444444),
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF888888)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("提交中...")
                        } else {
                            Text("提交反馈")
                        }
                    }
                }

                // 显示提交结果
                if (uiState.feedbackSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Feedback,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "反馈提交成功！感谢您的帮助。",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (uiState.feedbackError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.feedbackError ?: "提交失败",
                                color = Color.White,
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
private fun FeedbackTypeItem(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFE53935) else Color(0xFF444444),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) Color(0xFFE53935).copy(alpha = 0.1f) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFE53935),
                    unselectedColor = Color(0xFF888888)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = displayName,
                color = if (isSelected) Color(0xFFE53935) else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}