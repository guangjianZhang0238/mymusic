package com.music.system.controller;

import com.music.common.core.domain.Result;
import com.music.file.service.AudioTranscodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统状态")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final AudioTranscodingService audioTranscodingService;

    @Value("${music.transcoding.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Operation(summary = "获取转码/FFmpeg 状态")
    @GetMapping("/transcoding-status")
    public Result<Map<String, Object>> getTranscodingStatus() {
        boolean available = audioTranscodingService.isFfmpegAvailable();
        boolean enabled = audioTranscodingService.isTranscodingEnabled();
        Map<String, Object> data = Map.of(
                "ffmpegAvailable", available,
                "ffmpegPath", ffmpegPath,
                "transcodingEnabled", enabled
        );
        return Result.success(data);
    }
}
