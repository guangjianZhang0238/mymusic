package com.music.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.common.core.domain.PageResult;
import com.music.player.dto.PlayHistoryDTO;
import com.music.player.entity.PlayHistory;
import com.music.player.mapper.PlayHistoryMapper;
import com.music.player.service.PlayCountCacheService;
import com.music.player.service.PlayHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 播放历史服务实现
 */
@Service
public class PlayHistoryServiceImpl extends ServiceImpl<PlayHistoryMapper, PlayHistory> implements PlayHistoryService {

    @Resource
    private PlayHistoryMapper playHistoryMapper;

    @Resource
    private PlayCountCacheService playCountCacheService;

    @Override
    @Transactional
    public void addPlayHistory(PlayHistoryDTO dto) {
        PlayHistory playHistory = new PlayHistory();
        playHistory.setUserId(dto.getUserId());
        playHistory.setSongId(dto.getSongId());
        playHistory.setPlayDuration(dto.getDurationPlayed());
        save(playHistory);

        // 播放量写入 Redis 缓存（失败时自动降级直写数据库）
        playCountCacheService.incrementSongPlayCount(dto.getSongId());
    }

    @Override
    public PageResult<PlayHistoryDTO> pagePlayHistory(Long userId, Integer current, Integer size) {
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId);
        wrapper.orderByDesc(PlayHistory::getPlayTime);

        Page<PlayHistory> page = new Page<>(current, size);
        Page<PlayHistory> result = page(page, wrapper);

        List<PlayHistoryDTO> list = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public List<Long> getUserRecentPlays(Long userId, int limit) {
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId);
        wrapper.orderByDesc(PlayHistory::getPlayTime);
        wrapper.last("LIMIT " + limit);
        List<PlayHistory> histories = list(wrapper);
        return histories.stream().map(PlayHistory::getSongId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserTopPlayedSongs(Long userId, int limit) {
        // 这里需要自定义SQL来统计播放次数最多的歌曲
        return playHistoryMapper.selectTopPlayedSongs(userId, limit);
    }

    @Override
    @Transactional
    public void clearPlayHistory(Long userId) {
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId);
        remove(wrapper);
    }

    @Override
    @Transactional
    public void deletePlayHistory(Long id, Long userId) {
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getId, id);
        wrapper.eq(PlayHistory::getUserId, userId);
        remove(wrapper);
    }

    @Override
    public Long getUserPlayCount(Long userId) {
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId);
        return count(wrapper);
    }

    @Override
    public Integer getUserTodayPlayCount(Long userId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayHistory::getUserId, userId);
        wrapper.ge(PlayHistory::getPlayTime, today);
        return Math.toIntExact(count(wrapper));
    }

    /**
     * 转换为DTO
     */
    private PlayHistoryDTO convertToDTO(PlayHistory playHistory) {
        PlayHistoryDTO dto = new PlayHistoryDTO();
        dto.setId(playHistory.getId());
        dto.setUserId(playHistory.getUserId());
        dto.setSongId(playHistory.getSongId());
        dto.setPlayTime(playHistory.getPlayTime());
        dto.setDurationPlayed(playHistory.getPlayDuration());
        return dto;
    }
}
