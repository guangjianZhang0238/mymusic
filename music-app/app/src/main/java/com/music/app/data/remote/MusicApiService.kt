package com.music.app.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {
    @GET("api/app/music/song/page")
    suspend fun getSongPage(
        @Query("current") current: Int = 1,
        @Query("size") size: Int = 20,
        @Query("keyword") keyword: String? = null,
        @Query("albumId") albumId: Long? = null,
        @Query("artistId") artistId: Long? = null
    ): ApiResult<PageResult<SongDto>>

    @GET("api/app/music/song/hot")
    suspend fun getHotSongs(): ApiResult<List<SongDto>>

    @GET("api/app/music/album/page")
    suspend fun getAlbumPage(
        @Query("current") current: Int = 1,
        @Query("size") size: Int = 30,
        @Query("keyword") keyword: String? = null
    ): ApiResult<PageResult<AlbumDto>>

    @GET("api/app/music/artist/page")
    suspend fun getArtistPage(
        @Query("current") current: Int = 1,
        @Query("size") size: Int = 30,
        @Query("keyword") keyword: String? = null
    ): ApiResult<PageResult<ArtistDto>>

    @GET("api/app/music/lyrics/song/{songId}")
    suspend fun getLyricsBySongId(@Path("songId") songId: Long): ApiResult<LyricsDto>

    @GET("api/app/music/song/by-ids")
    suspend fun getSongsByIds(@Query("ids") ids: List<Long>): ApiResult<List<SongDto>>

    @GET("api/app/music/album/{albumId}")
    suspend fun getAlbumDetail(@Path("albumId") albumId: Long): ApiResult<AlbumDetailDto>

    @GET("api/app/music/artist/{artistId}")
    suspend fun getArtistDetail(@Path("artistId") artistId: Long): ApiResult<ArtistDetailDto>

    @GET("api/app/music/artist/{artistId}/top-songs")
    suspend fun getArtistTopSongs(
        @Path("artistId") artistId: Long,
        @Query("limit") limit: Int = 20
    ): ApiResult<List<SongDto>>
    
    @GET("api/app/music/artist/by-ids")
    suspend fun getArtistsByIds(@Query("ids") ids: List<Long>): ApiResult<List<ArtistDto>>

    // ==================== 播放历史 ====================

    @POST("api/app/music/player/history")
    suspend fun addPlayHistory(@Body dto: PlayHistoryRequest): ApiResult<Unit>

    @GET("api/app/music/player/history/recent")
    suspend fun getRecentPlays(@Query("limit") limit: Int = 20): ApiResult<List<Long>>

    @DELETE("api/app/music/player/history")
    suspend fun clearPlayHistory(): ApiResult<Unit>

    // ==================== 收藏 ====================

    @POST("api/app/music/player/favorite")
    suspend fun addFavorite(@Body dto: FavoriteRequest): ApiResult<Unit>

    @DELETE("api/app/music/player/favorite")
    suspend fun removeFavorite(
        @Query("favoriteType") favoriteType: Int,
        @Query("targetId") targetId: Long
    ): ApiResult<Unit>

    @GET("api/app/music/player/favorite/check")
    suspend fun checkFavorite(
        @Query("favoriteType") favoriteType: Int,
        @Query("targetId") targetId: Long
    ): ApiResult<Boolean>

    @GET("api/app/music/player/favorite/songs")
    suspend fun getFavoriteSongs(): ApiResult<List<Long>>

    @GET("api/app/music/player/favorite/albums")
    suspend fun getFavoriteAlbums(): ApiResult<List<Long>>

    @GET("api/app/music/player/favorite/artists")
    suspend fun getFavoriteArtists(): ApiResult<List<Long>>

    // ==================== 播放列表缓存 ====================

    @POST("api/app/music/player/playlist")
    suspend fun savePlaybackPlaylist(@Body dto: PlaybackPlaylistRequest): ApiResult<Unit>

    @GET("api/app/music/player/playlist")
    suspend fun getPlaybackPlaylist(): ApiResult<PlaybackPlaylistResponse>

    @DELETE("api/app/music/player/playlist")
    suspend fun clearPlaybackPlaylist(): ApiResult<Unit>

    // ==================== 认证 ====================

    @POST("api/app/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    @POST("api/app/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<Unit>

    @GET("api/app/auth/me")
    suspend fun getCurrentUser(): ApiResult<LoginResponse>

    @POST("api/app/auth/refresh")
    suspend fun refreshToken(@Header("Authorization") authorizationHeader: String): ApiResult<String>

    // ==================== 歌单 ====================

    @GET("api/app/music/playlist/user")
    suspend fun getUserPlaylists(): ApiResult<List<PlaylistDto>>

    @GET("api/app/music/playlist/public")
    suspend fun getPublicPlaylists(@Query("limit") limit: Int = 10): ApiResult<List<PlaylistDto>>

    @GET("api/app/music/playlist/{playlistId}")
    suspend fun getPlaylistDetail(@Path("playlistId") playlistId: Long): ApiResult<PlaylistDto>

    @POST("api/app/music/playlist")
    suspend fun createPlaylist(@Body request: PlaylistRequest): ApiResult<Long>

    @PUT("api/app/music/playlist")
    suspend fun updatePlaylist(@Body request: PlaylistRequest): ApiResult<Unit>

    @DELETE("api/app/music/playlist/{playlistId}")
    suspend fun deletePlaylist(@Path("playlistId") playlistId: Long): ApiResult<Unit>

    @GET("api/app/music/playlist/{playlistId}/songs")
    suspend fun getPlaylistSongs(@Path("playlistId") playlistId: Long): ApiResult<List<Long>>

    @POST("api/app/music/playlist/{playlistId}/song/{songId}")
    suspend fun addSongToPlaylist(
        @Path("playlistId") playlistId: Long,
        @Path("songId") songId: Long
    ): ApiResult<Unit>

    @DELETE("api/app/music/playlist/{playlistId}/song/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("playlistId") playlistId: Long,
        @Path("songId") songId: Long
    ): ApiResult<Unit>

    @POST("api/app/music/playlist/{playlistId}/play")
    suspend fun incrementPlayCount(@Path("playlistId") playlistId: Long): ApiResult<Unit>

    // ==================== 歌曲评论 ====================

    @POST("api/app/music/comment")
    suspend fun addComment(@Body request: SongCommentRequest): ApiResult<Long>

    @DELETE("api/app/music/comment/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): ApiResult<Unit>

    @GET("api/app/music/comment/song/{songId}")
    suspend fun getSongComments(
        @Path("songId") songId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResult<PageResult<SongCommentDto>>

    @POST("api/app/music/comment/{commentId}/like")
    suspend fun likeComment(@Path("commentId") commentId: Long): ApiResult<Unit>

    @DELETE("api/app/music/comment/{commentId}/like")
    suspend fun unlikeComment(@Path("commentId") commentId: Long): ApiResult<Unit>

    // ==================== 歌词分享 ====================

    @POST("api/app/music/lyrics/share")
    suspend fun createLyricsShare(@Body request: LyricsShareRequest): ApiResult<Long>

    @DELETE("api/app/music/lyrics/share/{shareId}")
    suspend fun deleteLyricsShare(@Path("shareId") shareId: Long): ApiResult<Unit>

    @GET("api/app/music/lyrics/share/user")
    suspend fun getUserLyricsShares(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResult<PageResult<LyricsShareDto>>

    @GET("api/app/music/lyrics/share/lyrics/{lyricsId}")
    suspend fun getLyricsShares(
        @Path("lyricsId") lyricsId: Long,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResult<PageResult<LyricsShareDto>>

    @GET("api/app/music/lyrics/share/{shareId}")
    suspend fun getLyricsShareDetail(@Path("shareId") shareId: Long): ApiResult<LyricsShareDto>

    // ==================== 反馈 ====================

    @POST("api/app/music/feedback")
    suspend fun createFeedback(@Body request: FeedbackRequest): ApiResult<Long>

    @GET("api/app/music/feedback/mine")
    suspend fun getMyFeedbacks(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResult<PageResult<FeedbackDto>>
    
    // ==================== 搜索联想 ====================
    
    @GET("api/app/music/search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int = 10
    ): ApiResult<List<SearchSuggestionDto>>
    
    // ==================== 用户设置 ====================
    
    @GET("api/app/user-setting/{settingKey}")
    suspend fun getUserSetting(@Path("settingKey") settingKey: String): ApiResult<UserSettingDto>
    
    @GET("api/app/user-setting")
    suspend fun getUserSettings(): ApiResult<List<UserSettingDto>>
    
    @POST("api/app/user-setting")
    suspend fun saveUserSetting(
        @Query("settingKey") settingKey: String,
        @Query("settingValue") settingValue: String,
        @Query("settingType") settingType: String? = null,
        @Query("description") description: String? = null
    ): ApiResult<UserSettingDto>
    
    @DELETE("api/app/user-setting/{settingKey}")
    suspend fun deleteUserSetting(@Path("settingKey") settingKey: String): ApiResult<Unit>
}
