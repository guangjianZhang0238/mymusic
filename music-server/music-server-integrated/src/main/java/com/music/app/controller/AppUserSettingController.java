package com.music.app.controller;

import com.music.app.service.AppUserSettingService;
import com.music.common.core.domain.Result;
import com.music.system.vo.UserSettingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "App用户设置管理")
@RestController
@RequestMapping("/api/app/user-setting")
@RequiredArgsConstructor
public class AppUserSettingController {
    
    private final AppUserSettingService appUserSettingService;
    
    @Operation(summary = "获取当前用户特定设置")
    @GetMapping("/{settingKey}")
    public Result<UserSettingVO> getCurrentUserSetting(@PathVariable String settingKey) {
        return Result.success(appUserSettingService.getCurrentUserSetting(settingKey));
    }
    
    @Operation(summary = "获取当前用户所有设置")
    @GetMapping
    public Result<List<UserSettingVO>> getCurrentUserSettings() {
        return Result.success(appUserSettingService.getCurrentUserSettings());
    }
    
    @Operation(summary = "保存或更新当前用户设置")
    @PostMapping
    public Result<UserSettingVO> saveOrUpdateCurrentUserSetting(
            @RequestParam String settingKey,
            @RequestParam String settingValue,
            @RequestParam(required = false) String settingType,
            @RequestParam(required = false) String description) {
        UserSettingVO result = appUserSettingService.saveOrUpdateCurrentUserSetting(settingKey, settingValue, settingType, description);
        return Result.success(result);
    }
    
    @Operation(summary = "删除当前用户设置")
    @DeleteMapping("/{settingKey}")
    public Result<Void> deleteCurrentUserSetting(@PathVariable String settingKey) {
        appUserSettingService.deleteCurrentUserSetting(settingKey);
        return Result.success();
    }
    
    @Operation(summary = "批量保存当前用户设置")
    @PostMapping("/batch")
    public Result<Void> batchSaveCurrentUserSettings(@RequestBody List<UserSettingVO> settings) {
        appUserSettingService.batchSaveCurrentUserSettings(settings);
        return Result.success();
    }
}