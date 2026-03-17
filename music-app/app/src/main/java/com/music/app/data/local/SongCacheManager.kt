package com.music.app.data.local

import android.content.Context
import com.music.app.data.remote.LyricLine
import com.music.app.data.remote.SongDto
import com.music.app.data.remote.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object SongCacheManager {

    private const val CACHE_DIR_NAME = "music_cache"
    private const val MAX_CACHED_SONGS = 50

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private fun cacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun buildAudioFile(context: Context, song: SongDto): File {
        val ext = song.filePath
            ?.substringAfterLast('.', missingDelimiterValue = "mp3")
            ?.takeIf { it.length <= 5 }
            ?: "mp3"
        val safeExt = ext.lowercase().ifBlank { "mp3" }
        return File(cacheDir(context), "song_${song.id}.$safeExt")
    }

    fun getAllCachedSongs(context: Context): kotlinx.coroutines.flow.Flow<List<CachedSongEntity>> {
        return AppDatabase.getInstance(context).cachedSongDao().getAllCachedSongs()
    }

    suspend fun getCachedSong(context: Context, songId: Long): CachedSongEntity? {
        return AppDatabase.getInstance(context).cachedSongDao().getCachedSong(songId)
    }

    suspend fun ensureSongCached(
        context: Context,
        song: SongDto,
        lyricsContent: String?,
        audioUrl: String
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.cachedSongDao()

        val targetFile = buildAudioFile(context, song)
        if (targetFile.exists()) {
            // 已有文件，仅更新元信息和歌词
            dao.upsert(
                CachedSongEntity(
                    songId = song.id,
                    songTitle = song.title,
                    artistName = song.artistNames ?: song.artistName,
                    albumName = song.albumName,
                    coverUrl = song.albumCover,
                    audioPath = targetFile.absolutePath,
                    lyricsContent = lyricsContent,
                    durationMs = null,
                    cachedAt = System.currentTimeMillis()
                )
            )
            return@withContext
        }

        // 数量控制：超过 50 首时删除最早的一首
        val count = dao.getCount()
        if (count >= MAX_CACHED_SONGS) {
            dao.getOldest()?.let { oldest ->
                runCatching {
                    File(oldest.audioPath).takeIf { it.exists() }?.delete()
                }
                dao.deleteBySongId(oldest.songId)
            }
        }

        // 下载音频文件
        runCatching {
            val builder = Request.Builder().url(audioUrl)
            val token = TokenStore.getToken(context)
            if (!token.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            val request = builder.build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body ?: return@use
                targetFile.outputStream().use { out ->
                    body.byteStream().use { input ->
                        input.copyTo(out)
                    }
                }
            }

            if (targetFile.exists()) {
                dao.upsert(
                    CachedSongEntity(
                        songId = song.id,
                        songTitle = song.title,
                        artistName = song.artistNames ?: song.artistName,
                        albumName = song.albumName,
                        coverUrl = song.albumCover,
                        audioPath = targetFile.absolutePath,
                        lyricsContent = lyricsContent,
                        durationMs = null,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteSongCache(context: Context, songId: Long) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).cachedSongDao()
        val entity = dao.getCachedSong(songId)
        if (entity != null) {
            runCatching {
                File(entity.audioPath).takeIf { it.exists() }?.delete()
            }
            dao.deleteBySongId(songId)
        }
    }

    suspend fun clearAllCachedSongs(context: Context) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).cachedSongDao()
        val all = dao.getAllCachedSongs()
        // 删除文件目录下的所有音频文件
        runCatching {
            cacheDir(context).deleteRecursively()
            cacheDir(context)
        }
        // 清空数据库记录
        dao.clearAll()
    }

    /**
     * 简单的 LRC 解析，供离线歌词展示使用
     */
    fun parseLyrics(content: String?): List<LyricLine> {
        if (content.isNullOrBlank()) return emptyList()
        val regex = Regex("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)$")
        return content
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .split("\n")
            .mapNotNull { line ->
                val match = regex.find(line.trim()) ?: return@mapNotNull null
                val min = match.groupValues[1].toIntOrNull() ?: 0
                val sec = match.groupValues[2].toIntOrNull() ?: 0
                val fractionRaw = match.groupValues[3]
                val fraction = if (fractionRaw.length == 2) {
                    fractionRaw.toIntOrNull()?.div(100f)
                } else {
                    fractionRaw.toIntOrNull()?.div(1000f)
                }
                LyricLine(min * 60 + sec + (fraction ?: 0f), match.groupValues[4].trim())
            }
            .sortedBy { it.timeSec }
    }
}

