package com.music.app.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.AlbumVO;
import com.music.api.vo.ArtistVO;
import com.music.api.vo.SongVO;
import com.music.app.service.AppMusicService;
import com.music.app.vo.AppAlbumVO;
import com.music.app.vo.AppArtistVO;
import com.music.app.vo.AppSongVO;
import com.music.app.vo.SearchSuggestionVO;
import com.music.common.utils.PinyinUtils;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.entity.SongArtist;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.AlbumService;
import com.music.content.service.ArtistService;
import com.music.content.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppMusicServiceImpl implements AppMusicService {

    private final SongService songService;
    private final AlbumService albumService;
    private final ArtistService artistService;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;
    private final SongArtistMapper songArtistMapper;

    @Override
    public Page<AppSongVO> pageSongs(Integer current, Integer size, String keyword, Long albumId, Long artistId) {
        SongQueryDTO query = new SongQueryDTO();
        query.setCurrent(current == null ? 1 : current);
        query.setSize(size == null ? 10 : size);
        query.setKeyword(keyword);
        query.setAlbumId(albumId);
        query.setArtistId(artistId);

        Page<SongVO> songPage = songService.pageList(query);
        Page<AppSongVO> result = new Page<>(songPage.getCurrent(), songPage.getSize(), songPage.getTotal());
        result.setRecords(songPage.getRecords().stream().map(this::toAppSongVO).toList());
        return result;
    }

    @Override
    public List<AppSongVO> hotSongs() {
        return songService.getHotSongs().stream().map(this::toAppSongVO).toList();
    }

    @Override
    public Page<AppAlbumVO> pageAlbums(Integer current, Integer size, String keyword, Long artistId) {
        Page<AlbumVO> page = albumService.pageList(keyword, artistId, current == null ? 1 : current, size == null ? 10 : size);
        Page<AppAlbumVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toAppAlbumVO).toList());
        return result;
    }

    @Override
    public Page<AppArtistVO> pageArtists(Integer current, Integer size, String keyword) {
        Page<ArtistVO> page = artistService.pageList(keyword, current == null ? 1 : current, size == null ? 10 : size);
        Page<AppArtistVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toAppArtistVO).toList());
        return result;
    }

    @Override
    public List<AppSongVO> getSongsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Song> songs = songService.listByIds(ids);
        // 按照传入的ID顺序返回
        return ids.stream()
                .map(id -> songs.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null))
                .filter(s -> s != null)
                .map(this::convertToAppSongVO)
                .toList();
    }

    private AppSongVO convertToAppSongVO(Song song) {
        AppSongVO vo = new AppSongVO();
        vo.setId(song.getId());
        vo.setTitle(song.getTitle());
        vo.setTitleEn(song.getTitleEn());
        vo.setAlbumId(song.getAlbumId());
        vo.setFilePath(song.getFilePath());
        vo.setHasLyrics(song.getHasLyrics());
        vo.setDuration(song.getDuration());
        vo.setPlayCount(song.getPlayCount());

        // 获取歌手名称（主唱 + 合唱歌手）
        if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
                vo.setArtistCover(artist.getAvatar());
            }
        }
        if (song.getArtistNames() != null && !song.getArtistNames().isEmpty()) {
            vo.setArtistNames(song.getArtistNames());
        } else if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                vo.setArtistNames(artist.getName());
            }
        }

        // 获取专辑名称和封面
        if (song.getAlbumId() != null) {
            Album album = albumMapper.selectById(song.getAlbumId());
            if (album != null) {
                vo.setAlbumName(album.getName());
                vo.setAlbumCover(album.getCoverImage());
            }
        }

        return vo;
    }

    private AppSongVO toAppSongVO(SongVO vo) {
        AppSongVO app = new AppSongVO();
        BeanUtils.copyProperties(vo, app);

        if (vo != null && vo.getArtistId() != null) {
            Artist artist = artistMapper.selectById(vo.getArtistId());
            if (artist != null) {
                app.setArtistCover(artist.getAvatar());
            }
        }

        return app;
    }

    private AppAlbumVO toAppAlbumVO(AlbumVO vo) {
        AppAlbumVO app = new AppAlbumVO();
        BeanUtils.copyProperties(vo, app);
        return app;
    }

    private AppArtistVO toAppArtistVO(ArtistVO vo) {
        AppArtistVO app = new AppArtistVO();
        BeanUtils.copyProperties(vo, app);
        return app;
    }

    @Override
    public AppAlbumVO getAlbumDetail(Long albumId) {
        AlbumVO albumVO = albumService.getDetail(albumId);
        if (albumVO == null) {
            return null;
        }
        AppAlbumVO app = new AppAlbumVO();
        BeanUtils.copyProperties(albumVO, app);
        return app;
    }

    @Override
    public AppArtistVO getArtistDetail(Long artistId) {
        ArtistVO artistVO = artistService.getDetail(artistId);
        if (artistVO == null) {
            return null;
        }
        AppArtistVO app = new AppArtistVO();
        BeanUtils.copyProperties(artistVO, app);
        return app;
    }
    
    @Override
    public List<AppSongVO> getArtistTopSongs(Long artistId, int limit) {
        if (artistId == null) return new ArrayList<>();

        LambdaQueryWrapper<SongArtist> saWrapper = new LambdaQueryWrapper<>();
        saWrapper.eq(SongArtist::getArtistId, artistId);
        List<Long> songIdsFromLink = songArtistMapper.selectList(saWrapper).stream()
                .map(SongArtist::getSongId)
                .distinct()
                .toList();

        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        // 与首页“热门歌手榜”的 songCount 口径保持一致：不过滤 status，避免出现“有歌曲数量但详情页为空”
        if (songIdsFromLink.isEmpty()) {
            wrapper.eq(Song::getArtistId, artistId);
        } else {
            wrapper.and(w -> w.eq(Song::getArtistId, artistId).or().in(Song::getId, songIdsFromLink));
        }

        wrapper.orderByDesc(Song::getPlayCount).last("LIMIT " + Math.min(limit, 50));
        List<Song> songs = songMapper.selectList(wrapper);
        return songs.stream().map(song -> {
            SongVO vo = songService.getDetail(song.getId());
            return toAppSongVO(vo);
        }).toList();
    }

    @Override
    public List<SearchSuggestionVO> getSuggestions(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String cleanKeyword = keyword.trim();
        List<SearchSuggestionVO> suggestions = new ArrayList<>();
        
        // 使用TreeMap按匹配得分排序
        java.util.Map<Integer, List<SearchSuggestionVO>> scoredResults = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        
        // 1. 搜索歌手
        List<Artist> artists = artistMapper.selectList(null);
        for (Artist artist : artists) {
            int score = PinyinUtils.fuzzyMatchScore(artist.getName(), cleanKeyword);
            if (score > 0) {
                SearchSuggestionVO suggestion = new SearchSuggestionVO();
                suggestion.setType(2); // 歌手
                suggestion.setId(artist.getId());
                suggestion.setName(artist.getName());
                suggestion.setCoverImage(artist.getAvatar());
                suggestion.setMatchedKeyword(cleanKeyword);
                
                scoredResults.computeIfAbsent(score, k -> new ArrayList<>()).add(suggestion);
            }
        }
        
        // 2. 搜索歌曲
        List<Song> songs = songService.list();
        for (Song song : songs) {
            int score = PinyinUtils.fuzzyMatchScore(song.getTitle(), cleanKeyword);
            if (score > 0) {
                SearchSuggestionVO suggestion = new SearchSuggestionVO();
                suggestion.setType(1); // 歌曲
                suggestion.setId(song.getId());
                suggestion.setName(song.getTitle());
                suggestion.setCoverImage(getAlbumCover(song.getAlbumId()));
                suggestion.setMatchedKeyword(cleanKeyword);
                
                // 获取歌手信息
                if (song.getArtistId() != null) {
                    Artist artist = artistMapper.selectById(song.getArtistId());
                    if (artist != null) {
                        suggestion.setArtistName(artist.getName());
                    }
                }
                
                // 获取专辑信息
                if (song.getAlbumId() != null) {
                    Album album = albumMapper.selectById(song.getAlbumId());
                    if (album != null) {
                        suggestion.setAlbumName(album.getName());
                    }
                }
                
                scoredResults.computeIfAbsent(score, k -> new ArrayList<>()).add(suggestion);
            }
        }
        
        // 3. 搜索专辑
        List<Album> albums = albumMapper.selectList(null);
        for (Album album : albums) {
            int score = PinyinUtils.fuzzyMatchScore(album.getName(), cleanKeyword);
            if (score > 0) {
                SearchSuggestionVO suggestion = new SearchSuggestionVO();
                suggestion.setType(3); // 专辑
                suggestion.setId(album.getId());
                suggestion.setName(album.getName());
                suggestion.setCoverImage(album.getCoverImage());
                suggestion.setMatchedKeyword(cleanKeyword);
                
                // 获取歌手信息
                if (album.getArtistId() != null) {
                    Artist artist = artistMapper.selectById(album.getArtistId());
                    if (artist != null) {
                        suggestion.setArtistName(artist.getName());
                    }
                }
                
                scoredResults.computeIfAbsent(score, k -> new ArrayList<>()).add(suggestion);
            }
        }
        
        // 按得分降序排列，取前limit个结果
        int count = 0;
        for (List<SearchSuggestionVO> resultList : scoredResults.values()) {
            for (SearchSuggestionVO suggestion : resultList) {
                if (count >= limit) break;
                suggestions.add(suggestion);
                count++;
            }
            if (count >= limit) break;
        }
        
        return suggestions;
    }
    
    /**
     * 获取专辑封面
     */
    private String getAlbumCover(Long albumId) {
        if (albumId == null) return null;
        Album album = albumMapper.selectById(albumId);
        return album != null ? album.getCoverImage() : null;
    }
}
