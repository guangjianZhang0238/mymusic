package com.music.player.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.player.entity.Favorite;
import com.music.player.dto.FavoriteDTO;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏
     */
    void addFavorite(FavoriteDTO dto);

    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Integer favoriteType, Long targetId);

    /**
     * 检查是否已收藏
     */
    boolean checkFavorite(Long userId, Integer favoriteType, Long targetId);

    /**
     * 获取用户的收藏列表
     */
    List<FavoriteDTO> getUserFavorites(Long userId, Integer favoriteType);

    /**
     * 获取用户收藏的歌曲
     */
    List<Long> getUserFavoriteSongs(Long userId);

    /**
     * 获取用户收藏的专辑
     */
    List<Long> getUserFavoriteAlbums(Long userId);

    /**
     * 获取用户收藏的歌手
     */
    List<Long> getUserFavoriteArtists(Long userId);

    /**
     * 获取用户收藏的播放列表
     */
    List<Long> getUserFavoritePlaylists(Long userId);
}
