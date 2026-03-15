package com.music.app.ui

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.music.app.data.remote.NetworkModule
import com.music.app.data.remote.TokenStore
import com.music.app.player.PlayMode
import com.music.app.ui.screen.AlbumDetailScreen
import com.music.app.ui.screen.ArtistDetailScreen
import com.music.app.ui.screen.CommentsDialog
import com.music.app.ui.screen.DiscoverScreen
import com.music.app.ui.screen.EqualizerDialog
import com.music.app.ui.screen.HomeScreen
import com.music.app.ui.screen.LoginScreen
import com.music.app.ui.screen.MineScreen
import com.music.app.ui.screen.MyFeedbackScreen
import com.music.app.ui.screen.PlaybackPlaylistScreen
import com.music.app.ui.screen.PlaylistCreateScreen
import com.music.app.ui.screen.PlaylistDetailScreen
import com.music.app.ui.screen.PlaylistEditScreen
import com.music.app.ui.screen.PlaylistListScreen
import com.music.app.ui.screen.RecentPlayedScreen
import com.music.app.ui.screen.RegisterScreen
import com.music.app.ui.screen.SearchScreen
import com.music.app.ui.screen.SettingsScreen
import com.music.app.ui.screen.SongFeedbackDialog

@Composable
fun MusicApp(viewModel: MusicViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    var showPlayerDialog by remember { mutableStateOf(false) }
    var initialLoginCheckDone by remember { mutableStateOf(false) }
    
    // 获取当前上下文用于检查本地token
    val context = androidx.compose.ui.platform.LocalContext.current
    // 根据本地 token 且 15 天免登期内，决定是否直接进首页
    val hasLocalToken by remember { 
        mutableStateOf(TokenStore.isLoggedInWithin15Days(context))
    }

    // 应用启动时立即检查登录状态（后台验证）
    LaunchedEffect(Unit) {
        viewModel.checkLoginStatus()
        initialLoginCheckDone = true
    }

    // 路由导航守卫：未登录时强制跳转到登录页
    LaunchedEffect(currentDestination, uiState.isLoggedIn, initialLoginCheckDone) {
        if (initialLoginCheckDone
            && currentDestination?.route != "login"
            && currentDestination?.route != "register"
            && !uiState.isLoggedIn
        ) {
            // 未登录，强制跳转到登录页，清空回退栈
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }


    if (showPlayerDialog && uiState.currentSong != null) {
        PlayerDialog(
            viewModel = viewModel,
            onDismiss = { showPlayerDialog = false }
        )
    }

    Scaffold(
        containerColor = userSettings.backgroundColor,
        bottomBar = {
            Column {
                MiniPlayerBar(
                    songName = uiState.currentSong?.title ?: "",
                    artist = uiState.currentSong?.artistNames ?: uiState.currentSong?.artistName ?: "",
                    isPlaying = uiState.isPlaying,
                    onClick = {
                        if (uiState.currentSong != null) {
                            showPlayerDialog = true
                        }
                    },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextSong() },
                    onPlaylistClick = {
                        if (uiState.playbackPlaylist.isNotEmpty()) {
                            navController.navigate("playbackPlaylist")
                        }
                    }
                )
                NavigationBar(containerColor = Color(0xFF1B1B1B)) {
                    val items = listOf(
                        NavItem("home", "首页", Icons.Default.Home),
                        NavItem("discover", "发现", Icons.Default.LibraryMusic),
                        NavItem("search", "搜索", Icons.Default.Search),
                        NavItem("mine", "我的", Icons.Default.Person)
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            enabled = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // 根据本地token决定启动页面：无token时先显示登录页
            startDestination = if (hasLocalToken) "home" else "login",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(userSettings.backgroundColor)
        ) {
            composable("home") { HomeScreen(viewModel = viewModel) }
            composable("discover") {
                DiscoverScreen(
                    viewModel = viewModel, 
                    onAlbumClick = { albumId -> navController.navigate("albumDetail/$albumId") }
                )
            }
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onArtistClick = { artistId -> navController.navigate("artistDetail/$artistId") },
                    onAlbumClick = { albumId -> navController.navigate("albumDetail/$albumId") }
                )
            }
            composable("mine") {
                MineScreen(
                    viewModel = viewModel,
                    onArtistClick = { artistId -> navController.navigate("artistDetail/$artistId") },
                    onLoginClick = { navController.navigate("login") },
                    onRegisterClick = { navController.navigate("register") },
                    onPlaylistClick = { navController.navigate("playlists") },
                    onSettingsClick = { navController.navigate("settings") },
                    onRecentPlayedClick = { navController.navigate("recentPlayed") }
                )
            }
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onLoginSuccess = {
                        // 登录成功后跳转到首页
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onRegisterClick = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onRegisterSuccess = { navController.popBackStack() },
                    onLoginClick = { navController.navigate("login") }
                )
            }
            composable("albumDetail/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull() ?: 0L
                AlbumDetailScreen(
                    albumId = albumId, 
                    viewModel = viewModel, 
                    onBack = { navController.popBackStack() }
                )
            }
            composable("artistDetail/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId")?.toLongOrNull() ?: 0L
                ArtistDetailScreen(
                    artistId = artistId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumId -> navController.navigate("albumDetail/$albumId") }
                )
            }
            composable("playlists") {
                PlaylistListScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPlaylistClick = { playlistId -> navController.navigate("playlistDetail/$playlistId") },
                    onCreatePlaylistClick = { navController.navigate("playlistCreate") }
                )
            }
            composable("playlistDetail/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSongClick = { },
                    onEditPlaylistClick = { pid -> navController.navigate("playlistEdit/$pid") },
                    onAddSongsClick = { }
                )
            }
            composable("playlistCreate") {
                PlaylistCreateScreen(
                    viewModel = viewModel, 
                    onBackClick = { navController.popBackStack() }, 
                    onCreateSuccess = { navController.popBackStack() }
                )
            }
            composable("playlistEdit/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
                PlaylistEditScreen(
                    viewModel = viewModel, 
                    playlistId = playlistId, 
                    onBackClick = { navController.popBackStack() }, 
                    onUpdateSuccess = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel, 
                    onBackClick = { navController.popBackStack() }, 
                    onNavigateToFeedback = { navController.navigate("myFeedback") }
                )
            }
            composable("myFeedback") {
                MyFeedbackScreen(
                    viewModel = viewModel, 
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("recentPlayed") {
                RecentPlayedScreen(
                    viewModel = viewModel, 
                    onBackClick = { navController.popBackStack() }, 
                    onSongClick = { song -> viewModel.playSong(song) }
                )
            }
            composable("playbackPlaylist") {
                PlaybackPlaylistScreen(
                    viewModel = viewModel, 
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    songName: String,
    artist: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPlaylistClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222222))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(end = 8.dp)
        ) {
            Column {
                Text(text = songName.ifBlank { "暂无播放" }, color = Color.White, fontWeight = FontWeight.Medium)
                Text(text = artist.ifBlank { "" }, color = Color(0xFFBBBBBB), style = MaterialTheme.typography.bodySmall)
            }
        }

        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 下一首按钮
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "下一首",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 播放列表按钮
        IconButton(
            onClick = onPlaylistClick,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Default.PlaylistPlay,
                contentDescription = "播放列表",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

@Composable
fun PlayerDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentLyricIndex, uiState.lyrics.isEmpty()) {
        if (uiState.lyrics.isNotEmpty() && uiState.currentLyricIndex >= 0) {
            listState.animateScrollToItem(index = uiState.currentLyricIndex.coerceAtLeast(0), scrollOffset = -200)
        }
    }

    LaunchedEffect(uiState.currentSong?.id) {
        uiState.currentSong?.id?.let { songId -> viewModel.loadSongComments(songId) }
    }

    val progress = uiState.progressMs.toFloat().coerceAtLeast(0f)
    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().background(Color(0xFF111111)), color = Color(0xFF111111)) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF111111))) {
                AsyncImage(
                    model = uiState.currentSong?.albumCover?.let { NetworkModule.staticBaseUrl + it },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.3f)
                )

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        if (uiState.lyrics.isNotEmpty()) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                itemsIndexed(uiState.lyrics) { index, line ->
                                    val isCurrent = index == uiState.currentLyricIndex
                                    Text(
                                        text = line.text.ifBlank { "..." },
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isCurrent) 20.sp else 16.sp,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            AsyncImage(
                                model = uiState.currentSong?.albumCover?.let { NetworkModule.staticBaseUrl + it },
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth(0.8f).height(200.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.currentSong?.title ?: "暂无歌曲",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = uiState.currentSong?.artistNames ?: uiState.currentSong?.artistName ?: "",
                                    color = Color(0xFFBDBDBD),
                                    modifier = Modifier.padding(top = 4.dp),
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                if (viewModel.checkLoginRequired(showWarning = true)) {
                                    showFeedbackDialog = true
                                    viewModel.resetFeedbackState()
                                }
                            }) {
                                Icon(Icons.Default.Feedback, contentDescription = "反馈", tint = Color.White)
                            }
                            IconButton(onClick = { showCommentsDialog = true }) {
                                Icon(Icons.Default.Comment, contentDescription = "评论", tint = Color.White)
                            }
                            IconButton(
                                onClick = { 
                                    if (viewModel.checkLoginRequired(showWarning = true)) {
                                        showEqualizerDialog = true
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "效果器", tint = Color.White)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Slider(value = progress, onValueChange = { viewModel.seekTo(it.toLong()) }, valueRange = 0f..duration, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMs(uiState.progressMs), color = Color(0xFFB0B0B0))
                            Text(formatMs(uiState.durationMs), color = Color(0xFFB0B0B0))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 52.dp),
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
                                contentDescription = null,
                                tint = if (uiState.playMode == PlayMode.SEQUENCE) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.previousSong() }) { Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White) }
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(60.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.nextSong() }) { Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (uiState.isCurrentSongFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (uiState.isCurrentSongFavorited) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCommentsDialog) {
        CommentsDialog(viewModel = viewModel, onDismiss = { showCommentsDialog = false })
    }
    if (showEqualizerDialog) {
        EqualizerDialog(viewModel = viewModel, onDismiss = { showEqualizerDialog = false })
    }
    if (showFeedbackDialog) {
        SongFeedbackDialog(
            viewModel = viewModel,
            onDismiss = {
                showFeedbackDialog = false
                viewModel.resetFeedbackState()
            }
        )
    }
}
