package com.music.app.data.remote

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResult<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class PageResult<T>(
    @SerialName("records") val records: List<T> = emptyList(),
    @SerialName("total") val total: Long = 0
)

@Serializable
data class SongDto(
    val id: Long,
    val title: String = "",
    val titleEn: String? = null,
    val artistName: String? = null,
    val artistNames: String? = null,
    val filePath: String? = null,
    val albumName: String? = null,
    val albumCover: String? = null,
    val hasLyrics: Int = 0,
    val playCount: Int? = null
)

@Serializable
data class AlbumDto(
    val id: Long,
    val name: String = "",
    val nameEn: String? = null,
    val artistName: String? = null,
    val coverImage: String? = null,
    val songCount: Int? = 0
)

@Serializable
data class AlbumDetailDto(
    val id: Long,
    val name: String = "",
    val nameEn: String? = null,
    val artistName: String? = null,
    val coverImage: String? = null,
    val songCount: Int? = 0,
    val releaseDate: String? = null,
    val description: String? = null
)

@Serializable
data class ArtistDto(
    val id: Long,
    val name: String = "",
    val nameEn: String? = null,
    val songCount: Int? = 0,
    val albumCount: Int? = 0
)

@Serializable
data class ArtistDetailDto(
    val id: Long,
    val name: String = "",
    val nameEn: String? = null,
    val avatar: String? = null,
    val description: String? = null,
    val region: String? = null,
    val songCount: Int? = 0,
    val albumCount: Int? = 0
)

@Serializable
data class LyricsDto(
    val id: Long? = null,
    val songId: Long,
    val content: String = "",
    val lyricsOffset: Double? = 0.0
)

data class LyricLine(
    val timeSec: Float,
    val text: String
)

// ==================== 播放历史 ====================

@Serializable
data class PlayHistoryRequest(
    val songId: Long,
    val songTitle: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val coverImage: String? = null,
    val duration: Int? = null,
    val durationPlayed: Int? = null
)

// ==================== 收藏 ====================

@Serializable
data class FavoriteRequest(
    val favoriteType: Int,  // 1-歌曲，2-专辑，3-歌手，4-播放列表
    val targetId: Long,
    val targetName: String? = null,
    val targetCover: String? = null
)

@Serializable
data class PlaybackPlaylistRequest(
    val songIds: List<Long>,
    val currentIndex: Int
)

@Serializable
data class PlaybackPlaylistResponse(
    val songIds: List<Long> = emptyList(),
    val currentIndex: Int = 0
)

// ==================== 认证 ====================

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val userInfo: UserInfo? = null
)

@Serializable
data class UserInfo(
    val id: Long? = null,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val role: Int? = 0,
    val status: Int? = 1,
    val createTime: String? = null,
    val lastLoginTime: String? = null
)

// ==================== 歌单 ====================

@Serializable
data class PlaylistDto(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String = "",
    val coverImage: String? = null,
    val description: String? = null,
    val isPublic: Int? = 1,
    val songCount: Int? = 0,
    val playCount: Int? = 0,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class PlaylistRequest(
    val id: Long? = null,
    val name: String,
    val coverImage: String? = null,
    val description: String? = null,
    val isPublic: Int? = 1
)

// ==================== 歌曲评论 ====================

@Serializable
data class SongCommentDto(
    val id: Long? = null,
    val songId: Long,
    val userId: Long,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val content: String = "",
    val likeCount: Int = 0,
    val createTime: String? = null,
    val updateTime: String? = null
)

@Serializable
data class SongCommentRequest(
    val songId: Long,
    val content: String
)

// ==================== 歌词分享 ====================

@Serializable
data class LyricsShareDto(
    val id: Long? = null,
    val lyricsId: Long,
    val userId: Long,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val shareType: String = "text",
    val createTime: String? = null
)

@Serializable
data class LyricsShareRequest(
    val lyricsId: Long,
    val shareType: String = "text"
)

// ==================== 反馈 ====================

@Serializable
data class FeedbackRequest(
    val type: String,
    val content: String,
    val songId: Long? = null,
    val keyword: String? = null,
    val contact: String? = null,
    val scene: String? = null
)

@Serializable
data class FeedbackDto(
    val id: Long? = null,
    val userId: Long? = null,
    val type: String = "",
    val content: String = "",
    val songId: Long? = null,
    val keyword: String? = null,
    val contact: String? = null,
    val scene: String? = null,
    val status: String? = null,
    val handleNote: String? = null,
    val handleTime: String? = null,
    val createTime: String? = null
)

// ==================== 搜索联想 ====================

@Serializable
data class SearchSuggestionDto(
    val type: Int = 0,  // 1-歌曲，2-歌手，3-专辑
    val id: Long = 0,
    val name: String = "",
    val artistName: String? = null,
    val albumName: String? = null,
    val coverImage: String? = null,
    val matchedKeyword: String? = null
)

// ==================== 反馈结果 ====================

sealed class FeedbackResult {
    object Loading : FeedbackResult()
    data class Success(val feedbackId: Long) : FeedbackResult()
    data class Error(val message: String) : FeedbackResult()
}

// ==================== 用户设置 ====================

@Serializable
data class UserSettingDto(
    val id: Long? = null,
    val userId: Long? = null,
    val settingKey: String = "",
    val settingValue: String = "",
    val settingType: String? = null,
    val description: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

// ==================== 用户界面设置 ====================

@Serializable
data class EqualizerSettings(
    val enabled: Boolean = false,
    val preset: String = "OFF", // OFF / PERFECT_BASS / ROCK / VOCAL / CUSTOM
    val masterGainDb: Float = 0f,
    val bandGainsDb: List<Float> = List(10) { 0f },
    val stereoBalance: Float = 0f,  // -1f=纯左声道, 0f=居中, 1f=纯右声道
    val pitchShiftSemitones: Int = 0 // 以半音为单位的升降调，范围建议 [-12, 12]
)

@Serializable
data class ThemeSettings(
    val primaryColor: String = "#FFE53935", // 默认红色
    val isDarkMode: Boolean = true
)

data class UserSettings(
    val backgroundColor: Color = Color(0xFF111111),
    val themeColor: Color = Color(0xFFE53935), // 主题色
    val isDarkMode: Boolean = true, // 是否深色模式
    val equalizer: EqualizerSettings = EqualizerSettings()
)
