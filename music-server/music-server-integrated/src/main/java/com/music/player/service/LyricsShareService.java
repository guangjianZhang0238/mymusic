package com.music.player.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.player.entity.LyricsShare;
import com.music.player.dto.LyricsShareDTO;
import com.music.common.core.domain.PageResult;



/**
 * 歌词分享服务接口
 */
public interface LyricsShareService extends IService<LyricsShare> {

    /**
     * 创建歌词分享
     */
    Long createShare(LyricsShareDTO dto);

    /**
     * 删除分享（仅限分享者或管理员）
     */
    void deleteShare(Long shareId, Long userId);

    /**
     * 获取用户的歌词分享列表（分页）
     */
    PageResult<LyricsShareDTO> getUserShares(Long userId, int page, int size);

    /**
     * 获取歌词的分享列表（分页）
     */
    PageResult<LyricsShareDTO> getLyricsShares(Long lyricsId, int page, int size);

    /**
     * 获取分享详情
     */
    LyricsShareDTO getShareDetail(Long shareId);
}