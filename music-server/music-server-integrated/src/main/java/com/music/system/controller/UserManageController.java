package com.music.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.common.core.domain.PageResult;
import com.music.common.core.domain.Result;
import com.music.system.dto.UserPasswordDTO;
import com.music.system.entity.User;
import com.music.system.entity.UserSession;
import com.music.system.entity.UserStats;
import com.music.system.service.UserService;
import com.music.system.service.UserSessionService;
import com.music.system.service.UserStatsService;
import com.music.system.vo.UserManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
public class UserManageController {
    
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final UserSessionService userSessionService;
    
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    public Result<PageResult<UserManageVO>> pageUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户名搜索") @RequestParam(required = false) String username,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @Parameter(description = "角色筛选") @RequestParam(required = false) Integer role,
            @Parameter(description = "在线状态筛选") @RequestParam(required = false) Integer onlineStatus) {
        
        log.info("分页查询用户列表，current={}, size={}, username={}, status={}, role={}, onlineStatus={}", 
                current, size, username, status, role, onlineStatus);
        
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = userService.page(page, wrapper);
        
        List<UserManageVO> userVOList = userPage.getRecords().stream()
                .map(this::convertToUserManageVO)
                .collect(Collectors.toList());
        
        // 应用在线状态过滤
        if (onlineStatus != null) {
            userVOList = userVOList.stream()
                    .filter(vo -> {
                        if (onlineStatus == 1) {
                            return vo.getIsOnline();
                        } else {
                            return !vo.getIsOnline();
                        }
                    })
                    .collect(Collectors.toList());
        }
        
        PageResult<UserManageVO> result = new PageResult<>();
        result.setRecords(userVOList);
        result.setTotal(userPage.getTotal());
        result.setCurrent(current);
        result.setSize(size);
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    public Result<UserManageVO> getUserDetail(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("获取用户详情，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(convertToUserManageVO(user));
    }
    
    @Operation(summary = "修改用户密码")
    @PutMapping("/{userId}/password")
    public Result<Void> updateUserPassword(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody UserPasswordDTO passwordDTO) {
        
        log.info("修改用户密码，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        userService.updatePassword(userId, user.getPassword(), passwordDTO.getNewPassword());
        return Result.success();
    }
    
    @Operation(summary = "禁用用户")
    @PutMapping("/{userId}/disable")
    public Result<Void> disableUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("禁用用户，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setStatus(0);
        userService.updateById(user);
        return Result.success();
    }
    
    @Operation(summary = "启用用户")
    @PutMapping("/{userId}/enable")
    public Result<Void> enableUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("启用用户，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setStatus(1);
        userService.updateById(user);
        return Result.success();
    }
    
    @Operation(summary = "强制用户下线")
    @PostMapping("/{userId}/force-logout")
    public Result<Void> forceLogoutUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("强制用户下线，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 调用会话服务强制下线用户
        boolean success = userSessionService.forceLogoutUser(userId);
        if (success) {
            return Result.success();
        } else {
            return Result.error("强制下线失败");
        }
    }
    
    @Operation(summary = "获取在线用户列表")
    @GetMapping("/online")
    public Result<List<UserManageVO>> getOnlineUsers() {
        log.info("获取在线用户列表");
        
        try {
            // 获取所有活跃会话的用户ID
            List<UserSession> activeSessions = userSessionService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSession>()
                    .eq(UserSession::getIsActive, 1)
                    .orderByDesc(UserSession::getLastActiveTime)
            );
            
            // 去重用户ID
            java.util.Set<Long> userIds = activeSessions.stream()
                .map(UserSession::getUserId)
                .collect(java.util.stream.Collectors.toSet());
            
            // 获取用户信息
            List<User> users = userService.listByIds(userIds);
            List<UserManageVO> userVOList = users.stream()
                .map(this::convertToUserManageVO)
                .collect(java.util.stream.Collectors.toList());
            
            return Result.success(userVOList);
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return Result.success(java.util.List.of());
        }
    }
    
    @Operation(summary = "获取用户统计信息")
    @GetMapping("/{userId}/stats")
    public Result<UserManageVO> getUserStats(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("获取用户统计信息，userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(convertToUserManageVO(user));
    }
    
    /**
     * 转换用户实体为管理VO
     */
    private UserManageVO convertToUserManageVO(User user) {
        UserManageVO vo = new UserManageVO();
        BeanUtils.copyProperties(user, vo);
        
        // 获取用户统计信息
        UserStats stats = userStatsService.getUserStats(user.getId());
        if (stats != null) {
            vo.setLoginCount(stats.getLoginCount());
            vo.setTotalOnlineTime(stats.getTotalOnlineTime());
            vo.setCurrentSessionStart(stats.getCurrentSessionStart());
            vo.setCurrentSessionDuration(stats.getCurrentSessionDuration());
            vo.setTotalPlayCount(stats.getTotalPlayCount());
            vo.setLastPlayTime(stats.getLastPlayTime());
            // 判断是否在线（如果有当前会话且会话开始时间在30分钟内）
            if (stats.getCurrentSessionStart() != null) {
                long minutesSinceLogin = java.time.Duration.between(stats.getCurrentSessionStart(), 
                    java.time.LocalDateTime.now()).toMinutes();
                vo.setIsOnline(minutesSinceLogin <= 30);
            } else {
                vo.setIsOnline(false);
            }
        }
        
        return vo;
    }
}