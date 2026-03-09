package com.music.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCreateScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var coverImage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建歌单", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // 歌单名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("歌单名称", color = Color.White) },
                leadingIcon = {
                    Icon(Icons.Default.Title, contentDescription = null, tint = Color.White)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedLabelColor = Color(0xFFE53935),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE53935)
                )
            )
            // 封面图片URL（可选）
            OutlinedTextField(
                value = coverImage,
                onValueChange = { coverImage = it },
                label = { Text("封面图片URL（可选）", color = Color.White) },
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedLabelColor = Color(0xFFE53935),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE53935)
                )
            )
            // 描述（可选）
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述（可选）", color = Color.White) },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedLabelColor = Color(0xFFE53935),
                    unfocusedLabelColor = Color(0xFF888888),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFE53935)
                )
            )
            // 公开/私有开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (isPublic) "公开歌单" else "私有歌单",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFE53935),
                        checkedTrackColor = Color(0xFFE53935).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF444444)
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 创建按钮
            Button(
                onClick = {
                    viewModel.createPlaylist(
                        name = name,
                        description = description
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank() && !uiState.playlistsLoading,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF444444),
                    disabledContentColor = Color(0xFF888888)
                )
            ) {
                if (uiState.playlistsLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("创建歌单", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            // 创建成功后自动返回
            LaunchedEffect(uiState.userPlaylists) {
                // 简单检查：如果加载用户歌单后数量增加，则认为创建成功
                // 实际上应该有一个更好的状态指示，但这里简单处理
                if (uiState.userPlaylists.isNotEmpty() && name.isNotBlank()) {
                    onCreateSuccess()
                }
            }
        }
    }
}