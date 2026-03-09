package com.music.app.controller;

import com.music.app.vo.AppSongCommentVO;
import com.music.common.core.domain.Result;
import com.music.common.core.domain.PageResult;
import com.music.common.utils.SecurityUtils;
import com.music.player.dto.SongCommentDTO;
import com.music.player.service.SongCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * App端歌曲评论接口
 */
@Tag(name = "App端歌曲评论接口")
@RestController
@RequestMapping("/api/app/music/comment")
@RequiredArgsConstructor
public class AppSongCommentController {

    private static final Logger log = LoggerFactory.getLogger(AppSongCommentController.class);
    
    private final SongCommentService songCommentService;

    // 获取当前用户ID
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            // 如果未登录，返回默认用户ID（为了向后兼容）
            return 1L;
        }
        return userId;
    }

    // 转换DTO到VO
    private AppSongCommentVO convertToVO(SongCommentDTO dto) {
        if (dto == null) {
            return null;
        }
        AppSongCommentVO vo = new AppSongCommentVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }

    @Operation(summary = "添加评论")
    @PostMapping
    public Result<Long> addComment(@RequestBody SongCommentDTO dto) {
        log.info("访问接口：开始添加评论，歌曲ID: {}, 内容长度: {}", dto.getSongId(), dto.getContent() != null ? dto.getContent().length() : 0);
        dto.setUserId(getCurrentUserId());
        Long commentId = songCommentService.addComment(dto);
        return Result.success(commentId);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        log.info("访问接口：开始删除评论，评论ID: {}", commentId);
        Long userId = getCurrentUserId();
        songCommentService.deleteComment(commentId, userId);
        return Result.success();
    }

    @Operation(summary = "获取歌曲评论列表（分页）")
    @GetMapping("/song/{songId}")
    public Result<PageResult<AppSongCommentVO>> getSongComments(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("访问接口：开始获取歌曲评论列表，歌曲ID: {}, 页码: {}, 大小: {}", songId, page, size);
        PageResult<SongCommentDTO> pageResult = songCommentService.getSongComments(songId, page, size);
        List<AppSongCommentVO> vos = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<AppSongCommentVO> voPageResult = new PageResult<>(
                vos, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(), pageResult.getPages());
        return Result.success(voPageResult);
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId) {
        log.info("访问接口：开始点赞评论，评论ID: {}", commentId);
        songCommentService.likeComment(commentId);
        return Result.success();
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable Long commentId) {
        log.info("访问接口：开始取消点赞评论，评论ID: {}", commentId);
        songCommentService.unlikeComment(commentId);
        return Result.success();
    }
}