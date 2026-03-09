package com.music.app.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.security.MessageDigest

/**
 * 缓存管理器，确保整个应用只有一个SimpleCache实例
 * 避免多个PlayerController实例同时访问同一缓存目录导致的冲突
 */
@OptIn(UnstableApi::class)
object CacheManager {
    private var simpleCache: SimpleCache? = null
    private const val CACHE_DIR_NAME = "music_cache"
    private const val MAX_CACHE_SIZE = 500L * 1024 * 1024 // 500MB
    
    /**
     * 获取全局唯一的SimpleCache实例
     */
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            simpleCache = SimpleCache(cacheDir, cacheEvictor)
        }
        return simpleCache!!
    }
    
    /**
     * 释放缓存资源
     */
    fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }
    
    /**
     * 清理缓存目录
     */
    fun clearCache(context: Context) {
        try {
            releaseCache()
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            cacheDir.mkdirs()
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "清理缓存失败", e)
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            getDirectorySize(cacheDir)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 根据歌曲ID生成缓存键
     */
    fun generateCacheKey(songId: Long): String {
        return "song_$songId"
    }
    
    /**
     * 根据歌曲文件路径生成缓存键
     */
    fun generateCacheKeyFromPath(filePath: String): String {
        return MessageDigest.getInstance("MD5").digest(filePath.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 检查指定歌曲是否已在缓存中
     */
    fun isSongCached(context: Context, songId: Long): Boolean {
        return try {
            val cache = getCache(context)
            val cacheKey = generateCacheKey(songId)
            cache.getCachedSpans(cacheKey).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查指定文件路径是否已在缓存中
     */
    fun isPathCached(context: Context, filePath: String): Boolean {
        return try {
            val cache = getCache(context)
            val cacheKey = generateCacheKeyFromPath(filePath)
            cache.getCachedSpans(cacheKey).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取缓存中的歌曲信息
     */
    fun getCachedSongInfo(context: Context, songId: Long): CachedSongInfo? {
        return try {
            val cache = getCache(context)
            val cacheKey = generateCacheKey(songId)
            val spans = cache.getCachedSpans(cacheKey)
            
            if (spans.isNotEmpty()) {
                val totalSize = spans.sumOf { it.length }
                CachedSongInfo(
                    songId = songId,
                    cacheKey = cacheKey,
                    cachedSize = totalSize,
                    isFullyCached = spans.any { it.length > 0 }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}

/**
 * 缓存的歌曲信息数据类
 */
data class CachedSongInfo(
    val songId: Long,
    val cacheKey: String,
    val cachedSize: Long,
    val isFullyCached: Boolean
)