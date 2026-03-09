package com.music.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.player.entity.PlaylistSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 播放列表歌曲关联Mapper
 */
@Mapper
public interface PlaylistSongMapper extends BaseMapper<PlaylistSong> {

    /**
     * 获取播放列表中最大的排序值
     */
    @Select("SELECT MAX(sort_order) FROM music_playlist_song WHERE playlist_id = #{playlistId}")
    Integer selectMaxSort(@Param("playlistId") Long playlistId);

    /**
     * 根据播放列表ID删除所有歌曲
     */
    void deleteByPlaylistId(@Param("playlistId") Long playlistId);

    /**
     * 根据播放列表ID和歌曲ID删除
     */
    void deleteByPlaylistIdAndSongId(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

    /**
     * 获取播放列表中的歌曲数量
     */
    @Select("SELECT COUNT(*) FROM music_playlist_song WHERE playlist_id = #{playlistId}")
    Integer selectCountByPlaylistId(@Param("playlistId") Long playlistId);
}
