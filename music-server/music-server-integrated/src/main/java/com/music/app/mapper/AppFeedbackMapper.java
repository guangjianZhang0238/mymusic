package com.music.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.app.entity.AppFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * App端用户反馈Mapper接口
 */
@Mapper
public interface AppFeedbackMapper extends BaseMapper<AppFeedback> {
}