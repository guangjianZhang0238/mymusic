package com.music.miniprogram.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.app.service.AppMusicService;
import com.music.app.vo.AppSongVO;
import com.music.content.entity.Album;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.SongMapper;
import com.music.miniprogram.service.MiniprogramRecommendService;
import com.music.player.service.FavoriteService;
import com.music.player.service.PlayHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniprogramRecommendServiceImpl implements MiniprogramRecommendService {

    private final PlayHistoryService playHistoryService;
    private final FavoriteService favoriteService;
    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;
    private final AppMusicService appMusicService;

    @Override
    @Cacheable(cacheNames = "mp:recommend:daily", key = "#userId + ':' + #limit")
    public List<AppSongVO> dailyRecommend(Long userId, int limit) {
        List<Long> baseSongIds = collectBaseSongIds(userId, limit);
        List<Long> candidateSongIds = expandByArtistAlbum(baseSongIds, limit);
        return toSongVos(fillFromHot(candidateSongIds, limit), limit);
    }

    @Override
    @Cacheable(cacheNames = "mp:recommend:scene", key = "#userId + ':' + #limit")
    public List<AppSongVO> sceneRecommend(Long userId, int limit) {
        List<Long> recent = playHistoryService.getUserRecentPlays(userId, limit * 3);
        List<Long> candidate = expandByArtistAlbum(recent, limit);
        return toSongVos(fillFromHot(candidate, limit), limit);
    }

    @Override
    @Cacheable(cacheNames = "mp:recommend:personal", key = "#userId + ':' + #limit")
    public List<AppSongVO> personalRecommend(Long userId, int limit) {
        List<Long> baseSongIds = collectBaseSongIds(userId, limit);
        return toSongVos(fillFromHot(baseSongIds, limit), limit);
    }

    private List<Long> collectBaseSongIds(Long userId, int limit) {
        Set<Long> result = new HashSet<>();
        List<Long> recent = playHistoryService.getUserRecentPlays(userId, limit * 2);
        result.addAll(recent);
        List<Long> favoriteSongs = favoriteService.getUserFavoriteSongs(userId);
        result.addAll(favoriteSongs);
        return new ArrayList<>(result);
    }

    private List<Long> expandByArtistAlbum(List<Long> songIds, int limit) {
        if (songIds == null || songIds.isEmpty()) {
            return songIds;
        }
        List<Song> songs = songMapper.selectBatchIds(songIds);
        Set<Long> artistIds = songs.stream()
                .map(Song::getArtistId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> albumIds = songs.stream()
                .map(Song::getAlbumId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<Long> candidateIds = new HashSet<>(songIds);
        if (!artistIds.isEmpty()) {
            List<Song> artistSongs = songMapper.selectList(
                    Wrappers.<Song>lambdaQuery().in(Song::getArtistId, artistIds).last("limit " + limit * 4)
            );
            artistSongs.forEach(song -> candidateIds.add(song.getId()));
        }
        if (!albumIds.isEmpty()) {
            List<Song> albumSongs = songMapper.selectList(
                    Wrappers.<Song>lambdaQuery().in(Song::getAlbumId, albumIds).last("limit " + limit * 4)
            );
            albumSongs.forEach(song -> candidateIds.add(song.getId()));
        }
        return new ArrayList<>(candidateIds);
    }

    private List<Long> fillFromHot(List<Long> songIds, int limit) {
        if (songIds == null) {
            songIds = new ArrayList<>();
        }
        if (songIds.size() >= limit) {
            return songIds;
        }
        Page<AppSongVO> page = appMusicService.pageSongs(1, limit, null, null, null);
        List<Long> hotIds = page.getRecords().stream().map(AppSongVO::getId).collect(Collectors.toList());
        songIds.addAll(hotIds);
        return songIds;
    }

    private List<AppSongVO> toSongVos(List<Long> songIds, int limit) {
        if (songIds == null || songIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> trimmed = songIds.stream().distinct().limit(limit).collect(Collectors.toList());
        return appMusicService.getSongsByIds(trimmed);
    }
}
