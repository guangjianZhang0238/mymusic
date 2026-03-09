package com.music.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 后台播放服务，支持 MediaSession 和通知栏控制。
 * 使用 MediaSessionService 作为基类，以便与 Android Auto、Wear OS 等兼容。
 */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private lateinit var playerController: PlayerController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // 立即启动前台服务
        startForegroundService()
        
        playerController = PlayerController(applicationContext)
        mediaSession = MediaSession.Builder(this, playerController.player)
            .setCallback(MediaSessionCallback())
            .build()
        addSession(mediaSession)
    }

    override fun onDestroy() {
        mediaSession.release()
        playerController.release()
        // 不要在服务销毁时释放全局缓存，让CacheManager管理
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // 如果任务被移除（例如从最近任务中划掉），停止服务
        stopSelf()
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        // 可以在这里处理自定义命令，例如播放指定歌曲
        // 默认行为由 ExoPlayer 处理
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("音乐播放")
            .setContentText("正在播放音乐")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
    }
}