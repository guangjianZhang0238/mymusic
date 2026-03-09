package com.music.app.player

import android.content.Context
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * 缓存管理器，确保整个应用只有一个SimpleCache实例
 * 避免多个PlayerController实例同时访问同一缓存目录导致的冲突
 */
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