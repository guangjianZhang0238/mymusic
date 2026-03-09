package com.music.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.system.entity.UserSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户会话Mapper接口
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
    
    /**
     * 根据用户ID获取活跃会话
     */
    @Select("SELECT * FROM sys_user_session WHERE user_id = #{userId} AND is_active = 1 AND deleted = 0 ORDER BY last_active_time DESC")
    List<UserSession> getActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据会话ID获取会话信息
     */
    @Select("SELECT * FROM sys_user_session WHERE session_id = #{sessionId} AND deleted = 0")
    UserSession getBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 获取用户的所有活跃会话
     */
    @Select("SELECT * FROM sys_user_session WHERE user_id = #{userId} AND is_active = 1 AND deleted = 0")
    List<UserSession> getUserActiveSessions(@Param("userId") Long userId);
    
    /**
     * 强制用户下线（结束所有活跃会话）
     */
    @Update("UPDATE sys_user_session SET is_active = 0, logout_time = NOW(), update_time = NOW() WHERE user_id = #{userId} AND is_active = 1")
    int forceLogoutUser(@Param("userId") Long userId);
    
    /**
     * 更新会话最后活跃时间
     */
    @Update("UPDATE sys_user_session SET last_active_time = NOW(), update_time = NOW() WHERE session_id = #{sessionId} AND is_active = 1")
    int updateLastActiveTime(@Param("sessionId") String sessionId);
    
    /**
     * 结束指定会话
     */
    @Update("UPDATE sys_user_session SET is_active = 0, logout_time = NOW(), update_time = NOW() WHERE session_id = #{sessionId}")
    int endSession(@Param("sessionId") String sessionId);
}