package com.music.player.controller;

import com.music.player.service.PlayHistoryService;
import com.music.player.dto.PlayHistoryDTO;
import com.music.common.core.domain.Result;
import com.music.common.core.domain.PageResult;
import com.music.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 播放历史控制器
 */
@RestController
@RequestMapping("/play-history")
@Tag(name = "播放历史管理", description = "播放历史相关接口")
public class PlayHistoryController {

    @Resource
    private PlayHistoryService playHistoryService;

    @Operation(summary = "添加播放历史")
    @PostMapping
    public Result<Void> addPlayHistory(@RequestBody PlayHistoryDTO dto) {
        dto.setUserId(SecurityUtils.getUserId());
        playHistoryService.addPlayHistory(dto);
        return Result.success();
    }

    @Operation(summary = "分页查询播放历史")
    @GetMapping("/page")
    public Result<PageResult<PlayHistoryDTO>> pagePlayHistory(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getUserId();
        PageResult<PlayHistoryDTO> result = playHistoryService.pagePlayHistory(userId, current, size);
        return Result.success(result);
    }

    @Operation(summary = "获取用户最近播放的歌曲")
    @GetMapping("/recent")
    public Result<List<Long>> getUserRecentPlays(
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtils.getUserId();
        List<Long> songIds = playHistoryService.getUserRecentPlays(userId, limit);
        return Result.success(songIds);
    }

    @Operation(summary = "获取用户播放次数最多的歌曲")
    @GetMapping("/top")
    public Result<List<Long>> getUserTopPlayedSongs(
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = SecurityUtils.getUserId();
        List<Long> songIds = playHistoryService.getUserTopPlayedSongs(userId, limit);
        return Result.success(songIds);
    }

    @Operation(summary = "清空用户的播放历史")
    @DeleteMapping
    public Result<Void> clearPlayHistory() {
        Long userId = SecurityUtils.getUserId();
        playHistoryService.clearPlayHistory(userId);
        return Result.success();
    }

    @Operation(summary = "删除指定的播放历史")
    @DeleteMapping("/{id}")
    public Result<Void> deletePlayHistory(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        playHistoryService.deletePlayHistory(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取用户的播放统计")
    @GetMapping("/stats")
    public Result<Long> getUserPlayCount() {
        Long userId = SecurityUtils.getUserId();
        Long count = playHistoryService.getUserPlayCount(userId);
        return Result.success(count);
    }

    @Operation(summary = "获取用户今日播放次数")
    @GetMapping("/today")
    public Result<Integer> getUserTodayPlayCount() {
        Long userId = SecurityUtils.getUserId();
        Integer count = playHistoryService.getUserTodayPlayCount(userId);
        return Result.success(count);
    }
}
