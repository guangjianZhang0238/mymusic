package com.music.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_songs")
data class CachedSongEntity(
    @PrimaryKey
    val songId: Long,
    val songTitle: String,
    val artistName: String?,
    val albumName: String?,
    val coverUrl: String?,
    /**
     * 本地音频文件绝对路径（位于 app 专用缓存目录）
     */
    val audioPath: String,
    /**
     * 原始 LRC 或纯文本歌词内容
     */
    val lyricsContent: String?,
    /**
     * 歌曲时长（毫秒），可选
     */
    val durationMs: Long?,
    /**
     * 缓存时间戳（毫秒）
     */
    val cachedAt: Long
)

