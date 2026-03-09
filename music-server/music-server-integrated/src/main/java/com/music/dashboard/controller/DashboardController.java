package com.music.dashboard.controller;

import com.music.api.vo.SongVO;
import com.music.common.core.domain.Result;
import com.music.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "仪表盘管理")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        return Result.success(dashboardService.getDashboardStats());
    }
    
    @Operation(summary = "获取播放量排行榜")
    @GetMapping("/ranking")
    public Result<List<SongVO>> getPlayCountRanking(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getPlayCountRanking(limit));
    }
}
