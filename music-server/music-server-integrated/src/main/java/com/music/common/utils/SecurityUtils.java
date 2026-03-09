package com.music.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtils {
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // 从认证对象中获取用户ID
        // 这里需要根据实际的UserDetails实现来调整
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            // 这种情况通常是在测试或使用内存用户时
            return 1L; // 默认ID
        }
        
        // 假设我们的UserDetails实现类有getUserId方法
        try {
            return (Long) principal.getClass().getMethod("getUserId").invoke(principal);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
    
    /**
     * 检查用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}