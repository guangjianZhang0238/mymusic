package com.music.app.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.music.app.data.remote.SongDto
import com.music.app.data.remote.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.pow

enum class PlayMode {
    SEQUENCE,   // 顺序播放
    LOOP_ONE,   // 单曲循环
    SHUFFLE     // 随机播放
}

class PlayerController(private val context: Context) {
    // 使用缓存管理器获取全局唯一的缓存实例
    private val downloadCache = CacheManager.getCache(context)
    
    // 使用 OkHttp 作为上游数据源，确保中文/特殊字符路径的 URL 处理一致
    // 与业务 API 保持一致：自动携带 token，避免静态资源被鉴权拦截后返回 HTML/JSON 导致音频解码失败
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = TokenStore.getToken(context)
            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val dataSourceFactory: DataSource.Factory =
        OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("MusicApp/1.0")
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Icy-MetaData" to "1"
                )
            )
    
    // 声道平衡处理器（可动态修改 balance 属性）
    val channelBalanceProcessor = ChannelBalanceAudioProcessor()

    // 自定义 RenderersFactory，注入声道平衡处理器
    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioOutputPlaybackParams: Boolean
        ): androidx.media3.exoplayer.audio.AudioSink {
            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf<AudioProcessor>(channelBalanceProcessor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioOutputPlaybackParams)
                .build()
        }
    }

    // 使用缓存数据源创建ExoPlayer
    val player = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentSong = MutableStateFlow<SongDto?>(null)
    val currentSong: StateFlow<SongDto?> = _currentSong.asStateFlow()

    private val _playlist = MutableStateFlow<List<SongDto>>(emptyList())
    val playlist: StateFlow<List<SongDto>> = _playlist.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.SEQUENCE)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    private val _songEndedEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val songEndedEvents: SharedFlow<Long> = _songEndedEvents

    private var shuffledIndices: List<Int> = emptyList()
    private var equalizer: Equalizer? = null
    private val eqFrequencies = intArrayOf(100, 200, 400, 600, 1000, 3000, 6000, 12000, 14000, 16000)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // 确保在正确的状态下获取时长
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    val newDuration = player.duration.coerceAtLeast(0L)
                    if (_durationMs.value != newDuration) {
                        _durationMs.value = newDuration
                        android.util.Log.d("PlayerController", "更新歌曲时长: ${newDuration}ms")
                    }
                }
                // 单曲循环：播放完成时重新播放当前歌曲
                if (playbackState == Player.STATE_ENDED && _playMode.value == PlayMode.LOOP_ONE) {
                    player.seekTo(0)
                    player.play()
                }

                if (playbackState == Player.STATE_ENDED && _playMode.value != PlayMode.LOOP_ONE) {
                    _currentSong.value?.id?.let { endedSongId ->
                        _songEndedEvents.tryEmit(endedSongId)
                    }
                }
                // 处理播放错误状态
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrentSongByIndex()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // 处理播放错误
                android.util.Log.e("PlayerController", "播放错误: ${error.message}", error)
                _isPlaying.value = false
                
                // 检查具体错误类型并给出相应处理建议
                val cause = error.cause
                when {
                    cause is androidx.media3.exoplayer.source.UnrecognizedInputFormatException -> {
                        android.util.Log.e("PlayerController", "音频格式无法识别，请检查文件格式或服务器配置")
                    }
                    cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException -> {
                        android.util.Log.e("PlayerController", "HTTP响应错误，状态码: ${cause.responseCode}")
                    }
                    else -> {
                        android.util.Log.e("PlayerController", "其他播放错误: ${error.javaClass.simpleName}, cause: ${cause?.javaClass?.simpleName}")
                    }
                }
                
                // 可以在这里添加错误重试逻辑或用户提示
            }
        })
    }

    private fun syncCurrentSongByIndex() {
        val idx = player.currentMediaItemIndex
        val list = _playlist.value
        if (idx in list.indices) {
            _currentSong.value = list[idx]
        }
    }

    fun setPlaylist(songs: List<SongDto>, baseStaticUrl: String, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _playlist.value = songs

        val mediaItems = songs.mapNotNull { song ->
            val path = song.filePath ?: return@mapNotNull null
            val mediaUrl = if (path.startsWith("http")) {
                path
            } else {
                // 对每个路径段做编码，避免把扩展名里的点号等也误处理
                val normalizedPath = path.replace("\\", "/").trimStart('/')
                val encodedPath = normalizedPath
                    .split("/")
                    .joinToString("/") { segment -> Uri.encode(segment) }
                "$baseStaticUrl$encodedPath"
            }
            android.util.Log.d("PlayerController", "构建媒体URL: $mediaUrl")
            val lowerPath = path.lowercase()
            val mimeType = when {
                lowerPath.endsWith(".flac") -> MimeTypes.AUDIO_FLAC
                lowerPath.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG
                lowerPath.endsWith(".wav") -> MimeTypes.AUDIO_WAV
                lowerPath.endsWith(".m4a") -> MimeTypes.AUDIO_MP4
                lowerPath.endsWith(".aac") -> MimeTypes.AUDIO_AAC
                lowerPath.endsWith(".ogg") -> MimeTypes.AUDIO_OGG
                else -> null
            }
            MediaItem.Builder()
                .setUri(mediaUrl)
                .apply { mimeType?.let { setMimeType(it) } }
                .build()
        }
        if (mediaItems.isEmpty()) return

        // 生成随机索引
        shuffledIndices = if (_playMode.value == PlayMode.SHUFFLE) {
            songs.indices.shuffled()
        } else {
            songs.indices.toList()
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.repeatMode = if (_playMode.value == PlayMode.LOOP_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.prepare()
        player.playWhenReady = true
        syncCurrentSongByIndex()
    }

    fun playSong(song: SongDto, baseStaticUrl: String) {
        setPlaylist(listOf(song), baseStaticUrl, 0)
    }
    
    /**
     * 智能播放歌曲 - 优先使用缓存，不存在则从网络加载
     */
    fun playSongWithCache(song: SongDto, baseStaticUrl: String) {
        try {
            // 检查歌曲是否已在缓存中
            val isCached = CacheManager.isSongCached(context, song.id)
            
            if (isCached) {
                android.util.Log.d("PlayerController", "歌曲 ${song.title} 已在缓存中，直接播放")
            } else {
                android.util.Log.d("PlayerController", "歌曲 ${song.title} 未缓存，从网络加载并缓存")
            }
            
            // ExoPlayer会自动处理缓存逻辑
            playSong(song, baseStaticUrl)
            
        } catch (e: Exception) {
            android.util.Log.e("PlayerController", "播放歌曲失败: ${e.message}", e)
            // 出错时回退到普通播放
            playSong(song, baseStaticUrl)
        }
    }
    
    /**
     * 获取歌曲缓存状态
     */
    fun getSongCacheStatus(songId: Long): CachedSongInfo? {
        return CacheManager.getCachedSongInfo(context, songId)
    }
    
    /**
     * 检查歌曲是否已缓存
     */
    fun isSongCached(songId: Long): Boolean {
        return CacheManager.isSongCached(context, songId)
    }

    fun playPauseToggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun next() {
        if (_playMode.value == PlayMode.SHUFFLE && shuffledIndices.isNotEmpty()) {
            val currentIdx = player.currentMediaItemIndex
            val currentShuffleIdx = shuffledIndices.indexOf(currentIdx)
            val nextShuffleIdx = (currentShuffleIdx + 1) % shuffledIndices.size
            player.seekTo(shuffledIndices[nextShuffleIdx], 0L)
            player.playWhenReady = true
            syncCurrentSongByIndex()
        } else if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.playWhenReady = true
            syncCurrentSongByIndex()
        } else if (_playMode.value == PlayMode.LOOP_ONE) {
            // 单曲循环模式下回到开头
            player.seekTo(0)
            player.playWhenReady = true
        }
        // 注意：对于顺序播放模式，到达末尾时不会自动播放下一首
    }

    fun previous() {
        if (_playMode.value == PlayMode.SHUFFLE && shuffledIndices.isNotEmpty()) {
            val currentIdx = player.currentMediaItemIndex
            val currentShuffleIdx = shuffledIndices.indexOf(currentIdx)
            val prevShuffleIdx = if (currentShuffleIdx > 0) currentShuffleIdx - 1 else shuffledIndices.size - 1
            player.seekTo(shuffledIndices[prevShuffleIdx], 0L)
            player.playWhenReady = true
            syncCurrentSongByIndex()
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.playWhenReady = true
            syncCurrentSongByIndex()
        } else {
            // 如果没有上一首，回到当前歌曲开头
            player.seekTo(0)
            player.playWhenReady = true
        }
    }

    fun togglePlayMode() {
        _playMode.value = when (_playMode.value) {
            PlayMode.SEQUENCE -> PlayMode.LOOP_ONE
            PlayMode.LOOP_ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.SEQUENCE
        }
        // 更新ExoPlayer的重复模式
        player.repeatMode = when (_playMode.value) {
            PlayMode.LOOP_ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        // 如果切换到随机模式，生成新的随机索引
        if (_playMode.value == PlayMode.SHUFFLE && _playlist.value.isNotEmpty()) {
            shuffledIndices = _playlist.value.indices.shuffled()
        }
    }

    fun syncProgress() {
        try {
            val currentPosition = player.currentPosition.coerceAtLeast(0L)
            val bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
            val duration = player.duration.coerceAtLeast(0L)
            
            // 只有当值发生变化时才更新，避免不必要的状态更新
            if (_currentPositionMs.value != currentPosition) {
                _currentPositionMs.value = currentPosition
            }

            if (_bufferedPositionMs.value != bufferedPosition) {
                _bufferedPositionMs.value = bufferedPosition
            }
            
            // 更严格的时长更新条件
            if (duration > 0 && _durationMs.value != duration) {
                _durationMs.value = duration
                android.util.Log.d("PlayerController", "同步进度 - 当前位置: ${currentPosition}ms, 时长: ${duration}ms")
            } else if (duration == 0L && player.playbackState == Player.STATE_READY) {
                // 当时长为0但播放器已准备好时，稍后再尝试获取
                android.util.Log.w("PlayerController", "时长为0，但播放器已准备好，可能需要重新获取")
            }
        } catch (e: Exception) {
            // 静默处理播放器异常，避免影响主流程
            android.util.Log.w("PlayerController", "进度同步异常: ${e.message}")
        }
    }

    fun applyEqualizer(enabled: Boolean, masterGainDb: Float, bandGainsDb: List<Float>) {
        val safeBands = if (bandGainsDb.size == 10) bandGainsDb else List(10) { 0f }
        runCatching {
            if (enabled) {
                if (equalizer == null) {
                    equalizer = Equalizer(0, player.audioSessionId)
                }
                val eq = equalizer ?: return@runCatching
                eq.enabled = true

                val levelRange = eq.bandLevelRange
                val minLevel = levelRange[0].toInt()
                val maxLevel = levelRange[1].toInt()
                val centerFreqList = (0 until eq.numberOfBands).map { bandIndex ->
                    eq.getCenterFreq(bandIndex.toShort()).toInt() / 1000
                }

                safeBands.forEachIndexed { index, gainDb ->
                    val targetHz = eqFrequencies[index]
                    val nearestBand = centerFreqList.indices.minByOrNull { i ->
                        kotlin.math.abs(centerFreqList[i] - targetHz)
                    } ?: return@forEachIndexed

                    val finalDb = (masterGainDb + gainDb).coerceIn(-12f, 12f)
                    val level = (finalDb * 100).toInt().coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(nearestBand.toShort(), level.toShort())
                }
            } else {
                equalizer?.enabled = false
            }
        }
    }

    fun applyChannelBalance(balance: Float) {
        channelBalanceProcessor.balance = balance.coerceIn(-1f, 1f)
    }

    fun applyPitchShift(semitones: Int) {
        val clamped = semitones.coerceIn(-12, 12)
        val pitch = 2.0.pow(clamped / 12.0).toFloat()
        val currentParams = player.playbackParameters
        val newParams = PlaybackParameters(
            /* speed = */ currentParams.speed,
            /* pitch = */ pitch
        )
        player.playbackParameters = newParams
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        player.release()
        // 不再主动释放downloadCache，由CacheManager统一管理
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getCacheSize(): Long {
        return CacheManager.getCacheSize(context)
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        CacheManager.clearCache(context)
    }

    /**
     * 直接播放本地音频文件，配合已缓存歌曲离线播放使用
     */
    fun playLocalFile(audioFile: File, song: SongDto) {
        try {
            val uri = Uri.fromFile(audioFile)
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            _playlist.value = listOf(song)
            _currentSong.value = song
        } catch (e: Exception) {
            android.util.Log.e("PlayerController", "播放本地文件失败: ${e.message}", e)
        }
    }
}
