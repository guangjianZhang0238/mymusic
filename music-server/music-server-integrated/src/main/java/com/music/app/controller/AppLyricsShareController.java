package com.music.app.controller;

import com.music.app.vo.AppLyricsShareVO;
import com.music.common.core.domain.Result;
import com.music.common.core.domain.PageResult;
import com.music.common.utils.SecurityUtils;
import com.music.player.dto.LyricsShareDTO;
import com.music.player.service.LyricsShareService;
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
 * App端歌词分享接口
 */
@Tag(name = "App端歌词分享接口")
@RestController
@RequestMapping("/api/app/music/lyrics/share")
@RequiredArgsConstructor
public class AppLyricsShareController {

    private static final Logger log = LoggerFactory.getLogger(AppLyricsShareController.class);
    
    private final LyricsShareService lyricsShareService;

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
    private AppLyricsShareVO convertToVO(LyricsShareDTO dto) {
        if (dto == null) {
            return null;
        }
        AppLyricsShareVO vo = new AppLyricsShareVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }

    @Operation(summary = "创建歌词分享")
    @PostMapping
    public Result<Long> createShare(@RequestBody LyricsShareDTO dto) {
        log.info("访问接口：开始创建歌词分享，歌词ID: {}", dto.getLyricsId());
        dto.setUserId(getCurrentUserId());
        Long shareId = lyricsShareService.createShare(dto);
        return Result.success(shareId);
    }

    @Operation(summary = "删除歌词分享")
    @DeleteMapping("/{shareId}")
    public Result<Void> deleteShare(@PathVariable Long shareId) {
        log.info("访问接口：开始删除歌词分享，分享ID: {}", shareId);
        Long userId = getCurrentUserId();
        lyricsShareService.deleteShare(shareId, userId);
        return Result.success();
    }

    @Operation(summary = "获取当前用户的歌词分享列表（分页）")
    @GetMapping("/user")
    public Result<PageResult<AppLyricsShareVO>> getUserShares(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("访问接口：开始获取当前用户的歌词分享列表，页码: {}, 大小: {}", page, size);
        Long userId = getCurrentUserId();
        PageResult<LyricsShareDTO> pageResult = lyricsShareService.getUserShares(userId, page, size);
        List<AppLyricsShareVO> vos = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<AppLyricsShareVO> voPageResult = new PageResult<>(
                vos, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(), pageResult.getPages());
        return Result.success(voPageResult);
    }

    @Operation(summary = "获取歌词的分享列表（分页）")
    @GetMapping("/lyrics/{lyricsId}")
    public Result<PageResult<AppLyricsShareVO>> getLyricsShares(
            @PathVariable Long lyricsId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("访问接口：开始获取歌词的分享列表，歌词ID: {}, 页码: {}, 大小: {}", lyricsId, page, size);
        PageResult<LyricsShareDTO> pageResult = lyricsShareService.getLyricsShares(lyricsId, page, size);
        List<AppLyricsShareVO> vos = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<AppLyricsShareVO> voPageResult = new PageResult<>(
                vos, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(), pageResult.getPages());
        return Result.success(voPageResult);
    }

    @Operation(summary = "获取分享详情")
    @GetMapping("/{shareId}")
    public Result<AppLyricsShareVO> getShareDetail(@PathVariable Long shareId) {
        log.info("访问接口：开始获取分享详情，分享ID: {}", shareId);
        LyricsShareDTO dto = lyricsShareService.getShareDetail(shareId);
        return Result.success(convertToVO(dto));
    }
}