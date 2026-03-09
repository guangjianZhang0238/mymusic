package com.music.player.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.player.entity.PlayHistory;
import com.music.player.dto.PlayHistoryDTO;
import com.music.common.core.domain.PageResult;

import java.util.List;

/**
 * 播放历史服务接口
 */
public interface PlayHistoryService extends IService<PlayHistory> {

    /**
     * 添加播放历史
     */
    void addPlayHistory(PlayHistoryDTO dto);

    /**
     * 分页查询播放历史
     */
    PageResult<PlayHistoryDTO> pagePlayHistory(Long userId, Integer current, Integer size);

    /**
     * 获取用户最近播放的歌曲
     */
    List<Long> getUserRecentPlays(Long userId, int limit);

    /**
     * 获取用户播放次数最多的歌曲
     */
    List<Long> getUserTopPlayedSongs(Long userId, int limit);

    /**
     * 清空用户的播放历史
     */
    void clearPlayHistory(Long userId);

    /**
     * 删除指定的播放历史
     */
    void deletePlayHistory(Long id, Long userId);

    /**
     * 获取用户的播放统计
     */
    Long getUserPlayCount(Long userId);

    /**
     * 获取用户今日播放次数
     */
    Integer getUserTodayPlayCount(Long userId);
}
