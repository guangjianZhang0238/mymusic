package com.music.player.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.player.entity.SongComment;
import com.music.player.dto.SongCommentDTO;
import com.music.common.core.domain.PageResult;

import java.util.List;

/**
 * 歌曲评论服务接口
 */
public interface SongCommentService extends IService<SongComment> {

    /**
     * 添加评论
     */
    Long addComment(SongCommentDTO dto);

    /**
     * 删除评论（仅限评论者或管理员）
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 获取歌曲的评论列表（分页）
     */
    PageResult<SongCommentDTO> getSongComments(Long songId, int page, int size);

    /**
     * 点赞评论
     */
    void likeComment(Long commentId);

    /**
     * 取消点赞评论
     */
    void unlikeComment(Long commentId);
}