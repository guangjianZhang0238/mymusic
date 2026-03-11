package com.music.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.app.data.remote.AlbumDto
import com.music.app.data.remote.ArtistDto
import com.music.app.data.remote.LyricLine
import com.music.app.data.remote.NetworkModule
import com.music.app.data.remote.TokenStore
import com.music.app.data.remote.SongDto
import com.music.app.data.remote.AlbumDetailDto
import com.music.app.data.remote.ArtistDetailDto
import com.music.app.data.remote.SearchSuggestionDto
import com.music.app.data.remote.UserInfo
import com.music.app.data.remote.PlaylistDto
import com.music.app.data.remote.PlaylistRequest
import com.music.app.data.remote.SongCommentDto
import com.music.app.data.remote.FeedbackDto
import com.music.app.data.remote.FeedbackResult
import com.music.app.data.remote.LyricsShareDto
import com.music.app.data.remote.UserSettingDto
import com.music.app.data.remote.UserSettings
import com.music.app.data.remote.EqualizerSettings
import com.music.app.data.repository.MusicRepository
import com.music.app.player.PlayMode
import com.music.app.ui.equalizer.EqualizerPreset
import android.util.Log
import com.music.app.player.PlayerController
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class MusicUiState(
    val hotSongs: List<SongDto> = emptyList(),
    val searchSongs: List<SongDto> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
    val albumSongs: List<SongDto> = emptyList(),
    val currentAlbumName: String = "",
    val artists: List<ArtistDto> = emptyList(),
    val currentSong: SongDto? = null,
    val isPlaying: Boolean = false,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val loading: Boolean = false,
    val searchLoading: Boolean = false,
    val discoverLoading: Boolean = false,
    val albumSongsLoading: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyrics: List<LyricLine> = emptyList(),
    val currentLyricIndex: Int = 0,
    val favoriteSongIds: Set<Long> = emptySet(),
    val isCurrentSongFavorited: Boolean = false,
    val recentPlayedSongs: List<SongDto> = emptyList(),
    val favoriteSongs: List<SongDto> = emptyList(),
    val followedArtists: List<ArtistDto> = emptyList(),
    val followedArtistIds: Set<Long> = emptySet(),
    val lyricsOffset: Float = 0f,
    val currentLyricsId: Long? = null,
    val currentAlbumDetail: AlbumDetailDto? = null,
    val albumDetailLoading: Boolean = false,
    val currentArtistDetail: ArtistDetailDto? = null,
    val artistDetailLoading: Boolean = false,
    val artistSongs: List<SongDto> = emptyList(),
    val artistSongsLoading: Boolean = false,
    val userPlaylists: List<PlaylistDto> = emptyList(),
    val publicPlaylists: List<PlaylistDto> = emptyList(),
    val currentPlaylist: PlaylistDto? = null,
    val currentPlaylistSongs: List<SongDto> = emptyList(),
    val playlistsLoading: Boolean = false,
    val playlistSongsLoading: Boolean = false,
    val currentUser: UserInfo? = null,
    val isLoggedIn: Boolean = false,
    val loginLoading: Boolean = false,
    val loginError: String? = null,
    val registerLoading: Boolean = false,
    val registerError: String? = null,
    val currentSongComments: List<SongCommentDto> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentPosting: Boolean = false,
    val userLyricsShares: List<LyricsShareDto> = emptyList(),
    val lyricsShares: List<LyricsShareDto> = emptyList(),
    val lyricsSharesLoading: Boolean = false,
    val sharePosting: Boolean = false,
    val feedbacks: List<FeedbackDto> = emptyList(),
    val feedbackLoading: Boolean = false,
    val feedbackPosting: Boolean = false,
    val feedbackSuccess: Boolean = false,
    val feedbackError: String? = null,
    val searchSuggestions: List<SearchSuggestionDto> = emptyList(),
    val suggestionsLoading: Boolean = false,
    // 播放列表相关字段
    val playbackPlaylist: List<SongDto> = emptyList(),
    val playbackPlaylistIndex: Int = -1,
    val isPlaybackPlaylistActive: Boolean = false,
    val equalizerSettings: EqualizerSettings = EqualizerSettings()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository()
    private val playerController = PlayerController(getApplication())
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()
    
    // 用户设置
    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    init {
        // 轻量初始化：避免首开触发大量网络请求导致卡顿
        initializeAppLightweight()
        observePlayer()
    }

    private fun initializeAppLightweight() {
        viewModelScope.launch {
            // 首开仅做本地/轻量任务
            loadUserSettings()
            // 延迟检查登录态，降低冷启动瞬时压力
            delay(300)
            checkLoginStatus()
            // 确保播放器状态正确初始化
            initializePlayerState()
        }
    }
    
    private fun initializePlayerState() {
        // 确保播放器状态正确初始化
        playerController.syncProgress()
    }

    private fun observePlayer() {
        viewModelScope.launch {
            var lastSongId: Long? = null
            var handledEndForSong = false
            while (true) {
                try {
                    playerController.syncProgress()
                    val song = playerController.currentSong.value
                    val progress = playerController.currentPositionMs.value
                    val duration = playerController.durationMs.value
                    val isPlaying = playerController.isPlaying.value
                    val playMode = playerController.playMode.value

                    // 只有当值真正改变时才更新UI状态，减少不必要的重组
                    val currentState = _uiState.value
                    
                    // 当歌曲变化时，重置「已处理结尾」标记
                    if (song?.id != lastSongId) {
                        lastSongId = song?.id
                        handledEndForSong = false
                    }
                    
                    if (currentState.currentSong != song ||
                        currentState.isPlaying != isPlaying ||
                        currentState.playMode != playMode ||
                        currentState.progressMs != progress ||
                        currentState.durationMs != duration) {

                        val currentLyrics = currentState.lyrics
                        val lyricsOffset = currentState.lyricsOffset
                        val adjustedProgress = progress + (lyricsOffset * 1000).toLong()
                        val lyricIndex = currentLyrics.indexOfLast { it.timeSec * 1000 <= adjustedProgress }.coerceAtLeast(0)

                        _uiState.value = currentState.copy(
                            currentSong = song,
                            isPlaying = isPlaying,
                            playMode = playMode,
                            progressMs = progress,
                            durationMs = duration,
                            currentLyricIndex = lyricIndex
                        )
                    }

                    // 自动切到下一首：当前歌曲自然播放结束时，复用现有的切歌逻辑
                    val shouldAutoNext =
                        song != null &&
                        !isPlaying &&
                        duration > 0 &&
                        progress >= (duration - 1000L).coerceAtLeast(0L) &&
                        !handledEndForSong &&
                        playMode != PlayMode.LOOP_ONE

                    if (shouldAutoNext) {
                        handledEndForSong = true
                        nextSong()
                    }

                    // 当歌曲改变时，自动加载歌词
                    if (currentState.currentSong != song && song != null) {
                        loadLyricsForSong(song)
                    }

                    // 空闲时降低轮询频率，减少 CPU 占用
                    val pollDelay = if (song == null && !isPlaying) 1200L else 500L
                    delay(pollDelay)
                } catch (e: Exception) {
                    android.util.Log.e("MusicViewModel", "播放状态同步错误", e)
                    delay(800)
                }
            }
        }
    }

    fun loadHotSongs() {
        viewModelScope.launch {
            Log.d("MusicViewModel", "开始加载热门歌曲")
            _uiState.value = _uiState.value.copy(loading = true)
            val songs = repository.fetchHotSongs()
            
            // 智能去重：同一歌手最多保留一首歌曲
            val uniqueSongs = songs
                .distinctBy { it.artistNames ?: it.artistName }
                .take(20)
                
            Log.d("MusicViewModel", "获取到热门歌曲数量: ${uniqueSongs.size}")
            _uiState.value = _uiState.value.copy(hotSongs = uniqueSongs, loading = false)
            Log.d("MusicViewModel", "UI状态更新完成，当前热门歌曲数: ${_uiState.value.hotSongs.size}")
        }
    }

    fun searchSongs(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searchLoading = true)
            val songs = repository.searchSongs(keyword)
            _uiState.value = _uiState.value.copy(searchSongs = songs, searchLoading = false)
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(discoverLoading = true)
            val albums = repository.fetchAlbums()
            _uiState.value = _uiState.value.copy(albums = albums, discoverLoading = false)
        }
    }

    fun loadAlbumDetail(albumId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(albumDetailLoading = true)
            val albumDetail = repository.fetchAlbumDetail(albumId)
            _uiState.value = _uiState.value.copy(
                currentAlbumDetail = albumDetail,
                albumDetailLoading = false
            )
        }
    }

    fun loadAlbumSongs(albumId: Long, albumName: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(albumSongsLoading = true, currentAlbumName = albumName)
            val songs = repository.fetchSongsByAlbum(albumId)
            _uiState.value = _uiState.value.copy(albumSongs = songs, albumSongsLoading = false)
        }
    }

    fun loadArtists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(discoverLoading = true)
            val artists = repository.fetchArtists()
            _uiState.value = _uiState.value.copy(artists = artists, discoverLoading = false)
        }
    }

    fun loadArtistDetail(artistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(artistDetailLoading = true)
            val artistDetail = repository.fetchArtistDetail(artistId)
            _uiState.value = _uiState.value.copy(
                currentArtistDetail = artistDetail,
                artistDetailLoading = false
            )
        }
    }

    fun loadArtistSongs(artistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(artistSongsLoading = true)
            val songs = repository.fetchSongsByArtist(artistId)
            _uiState.value = _uiState.value.copy(artistSongs = songs, artistSongsLoading = false)
        }
    }

    fun playSong(song: SongDto) {
        viewModelScope.launch {
            startPlaybackService()
            
            // 并行启动播放和加载歌词，提高响应速度
            coroutineScope {
                launch { 
                    // 使用缓存播放
                    playerController.playSongWithCache(song, NetworkModule.staticBaseUrl)
                    repository.addPlayHistory(song)
                }
                launch { loadLyricsForSong(song) }
            }
            
            // 将歌曲添加到播放列表（如果列表为空则创建新列表，否则添加到列表）
            val currentPlaylist = _uiState.value.playbackPlaylist
            val currentIndex = _uiState.value.playbackPlaylistIndex
            
            if (currentPlaylist.isEmpty()) {
                // 播放列表为空，创建新列表并播放
                setPlaybackPlaylist(listOf(song), 0)
            } else {
                // 检查歌曲是否已在播放列表中
                val existingIndex = currentPlaylist.indexOfFirst { it.id == song.id }
                if (existingIndex >= 0) {
                    // 歌曲已存在，跳转到该位置播放
                    playSongFromPlaylist(existingIndex)
                } else {
                    // 歌曲不存在，添加到播放列表末尾
                    val newPlaylist = currentPlaylist + song
                    val newIndex = newPlaylist.size - 1
                    _uiState.value = _uiState.value.copy(
                        playbackPlaylist = newPlaylist,
                        playbackPlaylistIndex = newIndex,
                        isPlaybackPlaylistActive = true
                    )
                }
            }
            
            _uiState.value = _uiState.value.copy(
                isCurrentSongFavorited = _uiState.value.favoriteSongIds.contains(song.id)
            )
            
            // 保存播放列表到服务器
            savePlaybackPlaylist()
        }
    }

    fun playSongWithQueue(songs: List<SongDto>, startIndex: Int) {
        viewModelScope.launch {
            startPlaybackService()
            setPlaybackPlaylist(songs, startIndex)
            val song = songs.getOrNull(startIndex)
            if (song != null) {
                loadLyricsForSong(song)
                viewModelScope.launch { repository.addPlayHistory(song) }
            }
        }
    }

    private fun loadLyricsForSong(song: SongDto) {
        viewModelScope.launch {
            try {
                // 并行获取歌词数据和元数据，提高加载速度
                val (lyricsData, lyricsMeta) = withContext(Dispatchers.IO) {
                    async { repository.fetchLyricsWithOffset(song.id) }.await() to
                    async { repository.fetchLyricsMeta(song.id) }.await()
                }
                
                _uiState.value = _uiState.value.copy(
                    currentSong = song,
                    lyrics = lyricsData.first,
                    currentLyricIndex = 0,
                    lyricsOffset = lyricsData.second,
                    currentLyricsId = lyricsMeta?.id
                )
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "歌词加载失败: ${e.message}", e)
                // 加载失败时清空歌词显示
                _uiState.value = _uiState.value.copy(
                    currentSong = song,
                    lyrics = emptyList(),
                    currentLyricIndex = 0,
                    lyricsOffset = 0f,
                    currentLyricsId = null
                )
            }
        }
    }
    
    // ==================== 播放列表管理 ====================
    
    /**
     * 添加歌曲到播放列表
     */
    private fun addToPlaybackPlaylist(song: SongDto) {
        val currentPlaylist = _uiState.value.playbackPlaylist
        // 检查歌曲是否已存在
        if (!currentPlaylist.any { it.id == song.id }) {
            val newPlaylist = currentPlaylist + song
            _uiState.value = _uiState.value.copy(
                playbackPlaylist = newPlaylist,
                isPlaybackPlaylistActive = true
            )
        }
    }
    
    /**
     * 设置播放列表（替换现有列表）
     */
    private fun setPlaybackPlaylist(songs: List<SongDto>, startIndex: Int = 0) {
        // 取前50首歌
        val limitedSongs = songs.take(50)
        _uiState.value = _uiState.value.copy(
            playbackPlaylist = limitedSongs,
            playbackPlaylistIndex = startIndex.coerceIn(0, limitedSongs.size - 1),
            isPlaybackPlaylistActive = true
        )
        // 保存播放列表到服务器
        savePlaybackPlaylist()
    }
    
    /**
     * 播放列表中的下一首
     */
    fun nextSongInPlaylist() {
        if (!_uiState.value.isPlaybackPlaylistActive || _uiState.value.playbackPlaylist.isEmpty()) return
        
        val currentIndex = _uiState.value.playbackPlaylistIndex
        val playlist = _uiState.value.playbackPlaylist
        
        val nextIndex = when (_uiState.value.playMode) {
            PlayMode.SHUFFLE -> {
                // 随机模式
                val randomIndex = (0 until playlist.size).filter { it != currentIndex }.randomOrNull() ?: currentIndex
                randomIndex
            }
            PlayMode.LOOP_ONE -> {
                // 单曲循环，保持当前索引
                currentIndex
            }
            else -> {
                // 顺序播放和顺序循环
                (currentIndex + 1) % playlist.size
            }
        }
        
        playSongFromPlaylist(nextIndex)
        
        // 预加载下一首歌词（如果是顺序播放）
        if (_uiState.value.playMode == PlayMode.SEQUENCE || _uiState.value.playMode == PlayMode.LOOP_ONE) {
            preloadNextSongLyrics(nextIndex)
        }
    }
    
    /**
     * 预加载下一首歌曲的歌词
     */
    private fun preloadNextSongLyrics(currentIndex: Int) {
        val playlist = _uiState.value.playbackPlaylist
        val nextIndex = (currentIndex + 1) % playlist.size
        
        // 避免重复预加载相同的歌曲
        if (nextIndex != currentIndex && nextIndex < playlist.size) {
            val nextSong = playlist[nextIndex]
            // 检查是否已经预加载过
            if (nextSong.id != _uiState.value.currentSong?.id) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        // 异步预加载，不影响当前播放
                        val lyricsData = repository.fetchLyricsWithOffset(nextSong.id)
                        val lyricsMeta = repository.fetchLyricsMeta(nextSong.id)
                        // 预加载完成，但不立即更新UI，等实际播放时再使用
                        android.util.Log.d("MusicViewModel", "预加载歌词完成: ${nextSong.title}")
                    } catch (e: Exception) {
                        android.util.Log.w("MusicViewModel", "预加载歌词失败: ${nextSong.title}, ${e.message}")
                    }
                }
            }
        }
    }
    
    /**
     * 播放列表中的上一首
     */
    fun previousSongInPlaylist() {
        if (!_uiState.value.isPlaybackPlaylistActive || _uiState.value.playbackPlaylist.isEmpty()) return
        
        val currentIndex = _uiState.value.playbackPlaylistIndex
        val playlist = _uiState.value.playbackPlaylist
        
        val prevIndex = when (_uiState.value.playMode) {
            PlayMode.SHUFFLE -> {
                // 随机模式
                val randomIndex = (0 until playlist.size).filter { it != currentIndex }.randomOrNull() ?: currentIndex
                randomIndex
            }
            else -> {
                // 其他模式
                if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
            }
        }
        
        playSongFromPlaylist(prevIndex)
    }
    
    /**
     * 从播放列表播放指定索引的歌曲
     */
    fun playSongFromPlaylist(index: Int) {
        val playlist = _uiState.value.playbackPlaylist
        if (index !in playlist.indices) return
        
        val song = playlist[index]
        startPlaybackService()
        
        // 并行处理：同时播放歌曲和加载歌词
        viewModelScope.launch {
            coroutineScope {
                launch { playerController.playSong(song, NetworkModule.staticBaseUrl) }
                launch { loadLyricsForSong(song) }
                launch { repository.addPlayHistory(song) }
            }
        }
        
        _uiState.value = _uiState.value.copy(
            playbackPlaylistIndex = index,
            isCurrentSongFavorited = _uiState.value.favoriteSongIds.contains(song.id)
        )
    }
    
    /**
     * 清空播放列表
     */
    fun clearPlaybackPlaylist() {
        _uiState.value = _uiState.value.copy(
            playbackPlaylist = emptyList(),
            playbackPlaylistIndex = -1,
            isPlaybackPlaylistActive = false
        )
    }
    
    fun togglePlayPause() {
        playerController.playPauseToggle()
    }

    fun seekTo(ms: Long) {
        playerController.seekTo(ms)
    }

    fun nextSong() {
        // 如果播放列表激活且非空，则使用播放列表逻辑
        if (_uiState.value.isPlaybackPlaylistActive && _uiState.value.playbackPlaylist.isNotEmpty()) {
            nextSongInPlaylist()
        } else {
            // 否则使用原有逻辑
            playerController.next()
            playerController.currentSong.value?.let {
                loadLyricsForSong(it)
                viewModelScope.launch { repository.addPlayHistory(it) }
                _uiState.value = _uiState.value.copy(
                    isCurrentSongFavorited = _uiState.value.favoriteSongIds.contains(it.id)
                )
            }
        }
    }

    fun previousSong() {
        // 如果播放列表激活且非空，则使用播放列表逻辑
        if (_uiState.value.isPlaybackPlaylistActive && _uiState.value.playbackPlaylist.isNotEmpty()) {
            previousSongInPlaylist()
        } else {
            // 否则使用原有逻辑
            playerController.previous()
            playerController.currentSong.value?.let {
                loadLyricsForSong(it)
                viewModelScope.launch { repository.addPlayHistory(it) }
                _uiState.value = _uiState.value.copy(
                    isCurrentSongFavorited = _uiState.value.favoriteSongIds.contains(it.id)
                )
            }
        }
    }

    fun loadFavoriteSongIds() {
        viewModelScope.launch {
            val ids = repository.fetchFavoriteSongIds()
            _uiState.value = _uiState.value.copy(
                favoriteSongIds = ids.toSet(),
                isCurrentSongFavorited = _uiState.value.currentSong?.let { ids.contains(it.id) } ?: false
            )
        }
    }

    fun loadRecentPlayedSongs() {
        viewModelScope.launch {
            val ids = repository.fetchRecentPlayIds(20)
            if (ids.isNotEmpty()) {
                val songs = repository.fetchSongsByIds(ids)
                _uiState.value = _uiState.value.copy(recentPlayedSongs = songs)
            } else {
                _uiState.value = _uiState.value.copy(recentPlayedSongs = emptyList())
            }
        }
    }

    fun loadFavoriteSongs() {
        viewModelScope.launch {
            val ids = repository.fetchFavoriteSongIds()
            if (ids.isNotEmpty()) {
                val songs = repository.fetchSongsByIds(ids)
                _uiState.value = _uiState.value.copy(
                    favoriteSongs = songs,
                    favoriteSongIds = ids.toSet()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    favoriteSongs = emptyList(),
                    favoriteSongIds = emptySet()
                )
            }
        }
    }

    fun toggleFavorite() {
        val song = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            val isFavorited = _uiState.value.isCurrentSongFavorited
            if (isFavorited) {
                repository.removeFavorite(1, song.id)
            } else {
                repository.addFavorite(1, song.id, song.title, song.albumCover)
            }
            loadFavoriteSongIds()
        }
    }

    fun togglePlayMode() {
        playerController.togglePlayMode()
    }

    fun adjustLyricsOffset(delta: Float) {
        val newOffset = _uiState.value.lyricsOffset + delta
        _uiState.value = _uiState.value.copy(lyricsOffset = newOffset)
    }

    fun resetLyricsOffset() {
        _uiState.value = _uiState.value.copy(lyricsOffset = 0f)
    }

    fun loadUserSettings() {
        viewModelScope.launch {
            val settingsList = repository.getUserSettings()
            val settings = UserSettings(
                equalizer = EqualizerSettings(
                    enabled = settingsList.find { it.settingKey == "equalizer_enabled" }?.settingValue?.toBoolean() ?: false,
                    preset = settingsList.find { it.settingKey == "equalizer_preset" }?.settingValue ?: "",
                    masterGainDb = settingsList.find { it.settingKey == "equalizer_master_gain" }?.settingValue?.toFloatOrNull() ?: 0f,
                    stereoBalance = settingsList.find { it.settingKey == "equalizer_stereo_balance" }?.settingValue?.toFloatOrNull() ?: 0f,
                    bandGainsDb = settingsList.filter { it.settingKey.startsWith("equalizer_band_") }
                        .sortedBy { it.settingKey }
                        .mapNotNull { it.settingValue.toFloatOrNull() }
                        .take(10)
                        .let { if (it.size < 10) it + List(10 - it.size) { 0f } else it },
                    pitchShiftSemitones = settingsList.find { it.settingKey == "equalizer_pitch_semitones" }
                        ?.settingValue
                        ?.toIntOrNull()
                        ?: 0
                )
            )
            _userSettings.value = settings
            _uiState.value = _uiState.value.copy(equalizerSettings = settings.equalizer ?: EqualizerSettings())
        }
    }

    fun updateUserSettings(settings: UserSettings, immediate: Boolean = false) {
        // 只更新本地状态，不保存到网络
        _userSettings.value = settings
        
        // 实时应用均衡器设置
        if (immediate) {
            applyEqualizerSettings(settings.equalizer)
        }
    }
    
    // 保存均衡器设置到服务器（退出均衡器页面时调用）
    fun saveEqualizerSettingsToServer() {
        viewModelScope.launch {
            val settings = _userSettings.value
            settings.equalizer?.let { eq ->
                repository.saveUserSetting("equalizer_enabled", eq.enabled.toString())
                eq.preset?.let { repository.saveUserSetting("equalizer_preset", it) }
                repository.saveUserSetting("equalizer_master_gain", eq.masterGainDb.toString())
                repository.saveUserSetting("equalizer_stereo_balance", eq.stereoBalance.toString())
                repository.saveUserSetting("equalizer_pitch_semitones", eq.pitchShiftSemitones.toString())
                eq.bandGainsDb.forEachIndexed { index, gain ->
                    repository.saveUserSetting("equalizer_band_$index", gain.toString())
                }
            }
        }
    }

    fun toggleEqualizerEnabled(enabled: Boolean) {
        val currentSettings = _userSettings.value
        val newEqSettings = currentSettings.equalizer?.copy(enabled = enabled) ?: EqualizerSettings(enabled = enabled)
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    fun updateEqualizerMasterGain(gain: Float) {
        val currentSettings = _userSettings.value
        val newEqSettings = currentSettings.equalizer?.copy(masterGainDb = gain) ?: EqualizerSettings(masterGainDb = gain)
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    fun updateEqualizerBand(index: Int, gain: Float) {
        val currentSettings = _userSettings.value
        val currentEq = currentSettings.equalizer
        
        // 获取当前频段增益列表
        val newGains = currentEq?.bandGainsDb?.toMutableList() ?: MutableList(10) { 0f }
        newGains[index] = gain
        
        // 检查当前预设模式，如果不是自定义且频段被修改，则切换到自定义模式
        val shouldSwitchToCustom = currentEq?.preset != null && 
                                  currentEq.preset != "CUSTOM" && 
                                  currentEq.preset != "OFF"
        
        val newPreset = if (shouldSwitchToCustom) "CUSTOM" else currentEq?.preset ?: "OFF"
        
        val newEqSettings = currentEq?.copy(
            bandGainsDb = newGains,
            preset = newPreset
        ) ?: EqualizerSettings(
            bandGainsDb = newGains,
            preset = newPreset
        )
        
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        val currentSettings = _userSettings.value
        val newEqSettings = when (preset.code) {
            "OFF" -> EqualizerSettings(enabled = false)
            "CUSTOM" -> currentSettings.equalizer ?: EqualizerSettings()
            else -> EqualizerSettings(enabled = true, preset = preset.code, bandGainsDb = preset.gains)
        }
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    private fun applyEqualizerSettings(eqSettings: EqualizerSettings?) {
        if (eqSettings == null) return
        
        // 应用均衡器设置到播放控制器
        playerController.applyEqualizer(
            enabled = eqSettings.enabled,
            masterGainDb = eqSettings.masterGainDb,
            bandGainsDb = eqSettings.bandGainsDb
        )
        
        // 应用声道平衡设置
        playerController.applyChannelBalance(eqSettings.stereoBalance ?: 0f)
        
        // 应用升降调（仅在效果器启用时生效）
        val pitchSemitones = if (eqSettings.enabled) eqSettings.pitchShiftSemitones else 0
        playerController.applyPitchShift(pitchSemitones)
    }
    
    fun updateStereoBalance(balance: Float) {
        val currentSettings = _userSettings.value
        val newEqSettings = currentSettings.equalizer?.copy(stereoBalance = balance) ?: EqualizerSettings(stereoBalance = balance)
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    fun updatePitchShift(semitones: Int) {
        val clamped = semitones.coerceIn(-12, 12)
        val currentSettings = _userSettings.value
        val currentEq = currentSettings.equalizer ?: EqualizerSettings()
        val newEqSettings = currentEq.copy(pitchShiftSemitones = clamped)
        updateUserSettings(currentSettings.copy(equalizer = newEqSettings), immediate = true)
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            val token = TokenStore.getToken(getApplication())
            if (token.isNullOrBlank()) {
                // 没有token，未登录，立即更新状态
                _uiState.value = _uiState.value.copy(isLoggedIn = false)
                return@launch
            }
            
            // 有token，先假设已登录，让UI可以立即响应
            // 同时启动后台验证
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
            
            // 在后台验证token有效性
            validateTokenAndLoadData()
        }
    }
    
    /**
     * 验证token并加载用户数据（在后台执行）
     */
    private fun validateTokenAndLoadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userInfo = repository.getCurrentUser()
                if (userInfo != null && userInfo.userInfo != null) {
                    // token有效，更新用户信息
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            currentUser = userInfo.userInfo
                        )
                    }
                    // 在后台并行加载用户数据
                    coroutineScope {
                        launch { loadFavoriteSongIds() }
                        launch { loadRecentPlayedSongs() }
                        launch { loadUserPlaylists() }
                        launch { loadPlaybackPlaylist() }
                    }
                } else {
                    // token无效或过期，清除token并标记为未登录
                    withContext(Dispatchers.Main) {
                        TokenStore.clearToken(getApplication())
                        _uiState.value = _uiState.value.copy(isLoggedIn = false)
                    }
                }
            } catch (e: Exception) {
                // 网络异常，保持当前登录状态
                Log.e("MusicViewModel", "验证token失败", e)
            }
        }
    }
    
    private fun loadPlaybackPlaylist() {
        viewModelScope.launch {
            val (songIds, currentIndex) = repository.getPlaybackPlaylist()
            if (songIds.isNotEmpty()) {
                // 根据歌曲ID获取歌曲详情
                val songs = repository.fetchSongsByIds(songIds)
                if (songs.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        playbackPlaylist = songs,
                        playbackPlaylistIndex = currentIndex.coerceIn(0, songs.size - 1),
                        isPlaybackPlaylistActive = true
                    )
                }
            }
        }
    }
    
    fun savePlaybackPlaylist() {
        viewModelScope.launch {
            val playlist = _uiState.value.playbackPlaylist
            val currentIndex = _uiState.value.playbackPlaylistIndex
            if (playlist.isNotEmpty()) {
                val songIds = playlist.map { it.id }
                repository.savePlaybackPlaylist(songIds, currentIndex)
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loginLoading = true, loginError = null)
            try {
                val result = repository.login(username, password)
                if (result != null) {
                    TokenStore.saveToken(getApplication(), result.token ?: "")
                    _uiState.value = _uiState.value.copy(
                        currentUser = result.userInfo,
                        isLoggedIn = true,
                        loginLoading = false
                    )
                    // 提示登录成功
                    Toast.makeText(getApplication(), "登录成功", Toast.LENGTH_SHORT).show()
                    // 登录成功后加载用户数据
                    loadUserPlaylists()
                    loadFavoriteSongIds()
                    loadRecentPlayedSongs()
                    // 加载播放列表
                    loadPlaybackPlaylist()
                } else {
                    _uiState.value = _uiState.value.copy(
                        loginError = "登录失败",
                        loginLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loginError = e.message ?: "网络错误",
                    loginLoading = false
                )
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(registerLoading = true, registerError = null)
            try {
                val result = repository.register(username, password)
                if (result) {
                    _uiState.value = _uiState.value.copy(registerLoading = false)
                    Toast.makeText(getApplication(), "注册成功，请登录", Toast.LENGTH_SHORT).show()
                } else {
                    _uiState.value = _uiState.value.copy(
                        registerError = "注册失败",
                        registerLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    registerError = "网络错误: ${e.message}",
                    registerLoading = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            TokenStore.clearToken(getApplication())
            _uiState.value = _uiState.value.copy(
                currentUser = null,
                isLoggedIn = false,
                favoriteSongIds = emptySet(),
                recentPlayedSongs = emptyList(),
                favoriteSongs = emptyList(),
                userPlaylists = emptyList(),
                publicPlaylists = emptyList()
            )
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val userInfo = repository.getCurrentUser()?.userInfo
            _uiState.value = _uiState.value.copy(currentUser = userInfo)
        }
    }

    fun loadSongComments(songId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentsLoading = true)
            val result = repository.getSongComments(songId)
            _uiState.value = _uiState.value.copy(currentSongComments = result?.records ?: emptyList(), commentsLoading = false)
        }
    }

    fun addComment(songId: Long, content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentPosting = true)
            val result = repository.addComment(songId, content)
            if (result != null) {
                loadSongComments(songId)
            }
            _uiState.value = _uiState.value.copy(commentPosting = false)
        }
    }

    fun loadUserPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(playlistsLoading = true)
            val playlists = repository.getUserPlaylists()
            _uiState.value = _uiState.value.copy(userPlaylists = playlists, playlistsLoading = false)
        }
    }

    fun loadPublicPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(playlistsLoading = true)
            val playlists = repository.getPublicPlaylists()
            _uiState.value = _uiState.value.copy(publicPlaylists = playlists, playlistsLoading = false)
        }
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            val request = PlaylistRequest(name = name, description = description)
            val result = repository.createPlaylist(request)
            if (result != null) {
                loadUserPlaylists()
            }
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(playlistSongsLoading = true)
            val result = repository.getPlaylistDetail(playlistId)
            if (result != null) {
                val playlist = result
                val songIds = repository.getPlaylistSongs(playlistId)
                val songs = repository.fetchSongsByIds(songIds)
                _uiState.value = _uiState.value.copy(
                    currentPlaylist = playlist,
                    currentPlaylistSongs = songs,
                    playlistSongsLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(playlistSongsLoading = false)
            }
        }
    }

    fun playPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val result = repository.getPlaylistDetail(playlistId)
            if (result != null) {
                val songIds = repository.getPlaylistSongs(playlistId)
                val songs = repository.fetchSongsByIds(songIds)
                if (songs.isNotEmpty()) {
                    playSongWithQueue(songs, 0)
                    _uiState.value = _uiState.value.copy(
                        currentPlaylist = result
                    )
                }
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            loadPlaylistSongs(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            loadPlaylistSongs(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            loadUserPlaylists()
        }
    }

    fun loadLyricsShares(lyricsId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(lyricsSharesLoading = true)
            val result = repository.getLyricsShares(lyricsId)
            if (result != null) {
                _uiState.value = _uiState.value.copy(lyricsShares = result.records ?: emptyList(), lyricsSharesLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(lyricsShares = emptyList(), lyricsSharesLoading = false)
            }
        }
    }

    fun shareLyrics(lyricsId: Long, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sharePosting = true)
            val result = repository.createLyricsShare(lyricsId)
            if (result != null) {
                loadLyricsShares(lyricsId)
            }
            _uiState.value = _uiState.value.copy(sharePosting = false)
        }
    }

    fun loadFeedbacks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(feedbackLoading = true)
            val result = repository.getMyFeedbacks()
            if (result != null) {
                _uiState.value = _uiState.value.copy(feedbacks = result.records ?: emptyList(), feedbackLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(feedbacks = emptyList(), feedbackLoading = false)
            }
        }
    }

    fun submitFeedback(feedback: FeedbackDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(feedbackPosting = true, feedbackError = null, feedbackSuccess = false)
            val result = repository.createFeedbackSimple(
                type = feedback.type,
                content = feedback.content,
                songId = feedback.songId,
                keyword = feedback.keyword,
                contact = feedback.contact,
                scene = feedback.scene
            )
            if (result != null) {
                _uiState.value = _uiState.value.copy(feedbackSuccess = true, feedbackPosting = false)
                loadFeedbacks()
            } else {
                _uiState.value = _uiState.value.copy(feedbackError = "提交失败", feedbackPosting = false)
            }
        }
    }

    fun resetFeedbackState() {
        _uiState.value = _uiState.value.copy(feedbackSuccess = false, feedbackError = null)
    }

    private fun startPlaybackService() {
        // 启动前台服务
        val intent = android.content.Intent(getApplication(), com.music.app.player.MusicPlaybackService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun loadSearchSuggestions(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(suggestionsLoading = true)
            val suggestions = repository.getSearchSuggestions(keyword)
            _uiState.value = _uiState.value.copy(searchSuggestions = suggestions, suggestionsLoading = false)
        }
    }

    // 别名方法，用于兼容其他屏幕的调用
    fun getSuggestions(keyword: String) = loadSearchSuggestions(keyword)

    fun loadDiscoverData() {
        viewModelScope.launch {
            loadAlbums()
            loadArtists()
        }
    }

    fun refreshHotSongsRandom() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val songs = repository.fetchHotSongs().shuffled().take(20)
            _uiState.value = _uiState.value.copy(hotSongs = songs, loading = false)
        }
    }

    fun loadMineData() {
        viewModelScope.launch {
            loadUserPlaylists()
            loadUserLyricsShares()
            loadFollowedArtists()
        }
    }

    fun loadUserLyricsShares() {
        viewModelScope.launch {
            val result = repository.getUserLyricsShares()
            _uiState.value = _uiState.value.copy(userLyricsShares = result?.records ?: emptyList())
        }
    }

    fun loadFollowedArtists() {
        viewModelScope.launch {
            val ids = repository.fetchFavoriteArtistIds()
            if (ids.isNotEmpty()) {
                val artists = repository.fetchArtistsByIds(ids)
                _uiState.value = _uiState.value.copy(
                    followedArtists = artists,
                    followedArtistIds = ids.toSet()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    followedArtists = emptyList(),
                    followedArtistIds = emptySet()
                )
            }
        }
    }

    fun loadMyFeedbacks() {
        loadFeedbacks()
    }

    fun removeFromPlaybackPlaylist(songId: Long) {
        val currentPlaylist = _uiState.value.playbackPlaylist
        val newPlaylist = currentPlaylist.filter { it.id != songId }
        val newIndex = if (_uiState.value.playbackPlaylistIndex >= newPlaylist.size) {
            newPlaylist.size - 1
        } else {
            _uiState.value.playbackPlaylistIndex
        }
        _uiState.value = _uiState.value.copy(
            playbackPlaylist = newPlaylist,
            playbackPlaylistIndex = newIndex,
            isPlaybackPlaylistActive = newPlaylist.isNotEmpty()
        )
    }

    fun createLyricsShareForCurrentSong(shareType: String = "text") {
        viewModelScope.launch {
            val lyricsId = _uiState.value.currentLyricsId ?: return@launch
            repository.createLyricsShare(lyricsId, shareType)
        }
    }

    fun submitNoLyricsFeedback(songId: Long) {
        viewModelScope.launch {
            repository.createFeedbackSimple(
                type = "NO_LYRICS",
                content = "歌曲缺少歌词",
                songId = songId
            )
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            repository.deleteComment(commentId)
            _uiState.value.currentSong?.id?.let { loadSongComments(it) }
        }
    }

    fun likeComment(commentId: Long) {
        viewModelScope.launch {
            repository.likeComment(commentId)
            _uiState.value.currentSong?.id?.let { loadSongComments(it) }
        }
    }

    fun unlikeComment(commentId: Long) {
        viewModelScope.launch {
            repository.unlikeComment(commentId)
            _uiState.value.currentSong?.id?.let { loadSongComments(it) }
        }
    }

    fun loadPlaylistDetail(playlistId: Long) {
        loadPlaylistSongs(playlistId)
    }

    fun incrementPlayCount(playlistId: Long) {
        viewModelScope.launch {
            repository.incrementPlayCount(playlistId)
        }
    }

    fun updatePlaylist(playlistId: Long, name: String, description: String?, coverImage: String?, isPublic: Int) {
        viewModelScope.launch {
            val request = PlaylistRequest(
                id = playlistId,
                name = name,
                description = description,
                coverImage = coverImage,
                isPublic = isPublic
            )
            repository.updatePlaylist(request)
            loadUserPlaylists()
        }
    }

    fun getCacheSize(): String {
        val bytes = playerController.getCacheSize()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    /**
     * 获取已缓存的歌曲数量
     */
    fun getCachedSongsCount(): Int {
        // 这里可以实现实际的缓存歌曲计数逻辑
        // 目前返回一个估计值或者从缓存管理器获取实际数量
        return try {
            val context = getApplication<Application>().applicationContext
            val cacheDir = context.cacheDir
            val musicCacheDir = java.io.File(cacheDir, "music_cache")
            if (musicCacheDir.exists()) {
                // 统计缓存目录下的文件数量（简单估算）
                musicCacheDir.listFiles()?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun clearCache() {
        playerController.clearCache()
    }

    fun checkLoginRequired(showWarning: Boolean = false): Boolean {
        val isLoggedIn = _uiState.value.isLoggedIn
        if (!isLoggedIn && showWarning) {
            // 可以在这里显示登录提示
        }
        return isLoggedIn
    }

    fun setThemeColor(
        color: androidx.compose.ui.graphics.Color,
        backgroundColor: androidx.compose.ui.graphics.Color? = null
    ) {
        val currentSettings = _userSettings.value
        _userSettings.value = currentSettings.copy(
            themeColor = color,
            backgroundColor = backgroundColor ?: currentSettings.backgroundColor
        )
    }

    fun submitSongFeedback(songId: Long, type: String, content: String, contact: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(feedbackPosting = true, feedbackError = null, feedbackSuccess = false)
            val result = repository.createFeedbackSimple(
                type = type,
                content = content,
                songId = songId,
                contact = contact
            )
            if (result != null) {
                _uiState.value = _uiState.value.copy(feedbackSuccess = true, feedbackPosting = false)
            } else {
                _uiState.value = _uiState.value.copy(feedbackError = "提交失败，请稍后重试", feedbackPosting = false)
            }
        }
    }
}
