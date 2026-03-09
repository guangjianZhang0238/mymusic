package com.music.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music.system.entity.AsyncTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异步任务Mapper
 */
@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {
}