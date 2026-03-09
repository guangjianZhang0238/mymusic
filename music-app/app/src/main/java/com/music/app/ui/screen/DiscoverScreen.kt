package com.music.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music.app.data.remote.NetworkModule
import com.music.app.ui.MusicViewModel

@Composable
fun DiscoverScreen(viewModel: MusicViewModel, onAlbumClick: (Long) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.albums.size, uiState.discoverLoading) {
        if (uiState.albums.isEmpty() && !uiState.discoverLoading) {
            viewModel.loadDiscoverData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .padding(16.dp)
    ) {
        Text("歌单 / 专辑", color = Color.White, fontWeight = FontWeight.Bold)

        if (uiState.discoverLoading) {
            CircularProgressIndicator(
                color = Color(0xFFE53935),
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.albums) { album ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = album.coverImage?.let { NetworkModule.staticBaseUrl + it },
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            Text(album.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(album.artistName ?: "", color = Color(0xFFBDBDBD))
                        }
                    }
                }
            }
        }
    }
}
