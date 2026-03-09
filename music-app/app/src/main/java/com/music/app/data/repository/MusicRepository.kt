package com.music.app.data.repository

import com.music.app.data.remote.AlbumDetailDto
import com.music.app.data.remote.AlbumDto
import com.music.app.data.remote.ArtistDetailDto
import com.music.app.data.remote.ArtistDto
import com.music.app.data.remote.FavoriteRequest
import com.music.app.data.remote.FeedbackDto
import com.music.app.data.remote.FeedbackRequest
import com.music.app.data.remote.FeedbackResult
import com.music.app.data.remote.LyricLine
import com.music.app.data.remote.LyricsDto
import com.music.app.data.remote.LyricsShareDto
import com.music.app.data.remote.LyricsShareRequest
import com.music.app.data.remote.LoginRequest
import com.music.app.data.remote.LoginResponse
import com.music.app.data.remote.NetworkModule
import com.music.app.data.remote.PageResult
import com.music.app.data.remote.PlayHistoryRequest
import com.music.app.data.remote.PlaybackPlaylistRequest
import com.music.app.data.remote.PlaylistDto
import com.music.app.data.remote.PlaylistRequest
import com.music.app.data.remote.RegisterRequest
import com.music.app.data.remote.SearchSuggestionDto
import com.music.app.data.remote.SongCommentDto
import android.util.Log
import com.music.app.data.remote.SongCommentRequest
import com.music.app.data.remote.SongDto
import com.music.app.data.remote.UserSettingDto

class MusicRepository {
    private val api = NetworkModule.musicApiService

    suspend fun fetchHotSongs(): List<SongDto> = try {
        Log.d("MusicRepository", "开始获取热门歌曲")
        val response = api.getHotSongs()
        Log.d("MusicRepository", "API响应码: ${response.code}, 数据大小: ${response.data?.size ?: 0}")
        val result = response.data ?: emptyList()
        Log.d("MusicRepository", "最终返回歌曲数量: ${result.size}")
        result
    } catch (e: Exception) {
        Log.e("MusicRepository", "获取热门歌曲失败", e)
        emptyList()
    }

    suspend fun searchSongs(keyword: String): List<SongDto> = try {
        api.getSongPage(current = 1, size = 50, keyword = keyword).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchAlbums(): List<AlbumDto> = try {
        api.getAlbumPage(current = 1, size = 50).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchArtists(): List<ArtistDto> = try {
        api.getArtistPage(current = 1, size = 50).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchSongsByAlbum(albumId: Long): List<SongDto> = try {
        api.getSongPage(current = 1, size = 100, albumId = albumId).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchAlbumDetail(albumId: Long): AlbumDetailDto? = try {
        api.getAlbumDetail(albumId).data
    } catch (_: Exception) {
        null
    }

    suspend fun fetchArtistDetail(artistId: Long): ArtistDetailDto? = try {
        api.getArtistDetail(artistId).data
    } catch (_: Exception) {
        null
    }

    suspend fun fetchAlbumsByArtist(artistId: Long): List<AlbumDto> = try {
        api.getAlbumPage(current = 1, size = 50).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchSongsByArtist(artistId: Long): List<SongDto> = try {
        api.getSongPage(current = 1, size = 50, artistId = artistId).data?.records ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchArtistTopSongs(artistId: Long, limit: Int = 20): List<SongDto> = try {
        api.getArtistTopSongs(artistId, limit).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchLyrics(songId: Long): List<LyricLine> = try {
        val content = api.getLyricsBySongId(songId).data?.content ?: return emptyList()
        parseLrc(content)
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchLyricsWithOffset(songId: Long): Pair<List<LyricLine>, Float> = try {
        val data = api.getLyricsBySongId(songId).data ?: return Pair(emptyList(), 0f)
        Pair(parseLrc(data.content), data.lyricsOffset?.toFloat() ?: 0f)
    } catch (_: Exception) {
        Pair(emptyList(), 0f)
    }

    suspend fun fetchLyricsMeta(songId: Long): LyricsDto? = try {
        api.getLyricsBySongId(songId).data
    } catch (_: Exception) {
        null
    }

    suspend fun fetchSongsByIds(ids: List<Long>): List<SongDto> {
        if (ids.isEmpty()) return emptyList()
        return try {
            api.getSongsByIds(ids).data ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseLrc(content: String): List<LyricLine> {
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

    // ==================== 播放历史 ====================

    suspend fun addPlayHistory(song: SongDto, durationPlayed: Int? = null) {
        try {
            api.addPlayHistory(
                PlayHistoryRequest(
                    songId = song.id,
                    songTitle = song.title,
                    artistName = song.artistName,
                    albumName = song.albumName,
                    coverImage = song.albumCover,
                    durationPlayed = durationPlayed
                )
            )
        } catch (_: Exception) {
        }
    }

    suspend fun fetchRecentPlayIds(limit: Int = 20): List<Long> = try {
        api.getRecentPlays(limit).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun clearPlayHistory(): Boolean = try {
        api.clearPlayHistory()
        true
    } catch (_: Exception) {
        false
    }

    // ==================== 收藏 ====================

    suspend fun addFavorite(type: Int, targetId: Long, targetName: String? = null, targetCover: String? = null): Boolean = try {
        api.addFavorite(FavoriteRequest(type, targetId, targetName, targetCover))
        true
    } catch (_: Exception) {
        false
    }

    suspend fun removeFavorite(type: Int, targetId: Long): Boolean = try {
        api.removeFavorite(type, targetId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun checkFavorite(type: Int, targetId: Long): Boolean = try {
        api.checkFavorite(type, targetId).data ?: false
    } catch (_: Exception) {
        false
    }

    suspend fun fetchFavoriteSongIds(): List<Long> = try {
        api.getFavoriteSongs().data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchFavoriteAlbumIds(): List<Long> = try {
        api.getFavoriteAlbums().data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchFavoriteArtistIds(): List<Long> = try {
        api.getFavoriteArtists().data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    
    suspend fun fetchArtistsByIds(artistIds: List<Long>): List<ArtistDto> = try {
        api.getArtistsByIds(artistIds).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    
    // ==================== 歌手关注 ====================
    
    suspend fun followArtist(artistId: Long, artistName: String? = null): Boolean = try {
        addFavorite(3, artistId, artistName, null) // favoriteType 3 表示关注歌手
    } catch (_: Exception) {
        false
    }
    
    suspend fun unfollowArtist(artistId: Long): Boolean = try {
        removeFavorite(3, artistId) // favoriteType 3 表示关注歌手
    } catch (_: Exception) {
        false
    }
    
    suspend fun checkArtistFollowed(artistId: Long): Boolean = try {
        checkFavorite(3, artistId) // favoriteType 3 表示关注歌手
    } catch (_: Exception) {
        false
    }
    
    // ==================== 用户设置 ====================
    
    suspend fun getUserSetting(settingKey: String): UserSettingDto? = try {
        api.getUserSetting(settingKey).data
    } catch (_: Exception) {
        null
    }
    
    suspend fun getUserSettings(): List<UserSettingDto> = try {
        api.getUserSettings().data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    
    suspend fun saveUserSetting(settingKey: String, settingValue: String, settingType: String? = null, description: String? = null): UserSettingDto? = try {
        api.saveUserSetting(settingKey, settingValue, settingType, description).data
    } catch (_: Exception) {
        null
    }
    
    suspend fun deleteUserSetting(settingKey: String): Boolean = try {
        api.deleteUserSetting(settingKey)
        true
    } catch (_: Exception) {
        false
    }

    // ==================== 认证 ====================

    suspend fun login(username: String, password: String): LoginResponse? = try {
        val response = api.login(LoginRequest(username, password))
        if (response.code == 200) {
            response.data
        } else {
            // 登录失败，抛出异常，包含后端返回的错误信息
            throw Exception(response.message ?: "登录失败")
        }
    } catch (e: Exception) {
        Log.e("MusicRepository", "登录失败: ${e.message}")
        throw e
    }

    suspend fun register(
        username: String,
        password: String
    ): Boolean = try {
        Log.d("MusicRepository", "开始注册，用户名: $username")
        Log.d("MusicRepository", "发送请求到: ${NetworkModule.BASE_URL}api/app/auth/register")
        val request = RegisterRequest(username, password, null, null, null)
        Log.d("MusicRepository", "请求体: $request")
        val response = api.register(request)
        Log.d("MusicRepository", "API响应: code=${response.code}, message=${response.message}")
        true
    } catch (e: Exception) {
        Log.e("MusicRepository", "注册失败", e)
        false
    }

    suspend fun getCurrentUser(): LoginResponse? = try {
        api.getCurrentUser().data
    } catch (_: Exception) {
        null
    }

    suspend fun refreshToken(token: String): String? = try {
        api.refreshToken(token).data
    } catch (_: Exception) {
        null
    }

    // ==================== 歌单 ====================

    suspend fun getUserPlaylists(): List<PlaylistDto> = try {
        api.getUserPlaylists().data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getPublicPlaylists(limit: Int = 10): List<PlaylistDto> = try {
        api.getPublicPlaylists(limit).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getPlaylistDetail(playlistId: Long): PlaylistDto? = try {
        api.getPlaylistDetail(playlistId).data
    } catch (_: Exception) {
        null
    }

    suspend fun createPlaylist(request: PlaylistRequest): Long? = try {
        api.createPlaylist(request).data
    } catch (_: Exception) {
        null
    }

    suspend fun updatePlaylist(request: PlaylistRequest): Boolean = try {
        api.updatePlaylist(request)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun deletePlaylist(playlistId: Long): Boolean = try {
        api.deletePlaylist(playlistId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<Long> = try {
        api.getPlaylistSongs(playlistId).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Boolean = try {
        api.addSongToPlaylist(playlistId, songId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Boolean = try {
        api.removeSongFromPlaylist(playlistId, songId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun incrementPlayCount(playlistId: Long): Boolean = try {
        api.incrementPlayCount(playlistId)
        true
    } catch (_: Exception) {
        false
    }

    // ==================== 歌曲评论 ====================

    suspend fun addComment(songId: Long, content: String): Long? = try {
        api.addComment(SongCommentRequest(songId, content)).data
    } catch (_: Exception) {
        null
    }

    suspend fun deleteComment(commentId: Long): Boolean = try {
        api.deleteComment(commentId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun getSongComments(songId: Long, page: Int = 1, size: Int = 20): PageResult<SongCommentDto>? = try {
        api.getSongComments(songId, page, size).data
    } catch (_: Exception) {
        null
    }

    suspend fun likeComment(commentId: Long): Boolean = try {
        api.likeComment(commentId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun unlikeComment(commentId: Long): Boolean = try {
        api.unlikeComment(commentId)
        true
    } catch (_: Exception) {
        false
    }

    // ==================== 歌词分享 ====================

    suspend fun createLyricsShare(lyricsId: Long, shareType: String = "text"): Long? = try {
        api.createLyricsShare(LyricsShareRequest(lyricsId, shareType)).data
    } catch (_: Exception) {
        null
    }

    suspend fun deleteLyricsShare(shareId: Long): Boolean = try {
        api.deleteLyricsShare(shareId)
        true
    } catch (_: Exception) {
        false
    }

    suspend fun getUserLyricsShares(page: Int = 1, size: Int = 20): PageResult<LyricsShareDto>? = try {
        api.getUserLyricsShares(page, size).data
    } catch (_: Exception) {
        null
    }

    suspend fun getLyricsShares(lyricsId: Long, page: Int = 1, size: Int = 20): PageResult<LyricsShareDto>? = try {
        api.getLyricsShares(lyricsId, page, size).data
    } catch (_: Exception) {
        null
    }

    suspend fun getLyricsShareDetail(shareId: Long): LyricsShareDto? = try {
        api.getLyricsShareDetail(shareId).data
    } catch (_: Exception) {
        null
    }

    // ==================== 反馈 ====================

    /**
     * 创建反馈，返回Result&lt;Long?&gt;以支持错误信息传递
     */
    suspend fun createFeedback(
        type: String,
        content: String,
        songId: Long? = null,
        keyword: String? = null,
        contact: String? = null,
        scene: String? = null
    ): kotlinx.coroutines.flow.MutableStateFlow<FeedbackResult> {
        val result = kotlinx.coroutines.flow.MutableStateFlow<FeedbackResult>(FeedbackResult.Loading)
        try {
            val response = api.createFeedback(
                FeedbackRequest(
                    type = type,
                    content = content,
                    songId = songId,
                    keyword = keyword,
                    contact = contact,
                    scene = scene
                )
            )
            if (response.code == 200 && response.data != null) {
                result.value = FeedbackResult.Success(response.data)
            } else {
                result.value = FeedbackResult.Error(response.message ?: "提交失败")
            }
        } catch (e: Exception) {
            result.value = FeedbackResult.Error(e.message ?: "网络错误")
        }
        return result
    }
    
    /**
     * 简单版本的createFeedback，只返回ID
     */
    suspend fun createFeedbackSimple(
        type: String,
        content: String,
        songId: Long? = null,
        keyword: String? = null,
        contact: String? = null,
        scene: String? = null
    ): Long? = try {
        api.createFeedback(
            FeedbackRequest(
                type = type,
                content = content,
                songId = songId,
                keyword = keyword,
                contact = contact,
                scene = scene
            )
        ).data
    } catch (_: Exception) {
        null
    }

    suspend fun getMyFeedbacks(page: Int = 1, size: Int = 20): PageResult<FeedbackDto>? = try {
        api.getMyFeedbacks(page, size).data
    } catch (_: Exception) {
        null
    }
    
    // ==================== 搜索联想 ====================
    
    suspend fun getSearchSuggestions(keyword: String, limit: Int = 10): List<SearchSuggestionDto> = try {
        api.getSearchSuggestions(keyword, limit).data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    
    // ==================== 播放列表缓存 ====================
    
    suspend fun savePlaybackPlaylist(songIds: List<Long>, currentIndex: Int): Boolean = try {
        api.savePlaybackPlaylist(PlaybackPlaylistRequest(songIds, currentIndex))
        true
    } catch (_: Exception) {
        false
    }
    
    suspend fun getPlaybackPlaylist(): Pair<List<Long>, Int> = try {
        val response = api.getPlaybackPlaylist()
        Pair(response.data?.songIds ?: emptyList(), response.data?.currentIndex ?: 0)
    } catch (_: Exception) {
        Pair(emptyList(), 0)
    }
    
    suspend fun clearPlaybackPlaylist(): Boolean = try {
        api.clearPlaybackPlaylist()
        true
    } catch (_: Exception) {
        false
    }
}
