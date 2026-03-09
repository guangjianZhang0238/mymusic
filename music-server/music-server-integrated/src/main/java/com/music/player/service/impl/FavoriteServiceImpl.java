package com.music.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.player.entity.Favorite;
import com.music.player.mapper.FavoriteMapper;
import com.music.player.service.FavoriteService;
import com.music.player.dto.FavoriteDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Resource
    private FavoriteMapper favoriteMapper;

    @Override
    @Transactional
    public void addFavorite(FavoriteDTO dto) {
        Favorite favorite = new Favorite();
        favorite.setUserId(dto.getUserId());
        favorite.setFavoriteType(dto.getFavoriteType());
        favorite.setTargetId(dto.getTargetId());
        save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Integer favoriteType, Long targetId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getFavoriteType, favoriteType);
        wrapper.eq(Favorite::getTargetId, targetId);
        remove(wrapper);
    }

    @Override
    public boolean checkFavorite(Long userId, Integer favoriteType, Long targetId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getFavoriteType, favoriteType);
        wrapper.eq(Favorite::getTargetId, targetId);
        return count(wrapper) > 0;
    }

    @Override
    public List<FavoriteDTO> getUserFavorites(Long userId, Integer favoriteType) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        if (favoriteType != null) {
            wrapper.eq(Favorite::getFavoriteType, favoriteType);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);
        List<Favorite> favorites = list(wrapper);
        return favorites.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserFavoriteSongs(Long userId) {
        return getFavoriteTargets(userId, 1);
    }

    @Override
    public List<Long> getUserFavoriteAlbums(Long userId) {
        return getFavoriteTargets(userId, 2);
    }

    @Override
    public List<Long> getUserFavoriteArtists(Long userId) {
        return getFavoriteTargets(userId, 3);
    }

    @Override
    public List<Long> getUserFavoritePlaylists(Long userId) {
        return getFavoriteTargets(userId, 4);
    }

    /**
     * 获取用户收藏的目标ID列表
     */
    private List<Long> getFavoriteTargets(Long userId, Integer favoriteType) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getFavoriteType, favoriteType);
        List<Favorite> favorites = list(wrapper);
        return favorites.stream().map(Favorite::getTargetId).collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private FavoriteDTO convertToDTO(Favorite favorite) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setUserId(favorite.getUserId());
        dto.setFavoriteType(favorite.getFavoriteType());
        dto.setTargetId(favorite.getTargetId());
        dto.setCreateTime(favorite.getCreateTime());
        return dto;
    }
}
