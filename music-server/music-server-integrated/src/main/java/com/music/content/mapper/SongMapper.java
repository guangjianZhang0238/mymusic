package com.music.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.content.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SongMapper extends BaseMapper<Song> {

    /**
     * 统计歌曲总播放量（数据库聚合，避免全量加载歌曲记录）
     */
    @Select("SELECT COALESCE(SUM(play_count), 0) FROM content_song")
    Long selectTotalPlayCount();

    /**
     * 原子增加歌曲播放量
     */
    @Update("UPDATE content_song SET play_count = COALESCE(play_count, 0) + #{delta} WHERE id = #{songId}")
    int incrementPlayCountById(@Param("songId") Long songId, @Param("delta") long delta);
}
