package com.music.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.player.entity.SongComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SongCommentMapper extends BaseMapper<SongComment> {
}