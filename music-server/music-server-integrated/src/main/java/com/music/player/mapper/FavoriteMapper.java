package com.music.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.player.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
