package com.music.dashboard.service;

import com.music.api.vo.SongVO;
import java.util.Map;
import java.util.List;

public interface DashboardService {
    
    /**
     * 获取仪表盘统计数据
     * @return 包含歌曲总数、专辑总数、歌手总数和总播放次数的Map
     */
    Map<String, Object> getDashboardStats();
    
    /**
     * 获取播放量排行榜
     * @param limit 限制返回的歌曲数量
     * @return 按播放量排序的歌曲列表
     */
    List<SongVO> getPlayCountRanking(int limit);
}
