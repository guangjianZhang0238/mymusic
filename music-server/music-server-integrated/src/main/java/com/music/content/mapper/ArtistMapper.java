package com.music.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.content.entity.Artist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArtistMapper extends BaseMapper<Artist> {
}
