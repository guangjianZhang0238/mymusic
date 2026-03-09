package com.music.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log

/**
 * 内存监控工具类
 * 用于监控应用内存使用情况，帮助诊断性能问题
 */
object MemoryMonitor {
    private const val TAG = "MemoryMonitor"
    
    /**
     * 获取当前内存使用情况
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        
        return MemoryInfo(
            availMem = memoryInfo.availMem,
            totalMem = memoryInfo.totalMem,
            threshold = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory,
            usedMemory = usedMemory,
            maxMemory = runtime.maxMemory(),
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize()
        )
    }
    
    /**
     * 打印内存使用情况到日志
     */
    fun logMemoryInfo(context: Context) {
        val info = getMemoryInfo(context)
        Log.d(TAG, """
            内存使用情况:
            - 可用内存: ${formatBytes(info.availMem)}
            - 总内存: ${formatBytes(info.totalMem)}
            - 阈值: ${formatBytes(info.threshold)}
            - 内存不足: ${info.lowMemory}
            - 已使用内存: ${formatBytes(info.usedMemory)}
            - 最大内存: ${formatBytes(info.maxMemory)}
            - Native堆大小: ${formatBytes(info.nativeHeapSize)}
            - Native堆已分配: ${formatBytes(info.nativeHeapAllocatedSize)}
        """.trimIndent())
    }
    
    /**
     * 检查是否接近内存限制
     */
    fun isNearMemoryLimit(context: Context, thresholdPercent: Double = 0.8): Boolean {
        val info = getMemoryInfo(context)
        val usedPercent = info.usedMemory.toDouble() / info.maxMemory.toDouble()
        return usedPercent > thresholdPercent
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val availMem: Long,
        val totalMem: Long,
        val threshold: Long,
        val lowMemory: Boolean,
        val usedMemory: Long,
        val maxMemory: Long,
        val nativeHeapSize: Long,
        val nativeHeapAllocatedSize: Long
    )
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}