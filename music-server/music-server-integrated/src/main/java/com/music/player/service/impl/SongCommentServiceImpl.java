package com.music.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.player.entity.SongComment;
import com.music.player.mapper.SongCommentMapper;
import com.music.player.service.SongCommentService;
import com.music.player.dto.SongCommentDTO;
import com.music.common.core.domain.PageResult;
import com.music.system.entity.User;
import com.music.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 歌曲评论服务实现
 */
@Service
public class SongCommentServiceImpl extends ServiceImpl<SongCommentMapper, SongComment> implements SongCommentService {

    @Resource
    private SongCommentMapper songCommentMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public Long addComment(SongCommentDTO dto) {
        SongComment comment = new SongComment();
        comment.setSongId(dto.getSongId());
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setDeleted(0);
        save(comment);
        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        SongComment comment = getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        // 检查权限：评论者或管理员
        if (!comment.getUserId().equals(userId)) {
            // 这里可以添加管理员检查，暂时只允许评论者删除
            throw new RuntimeException("无权删除此评论");
        }
        comment.setDeleted(1);
        updateById(comment);
    }

    @Override
    public PageResult<SongCommentDTO> getSongComments(Long songId, int page, int size) {
        LambdaQueryWrapper<SongComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SongComment::getSongId, songId);
        wrapper.eq(SongComment::getDeleted, 0);
        wrapper.orderByDesc(SongComment::getCreateTime);

        Page<SongComment> pageObj = new Page<>(page, size);
        Page<SongComment> result = page(pageObj, wrapper);

        List<SongCommentDTO> list = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        SongComment comment = getById(commentId);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            updateById(comment);
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        SongComment comment = getById(commentId);
        if (comment != null && comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            updateById(comment);
        }
    }

    /**
     * 转换为DTO，包含用户信息
     */
    private SongCommentDTO convertToDTO(SongComment comment) {
        SongCommentDTO dto = new SongCommentDTO();
        dto.setId(comment.getId());
        dto.setSongId(comment.getSongId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreateTime(comment.getCreateTime());
        dto.setUpdateTime(comment.getUpdateTime());

        // 查询用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatar(user.getAvatar());
        }

        return dto;
    }
}