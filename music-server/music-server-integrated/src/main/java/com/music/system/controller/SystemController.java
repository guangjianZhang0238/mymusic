package com.music.system.controller;

import com.music.common.core.domain.Result;
import com.music.dashboard.service.DashboardService;
import com.music.system.entity.UserStats;
import com.music.system.service.UserStatsService;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.music.file.service.AudioTranscodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "系统状态")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final AudioTranscodingService audioTranscodingService;
    private final DashboardService dashboardService;
    private final UserStatsService userStatsService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${music.transcoding.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${music.storage.data-root-url:}")
    private String dataRootPath;

    @Value("${music.storage.temp-path:}")
    private String tempPath;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 系统基础信息（名称、版本、环境等）
     */
    @Operation(summary = "获取系统基础信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "音乐管理系统");
        data.put("version", "1.0.0");
        data.put("env", activeProfile);
        data.put("backendBaseUrl", "http://localhost:" + serverPort + ("/".equals(contextPath) ? "" : contextPath));
        // 前端地址由管理端自行配置展示，这里只给出一个建议字段
        data.put("frontendBaseUrl", "http://localhost:5555");
        data.put("dataRootPath", dataRootPath);
        data.put("tempPath", tempPath);
        return Result.success(data);
    }

    @Operation(summary = "获取转码/FFmpeg 状态")
    @GetMapping("/transcoding-status")
    public Result<Map<String, Object>> getTranscodingStatus() {
        boolean available = audioTranscodingService.isFfmpegAvailable();
        boolean enabled = audioTranscodingService.isTranscodingEnabled();
        Map<String, Object> data = new HashMap<>();
        data.put("ffmpegAvailable", available);
        data.put("ffmpegPath", ffmpegPath);
        data.put("transcodingEnabled", enabled);
        return Result.success(data);
    }

    /**
     * 系统运行总览：内容统计 + 播放统计 + 在线用户等
     */
    @Operation(summary = "获取系统总览统计信息")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 内容与播放相关统计（复用仪表盘服务）
        Map<String, Object> dashboardStats = dashboardService.getDashboardStats();
        overview.putAll(dashboardStats);

        // 在线用户数量（基于 UserStats 是否在线的简单统计）
        long onlineUserCount = userStatsService.list().stream()
                .filter(stats -> stats.getCurrentSessionStart() != null)
                .filter(stats -> {
                    // 30 分钟内视为在线
                    if (stats.getCurrentSessionStart() == null) {
                        return false;
                    }
                    return java.time.Duration.between(
                            stats.getCurrentSessionStart(),
                            java.time.LocalDateTime.now()
                    ).toMinutes() <= 30;
                })
                .count();
        long totalUserCount = userStatsService.list().size();
        overview.put("onlineUserCount", onlineUserCount);
        overview.put("totalUserCount", totalUserCount);

        return Result.success(overview);
    }

    /**
     * Top 活跃用户（按播放次数排序）
     */
    @Operation(summary = "获取最活跃用户排行榜")
    @GetMapping("/top-active-users")
    public Result<List<UserStats>> getTopActiveUsers(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit) {
        List<UserStats> all = userStatsService.list();
        all.sort((a, b) -> Long.compare(
                b.getTotalPlayCount() == null ? 0L : b.getTotalPlayCount(),
                a.getTotalPlayCount() == null ? 0L : a.getTotalPlayCount()
        ));
        if (limit > 0 && all.size() > limit) {
            all = all.subList(0, limit);
        }
        return Result.success(all);
    }

    /**
     * 缓存状态：只返回 Redis 是否连接正常等简单信息
     */
    @Operation(summary = "获取缓存状态")
    @GetMapping("/cache-status")
    public Result<Map<String, Object>> getCacheStatus() {
        Map<String, Object> data = new HashMap<>();
        boolean redisOk = false;
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().get("health:ping");
                redisOk = true;
            }
        } catch (Exception ignored) {
            redisOk = false;
        }
        data.put("redisOk", redisOk);
        return Result.success(data);
    }

    /**
     * 清空 Redis 缓存（谨慎使用，主要给个人平台用）
     */
    @Operation(summary = "清空应用缓存（Redis）")
    @GetMapping("/clear-cache")
    public Result<Void> clearCache() {
        if (stringRedisTemplate != null && stringRedisTemplate.getConnectionFactory() != null) {
            stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .serverCommands()
                    .flushDb();
        }
        return Result.success();
    }

    /**
     * 清理临时目录文件
     */
    @Operation(summary = "清理临时文件目录")
    @GetMapping("/cleanup-temp")
    public Result<Map<String, Object>> cleanupTempFiles() {
        Map<String, Object> result = new HashMap<>();
        if (tempPath == null || tempPath.isEmpty()) {
            result.put("deletedFiles", 0);
            result.put("message", "未配置临时目录");
            return Result.success(result);
        }
        File dir = new File(dataRootPath, tempPath);
        if (!dir.exists() || !dir.isDirectory()) {
            result.put("deletedFiles", 0);
            result.put("message", "临时目录不存在");
            return Result.success(result);
        }
        int deleted = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.delete()) {
                    deleted++;
                }
            }
        }
        result.put("deletedFiles", deleted);
        result.put("message", "已清理临时文件: " + deleted + " 个");
        return Result.success(result);
    }
}
