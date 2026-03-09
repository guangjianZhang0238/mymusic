package com.music.app.controller;

import com.music.api.vo.LyricsVO;
import com.music.common.core.domain.Result;
import com.music.content.service.LyricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App端歌词接口")
@RestController
@RequestMapping("/api/app/music/lyrics")
@RequiredArgsConstructor
public class AppLyricsController {

    private static final Logger log = LoggerFactory.getLogger(AppLyricsController.class);
    
    private final LyricsService lyricsService;

    @Operation(summary = "App端根据歌曲ID获取歌词")
    @GetMapping("/song/{songId}")
    public Result<LyricsVO> getBySongId(@PathVariable Long songId) {
        log.info("访问接口：开始根据歌曲ID获取歌词，歌曲ID: {}", songId);
        return Result.success(lyricsService.getBySongId(songId));
    }
}
