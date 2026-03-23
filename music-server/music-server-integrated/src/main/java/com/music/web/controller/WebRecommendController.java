package com.music.web.controller;

import com.music.app.vo.AppSongVO;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.miniprogram.dto.RecommendationQueryDTO;
import com.music.miniprogram.service.MiniprogramRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Web推荐接口")
@RestController
@RequestMapping("/api/web/recommend")
@RequiredArgsConstructor
public class WebRecommendController {

    private final MiniprogramRecommendService recommendService;

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        return userId == null ? 1L : userId;
    }

    @Operation(summary = "每日推荐")
    @GetMapping("/daily")
    public Result<List<AppSongVO>> daily(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        return Result.success(recommendService.dailyRecommend(getCurrentUserId(), dto.getLimit()));
    }

    @Operation(summary = "场景推荐")
    @GetMapping("/scene")
    public Result<List<AppSongVO>> scene(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        return Result.success(recommendService.sceneRecommend(getCurrentUserId(), dto.getLimit()));
    }

    @Operation(summary = "个性化推荐")
    @GetMapping("/personal")
    public Result<List<AppSongVO>> personal(@Valid @ModelAttribute RecommendationQueryDTO dto) {
        return Result.success(recommendService.personalRecommend(getCurrentUserId(), dto.getLimit()));
    }
}
