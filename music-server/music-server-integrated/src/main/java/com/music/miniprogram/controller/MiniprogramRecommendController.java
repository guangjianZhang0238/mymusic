package com.music.miniprogram.controller;

import com.music.app.vo.AppSongVO;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.miniprogram.dto.RecommendationQueryDTO;
import com.music.miniprogram.service.MiniprogramRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "小程序推荐接口")
@RestController
@RequestMapping("/api/mp/recommend")
@RequiredArgsConstructor
public class MiniprogramRecommendController {

    private static final Logger log = LoggerFactory.getLogger(MiniprogramRecommendController.class);

    private final MiniprogramRecommendService recommendService;

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return 1L;
        }
        return userId;
    }

    @Operation(summary = "每日推荐")
    @GetMapping("/daily")
    public Result<List<AppSongVO>> daily(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        log.info("访问接口：小程序每日推荐");
        return Result.success(recommendService.dailyRecommend(getCurrentUserId(), dto.getLimit()));
    }

    @Operation(summary = "场景推荐")
    @GetMapping("/scene")
    public Result<List<AppSongVO>> scene(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        log.info("访问接口：小程序场景推荐");
        return Result.success(recommendService.sceneRecommend(getCurrentUserId(), dto.getLimit()));
    }

    @Operation(summary = "个性化推荐")
    @GetMapping("/personal")
    public Result<List<AppSongVO>> personal(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        log.info("访问接口：小程序个性化推荐");
        return Result.success(recommendService.personalRecommend(getCurrentUserId(), dto.getLimit()));
    }
}
