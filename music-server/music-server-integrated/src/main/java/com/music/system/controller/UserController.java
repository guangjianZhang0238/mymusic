package com.music.system.controller;

import com.music.api.vo.UserVO;
import com.music.common.core.domain.Result;
import com.music.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @Operation(summary = "获取用户信息")
    @GetMapping("/info/{userId}")
    public Result<UserVO> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }
    
    @Operation(summary = "更新用户信息")
    @PutMapping("/info/{userId}")
    public Result<UserVO> updateUserInfo(@PathVariable Long userId, @RequestBody UserVO userVO) {
        return Result.success(userService.updateUserInfo(userId, userVO));
    }
    
    @Operation(summary = "修改密码")
    @PutMapping("/password/{userId}")
    public Result<Void> updatePassword(
            @PathVariable Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
}
