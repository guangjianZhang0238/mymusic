package com.music.app.controller;

import com.music.api.dto.LyricsDTO;
import com.music.api.vo.LyricsVO;
import com.music.app.entity.AppFeedback;
import com.music.app.service.AppFeedbackService;
import com.music.app.vo.AppFeedbackVO;
import com.music.common.core.domain.PageResult;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.content.config.Lyrics2Config;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.LyricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * App端反馈接口（轻量内存实现，适合私人平台快速迭代）
 */
@Tag(name = "App端反馈接口")
@RestController
@RequestMapping("/api/app/music/feedback")
public class AppFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(AppFeedbackController.class);
    
    @Autowired
    private AppFeedbackService feedbackService;
    
    @Autowired
    private LyricsService lyricsService;
    
    @Autowired
    private SongMapper songMapper;
    
    @Autowired
    private ArtistMapper artistMapper;
    
    @Autowired
    private AlbumMapper albumMapper;
    
    @Autowired
    private Lyrics2Config lyrics2Config;
    
    // 反馈状态常量
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_FUTURE = "FUTURE";
    private static final String STATUS_UNABLE = "UNABLE";

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        return userId != null ? userId : 1L;
    }

    @Operation(summary = "提交反馈")
    @PostMapping
    public Result<Long> createFeedback(@RequestBody FeedbackCreateRequest request) {
        log.info("访问接口：开始提交反馈，类型: {}, 内容长度: {}", request.getType(), request.getContent() != null ? request.getContent().length() : 0);
        if (request == null || request.getType() == null || request.getType().isBlank()) {
            return Result.error("反馈类型不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            return Result.error("反馈内容不能为空");
        }
        
        Long userId = getCurrentUserId();
        String content = request.getContent().trim();
        String type = request.getType().trim();
        
        // 防重复提交检查
        if (feedbackService.isDuplicateSubmission(userId, type, content)) {
            log.warn("重复反馈被拦截，用户ID: {}, 类型: {}", userId, type);
            return Result.error("您刚刚已经提交过相同的反馈，请稍后再试");
        }

        AppFeedback feedback = new AppFeedback();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setSongId(request.getSongId());
        feedback.setKeyword(request.getKeyword());
        feedback.setContact(request.getContact());
        feedback.setScene(request.getScene());
        
        Long feedbackId = feedbackService.createFeedback(feedback);
        if (feedbackId != null) {
            log.info("反馈提交成功，ID: {}", feedbackId);
            return Result.success(feedbackId);
        } else {
            log.error("反馈提交失败");
            return Result.error("反馈提交失败");
        }
    }

    @Operation(summary = "获取我的反馈（分页）")
    @GetMapping("/mine")
    public Result<PageResult<AppFeedbackVO>> getMyFeedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("访问接口：开始获取我的反馈，页码: {}, 大小: {}", page, size);
        Long userId = getCurrentUserId();
        
        PageResult<AppFeedback> pageResult = feedbackService.getMyFeedbacks(userId, page, size);
        
        // 转换为VO对象
        List<AppFeedbackVO> voList = pageResult.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        PageResult<AppFeedbackVO> voPageResult = PageResult.of(
            voList,
            pageResult.getTotal(),
            pageResult.getSize(),
            pageResult.getCurrent()
        );
        
        return Result.success(voPageResult);
    }
    
    // ==================== 管理端接口 ====================
    
    @Operation(summary = "获取所有反馈列表（管理端）")
    @GetMapping("/admin/list")
    public Result<PageResult<AppFeedbackVO>> getAllFeedbacks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        log.info("访问接口：管理端获取反馈列表，页码: {}, 大小: {}, 状态: {}, 类型: {}", page, size, status, type);
        
        PageResult<AppFeedback> pageResult = feedbackService.getAllFeedbacks(page, size, status, type);
        
        // 转换为VO对象
        List<AppFeedbackVO> voList = pageResult.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        PageResult<AppFeedbackVO> voPageResult = PageResult.of(
            voList,
            pageResult.getTotal(),
            pageResult.getSize(),
            pageResult.getCurrent()
        );
        
        return Result.success(voPageResult);
    }
    
    @Operation(summary = "处理反馈（管理端）")
    @PutMapping("/admin/handle/{id}")
    public Result<Void> handleFeedback(
            @PathVariable Long id,
            @RequestBody FeedbackHandleRequest request) {
        log.info("访问接口：处理反馈，ID: {}, 状态: {}", id, request.getStatus());
        
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return Result.error("处理状态不能为空");
        }
        
        String newStatus = request.getStatus().trim();
        if (!List.of(STATUS_RESOLVED, STATUS_FUTURE, STATUS_UNABLE).contains(newStatus)) {
            return Result.error("无效的处理状态");
        }
        
        boolean success = feedbackService.handleFeedback(id, newStatus, request.getHandleNote());
        if (success) {
            log.info("反馈处理成功，ID: {}, 新状态: {}", id, newStatus);
            return Result.success();
        } else {
            return Result.error("反馈不存在或处理失败");
        }
    }

    @Operation(summary = "管理端-自动匹配歌词（根据反馈中的歌曲ID）")
    @PostMapping("/admin/auto-match-lyrics/{id}")
    public Result<LyricsVO> autoMatchLyrics(@PathVariable Long id) {
        log.info("访问接口：管理端自动匹配歌词，反馈ID: {}", id);
        AppFeedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }
        if (feedback.getSongId() == null) {
            return Result.error("该反馈未关联歌曲，无法自动匹配");
        }
        
        Long songId = feedback.getSongId();
        try {
            // 先检查歌曲是否存在
            Song song = songMapper.selectById(songId);
            if (song == null) {
                return Result.error("关联的歌曲不存在");
            }
            
            // 检查是否已有歌词
            LyricsVO existingLyrics = lyricsService.getBySongId(songId);
            if (existingLyrics != null && existingLyrics.getContent() != null && 
                !existingLyrics.getContent().trim().isEmpty()) {
                log.info("歌曲ID {} 已有歌词，直接返回现有歌词", songId);
                return Result.success(existingLyrics);
            }
            
            // 获取歌曲和歌手信息用于API查询
            String songName = song.getTitle();
            String artistName = null;
            
            if (song.getArtistId() != null) {
                Artist artist = artistMapper.selectById(song.getArtistId());
                if (artist != null) {
                    artistName = artist.getName();
                }
            }
            
            if (!StringUtils.hasText(songName)) {
                return Result.error("歌曲名称为空，无法匹配歌词");
            }
            
            // 构造API查询参数
            String encodedSongName = URLEncoder.encode(songName, StandardCharsets.UTF_8);
            
            String apiUrl = lyrics2Config.getPath() + "?" + lyrics2Config.getSongName() + "=" + encodedSongName;
            // Lyrics2Config只包含path和songName，不包含singerName参数
            
            log.info("开始自动匹配歌词，歌曲ID: {}, 歌曲名: {}, 歌手: {}, API: {}", 
                     songId, songName, artistName, apiUrl);
            
            Map<String, Object> result = lyricsService.autoMatchLyricsFromApi(apiUrl, songId);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                LyricsVO matchedLyrics = lyricsService.getBySongId(songId);
                if (matchedLyrics != null && StringUtils.hasText(matchedLyrics.getContent())) {
                    log.info("自动匹配歌词成功，反馈ID: {}, 歌曲ID: {}, 歌词长度: {}字符", 
                             id, songId, matchedLyrics.getContent().length());
                    return Result.success(matchedLyrics);
                } else {
                    return Result.error("匹配成功但未获取到歌词内容");
                }
            } else {
                String errorMsg = result.getOrDefault("message", "匹配失败").toString();
                log.warn("自动匹配歌词失败，反馈ID: {}, 歌曲ID: {}, 错误: {}", id, songId, errorMsg);
                return Result.error("自动匹配失败：" + errorMsg);
            }
        } catch (Exception e) {
            log.error("自动匹配歌词异常，反馈ID: {}, 歌曲ID: {}", id, songId, e);
            return Result.error("自动匹配失败：" + e.getMessage());
        }
    }

    @Operation(summary = "管理端-保存歌词内容到歌曲（全量替换）")
    @PostMapping("/admin/save-lyrics/{id}")
    public Result<Void> saveLyricsForFeedback(
            @PathVariable Long id,
            @RequestBody FeedbackSaveLyricsRequest request) {
        log.info("访问接口：管理端保存歌词，反馈ID: {}", id);
        AppFeedback feedback = feedbackService.getById(id);
        if (feedback == null || feedback.getSongId() == null) {
            return Result.error("反馈不存在或未关联歌曲");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            return Result.error("歌词内容不能为空");
        }
        Long songId = feedback.getSongId();
        try {
            // 查询是否已有歌词，有则更新，无则新建
            LyricsVO existing = lyricsService.getBySongId(songId);
            LyricsDTO dto = new LyricsDTO();
            dto.setSongId(songId);
            dto.setContent(request.getContent());
            dto.setLyricsType(request.getContent().contains("[") ? 1 : 0);
            dto.setSource("admin_manual");
            if (existing != null && existing.getId() != null) {
                dto.setId(existing.getId());
                lyricsService.update(dto);
            } else {
                lyricsService.create(dto);
            }
            log.info("保存歌词成功，反馈ID: {}, 歌曲ID: {}", id, songId);
            return Result.success();
        } catch (Exception e) {
            log.error("保存歌词异常", e);
            return Result.error("保存歌词失败：" + e.getMessage());
        }
    }

    @Data
    public static class FeedbackCreateRequest {
        private String type;
        private String content;
        private Long songId;
        private String keyword;
        private String contact;
        private String scene;
    }
    
    @Data
    public static class FeedbackHandleRequest {
        private String status;
        private String handleNote;
    }

    @Data
    public static class FeedbackSaveLyricsRequest {
        private String content;
    }
    
    /**
     * 将实体对象转换为VO对象
     */
    private AppFeedbackVO convertToVO(AppFeedback feedback) {
        AppFeedbackVO vo = new AppFeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setType(feedback.getType());
        vo.setContent(feedback.getContent());
        vo.setSongId(feedback.getSongId());
        vo.setKeyword(feedback.getKeyword());
        vo.setContact(feedback.getContact());
        vo.setScene(feedback.getScene());
        vo.setStatus(feedback.getStatus());
        vo.setHandleNote(feedback.getHandleNote());
        vo.setHandleTime(feedback.getHandleTime());
        vo.setCreateTime(feedback.getCreateTime());
        // 填充歌曲信息
        if (feedback.getSongId() != null) {
            try {
                Song song = songMapper.selectById(feedback.getSongId());
                if (song != null) {
                    vo.setSongTitle(song.getTitle());
                    if (song.getArtistId() != null) {
                        Artist artist = artistMapper.selectById(song.getArtistId());
                        if (artist != null) vo.setArtistName(artist.getName());
                    }
                    if (song.getAlbumId() != null) {
                        Album album = albumMapper.selectById(song.getAlbumId());
                        if (album != null) vo.setAlbumName(album.getName());
                    }
                }
            } catch (Exception e) {
                log.warn("获取歌曲信息失败, songId={}", feedback.getSongId(), e);
            }
        }
        return vo;
    }
}