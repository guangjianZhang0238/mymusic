package com.music.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.system.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
    
    @Select("SELECT * FROM sys_user_setting WHERE user_id = #{userId} AND setting_key = #{settingKey}")
    UserSetting getByUserIdAndKey(@Param("userId") Long userId, @Param("settingKey") String settingKey);
    
    @Select("SELECT * FROM sys_user_setting WHERE user_id = #{userId}")
    List<UserSetting> getByUserId(@Param("userId") Long userId);
}