package com.music.app.controller;

import com.music.api.dto.LoginDTO;
import com.music.api.dto.RegisterDTO;
import com.music.api.vo.LoginVO;
import com.music.api.vo.UserVO;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * App端认证接口
 */
@Tag(name = "App端认证接口")
@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AppAuthController.class);
    
    private final UserService userService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("访问接口：开始用户登录，用户名: {}", dto.getUsername());
        return Result.success(userService.login(dto));
    }
    
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        log.info("访问接口：开始用户注册，用户名: {}", dto.getUsername());
        userService.register(dto);
        return Result.success();
    }
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<LoginVO> getCurrentUser() {
        log.info("访问接口：开始获取当前用户信息");
        
        // 从安全上下文中获取当前用户ID
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            log.warn("访问接口：用户未登录或无法获取用户ID");
            return Result.success(null);
        }
        
        log.info("访问接口：获取到当前用户ID: {}", userId);
        
        try {
            // 获取用户详细信息
            UserVO userVO = userService.getUserInfo(userId);
            
            // 构造LoginVO响应（不包含token，因为这是获取用户信息而非登录）
            LoginVO loginVO = new LoginVO();
            loginVO.setUserInfo(userVO);
            // token字段保持为null，表示这不是登录操作
            loginVO.setToken(null);
            
            log.info("访问接口：成功获取用户信息，用户名: {}", userVO.getUsername());
            return Result.success(loginVO);
        } catch (Exception e) {
            log.error("访问接口：获取用户信息失败，用户ID: {}", userId, e);
            return Result.success(null);
        }
    }
    
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        log.info("访问接口：开始刷新令牌");
        // 这里需要实现刷新令牌逻辑
        // 暂时返回空字符串，后续实现
        return Result.success("");
    }
}