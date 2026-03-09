package com.music.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.music.api.vo.SongVO;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.dashboard.service.DashboardService;
import com.music.player.service.PlayCountCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;
    private final ArtistMapper artistMapper;
    private final PlayCountCacheService playCountCacheService;

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 获取歌曲总数
        long songCount = songMapper.selectCount(null);
        stats.put("songCount", songCount);

        // 获取专辑总数
        long albumCount = albumMapper.selectCount(null);
        stats.put("albumCount", albumCount);

        // 获取歌手总数
        long artistCount = artistMapper.selectCount(null);
        stats.put("artistCount", artistCount);

        // 获取总播放次数：数据库已落库 + Redis待同步增量
        Long dbTotalPlayCount = songMapper.selectTotalPlayCount();
        long pendingPlayCount = playCountCacheService.getPendingTotalPlayCount();
        long totalPlayCount = (dbTotalPlayCount == null ? 0L : dbTotalPlayCount) + pendingPlayCount;
        stats.put("totalPlayCount", totalPlayCount);

        return stats;
    }

    @Override
    public List<SongVO> getPlayCountRanking(int limit) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + limit);

        List<Song> songs = songMapper.selectList(wrapper);
        return songs.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::convertToVO)
                .toList();
    }

    private SongVO convertToVO(Song song) {
        SongVO vo = new SongVO();
        if (song == null) {
            return vo;
        }
        BeanUtils.copyProperties(song, vo);

        // 获取歌手名称
        Artist artist = artistMapper.selectById(song.getArtistId());
        if (artist != null) {
            vo.setArtistName(artist.getName());
        }

        // 获取专辑名称
        Album album = albumMapper.selectById(song.getAlbumId());
        if (album != null) {
            vo.setAlbumName(album.getName());
        }

        return vo;
    }
}
