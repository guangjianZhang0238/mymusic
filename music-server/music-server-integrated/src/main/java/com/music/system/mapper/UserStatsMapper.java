package com.music.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.system.entity.UserStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户统计Mapper接口
 */
@Mapper
public interface UserStatsMapper extends BaseMapper<UserStats> {
    
    /**
     * 根据用户ID获取用户统计信息
     */
    @Select("SELECT * FROM sys_user_stats WHERE user_id = #{userId} AND deleted = 0")
    UserStats getByUserId(@Param("userId") Long userId);
    
    /**
     * 增加用户登录次数
     */
    @Update("UPDATE sys_user_stats SET login_count = login_count + 1, last_login_time = NOW(), update_time = NOW() WHERE user_id = #{userId}")
    void incrementLoginCount(@Param("userId") Long userId);
    
    /**
     * 更新用户在线时长
     */
    @Update("UPDATE sys_user_stats SET total_online_time = total_online_time + #{duration}, update_time = NOW() WHERE user_id = #{userId}")
    void updateOnlineTime(@Param("userId") Long userId, @Param("duration") Long duration);
    
    /**
     * 更新播放统计
     */
    @Update("UPDATE sys_user_stats SET total_play_count = total_play_count + 1, last_play_time = NOW(), update_time = NOW() WHERE user_id = #{userId}")
    void incrementPlayCount(@Param("userId") Long userId);
    
    /**
     * 开始新的会话
     */
    @Update("UPDATE sys_user_stats SET current_session_start = NOW(), current_session_duration = 0, update_time = NOW() WHERE user_id = #{userId}")
    void startNewSession(@Param("userId") Long userId);
    
    /**
     * 更新当前会话时长
     */
    @Update("UPDATE sys_user_stats SET current_session_duration = TIMESTAMPDIFF(SECOND, current_session_start, NOW()), update_time = NOW() WHERE user_id = #{userId} AND current_session_start IS NOT NULL")
    void updateCurrentSessionDuration(@Param("userId") Long userId);
}