package com.music.system.controller;

import com.music.common.core.domain.Result;
import com.music.system.service.UserSettingService;
import com.music.system.vo.UserSettingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户设置管理")
@RestController
@RequestMapping("/api/user-setting")
@RequiredArgsConstructor
public class UserSettingController {
    
    private final UserSettingService userSettingService;
    
    @Operation(summary = "获取用户特定设置")
    @GetMapping("/{userId}/{settingKey}")
    public Result<UserSettingVO> getUserSetting(
            @PathVariable Long userId,
            @PathVariable String settingKey) {
        return Result.success(userSettingService.getUserSetting(userId, settingKey));
    }
    
    @Operation(summary = "获取用户所有设置")
    @GetMapping("/{userId}")
    public Result<List<UserSettingVO>> getUserSettings(@PathVariable Long userId) {
        return Result.success(userSettingService.getUserSettings(userId));
    }
    
    @Operation(summary = "保存或更新用户设置")
    @PostMapping("/{userId}")
    public Result<UserSettingVO> saveOrUpdateSetting(
            @PathVariable Long userId,
            @RequestParam String settingKey,
            @RequestParam String settingValue,
            @RequestParam(required = false) String settingType,
            @RequestParam(required = false) String description) {
        UserSettingVO result = userSettingService.saveOrUpdateSetting(userId, settingKey, settingValue, settingType, description);
        return Result.success(result);
    }
    
    @Operation(summary = "删除用户设置")
    @DeleteMapping("/{userId}/{settingKey}")
    public Result<Void> deleteUserSetting(
            @PathVariable Long userId,
            @PathVariable String settingKey) {
        userSettingService.deleteUserSetting(userId, settingKey);
        return Result.success();
    }
    
    @Operation(summary = "批量保存用户设置")
    @PostMapping("/{userId}/batch")
    public Result<Void> batchSaveSettings(
            @PathVariable Long userId,
            @RequestBody List<UserSettingVO> settings) {
        userSettingService.batchSaveSettings(userId, settings);
        return Result.success();
    }
}