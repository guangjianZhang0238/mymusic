package com.music.miniprogram.service;

import com.music.app.vo.AppSongVO;

import java.util.List;

public interface MiniprogramRecommendService {

    List<AppSongVO> dailyRecommend(Long userId, int limit);

    List<AppSongVO> sceneRecommend(Long userId, int limit);

    List<AppSongVO> personalRecommend(Long userId, int limit);
}
