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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.data.repository.MusicRepository
import com.music.app.data.remote.SearchSuggestionDto
import com.music.app.ui.MusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onArtistClick: (Long) -> Unit = {},
    onAlbumClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var keyword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp)
    ) {
        // 搜索框 - 使用高对比度颜色
        OutlinedTextField(
            value = keyword,
            onValueChange = { newKeyword ->
                keyword = newKeyword
                viewModel.searchSongs(newKeyword)
                if (newKeyword.isNotBlank()) {
                    viewModel.getSuggestions(newKeyword)
                }
            },
            label = { Text("搜索歌曲", color = Color.White) },
            placeholder = { Text("输入拼音首字母或全拼搜索，如：z 或 zhang", color = Color(0xFFBDBDBD)) },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1C1C1E),
                unfocusedContainerColor = Color(0xFF1C1C1E),
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color(0xFF444444),
                cursorColor = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 搜索联想结果
        if (uiState.suggestionsLoading) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("加载中...", color = Color(0xFFBDBDBD))
            }
        } else if (keyword.isNotBlank() && uiState.searchSuggestions.isNotEmpty()) {
            Text(
                text = "搜索联想：",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.searchSuggestions.take(10)) { index, suggestion ->
                    SuggestionItem(
                        suggestion = suggestion,
                        onItemClick = { 
                            // 点击联想项
                            when (suggestion.type) {
                                1 -> {
                                    // 歌曲：直接播放
                                    viewModel.searchSongs(suggestion.name)
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        kotlinx.coroutines.delay(500)
                                        if (viewModel.uiState.value.searchSongs.isNotEmpty()) {
                                            viewModel.playSongWithQueue(viewModel.uiState.value.searchSongs, 0)
                                        }
                                    }
                                }
                                2 -> {
                                    // 歌手：跳转到歌手详情页
                                    onArtistClick(suggestion.id)
                                }
                                3 -> {
                                    // 专辑：跳转到专辑详情页
                                    onAlbumClick(suggestion.id)
                                }
                                else -> {
                                    keyword = suggestion.name
                                    viewModel.searchSongs(suggestion.name)
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 搜索结果
        if (uiState.searchLoading) {
            CircularProgressIndicator(
                color = Color(0xFFE53935),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        if (!uiState.searchLoading && keyword.isNotBlank() && uiState.searchSongs.isEmpty()) {
            Text(
                text = "没有找到你想要的歌？可以直接反馈给我",
                color = Color(0xFFBDBDBD),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val feedbackId = MusicRepository().createFeedbackSimple(
                            type = "SONG_MISSING",
                            content = "搜索未命中：$keyword",
                            keyword = keyword,
                            scene = "SEARCH_EMPTY"
                        )
                        if (feedbackId != null) {
                            // 显示成功提示
                            CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar("反馈提交成功！感谢您的建议")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("反馈缺少歌曲")
            }
        }
        
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(uiState.searchSongs) { index, song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            // 单曲点击：添加到播放列表（不替换）
                            if (uiState.isLoggedIn) {
                                viewModel.playSong(song)
                            } else {
                                viewModel.playSong(song)
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(song.artistName ?: "未知歌手", color = Color(0xFFBDBDBD))
                        }
                        // 添加播放图标
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "播放",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Snackbar提示
    SnackbarHost(hostState = snackbarHostState)
}

@Composable
fun SuggestionItem(
    suggestion: SearchSuggestionDto,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252526))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Icon(
                imageVector = when (suggestion.type) {
                    1 -> Icons.Default.MusicNote  // 歌曲
                    2 -> Icons.Default.AccountCircle  // 歌手
                    3 -> Icons.Default.Album  // 专辑
                    else -> Icons.Default.MusicNote
                },
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = suggestion.name,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    // 为歌曲类型添加播放指示
                    if (suggestion.type == 1) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "点击播放",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                when (suggestion.type) {
                    1 -> { // 歌曲
                        Row {
                            suggestion.artistName?.let { artist ->
                                Text(
                                    text = artist,
                                    color = Color(0xFFBDBDBD),
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                            }
                            suggestion.albumName?.let { album ->
                                Text(
                                    text = " • ${album}",
                                    color = Color(0xFFBDBDBD),
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                            }
                        }
                    }
                    2 -> { // 歌手
                        Text(
                            text = "歌手",
                            color = Color(0xFFBDBDBD),
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                    3 -> { // 专辑
                        suggestion.artistName?.let { artist ->
                            Text(
                                text = artist,
                                color = Color(0xFFBDBDBD),
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            )
                        }
                    }
                }
            }
        }
    }
}
