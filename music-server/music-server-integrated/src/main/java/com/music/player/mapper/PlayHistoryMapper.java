package com.music.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.player.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 播放历史Mapper
 */
@Mapper
public interface PlayHistoryMapper extends BaseMapper<PlayHistory> {

    /**
     * 获取用户播放次数最多的歌曲
     */
    @Select("SELECT song_id FROM music_play_history WHERE user_id = #{userId} GROUP BY song_id ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<Long> selectTopPlayedSongs(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取用户今日播放次数
     */
    @Select("SELECT COUNT(*) FROM music_play_history WHERE user_id = #{userId} AND DATE(play_time) = CURDATE()")
    Integer selectTodayPlayCount(@Param("userId") Long userId);

    /**
     * 获取用户指定歌曲的播放次数
     */
    @Select("SELECT COUNT(*) FROM music_play_history WHERE user_id = #{userId} AND song_id = #{songId}")
    Integer selectSongPlayCount(@Param("userId") Long userId, @Param("songId") Long songId);

    /**
     * 清空用户的播放历史
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和歌曲ID删除播放历史
     */
    void deleteByUserIdAndSongId(@Param("userId") Long userId, @Param("songId") Long songId);
}
