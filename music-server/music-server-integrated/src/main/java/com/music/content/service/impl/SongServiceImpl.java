package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.SongDTO;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.SongVO;
import com.music.common.exception.BusinessException;
import com.music.common.utils.FileUtils;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.LyricsService;
import com.music.content.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {
    
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final LyricsService lyricsService;
    
    @Value("${lyrics.path}")
    private String lyricsApiPath;
    
    @Value("${lyrics.songName}")
    private String lyricsApiSongNameParam;
    
    @Value("${lyrics.singerName}")
    private String lyricsApiSingerNameParam;
    
    @Override
    public Page<SongVO> pageList(SongQueryDTO query) {
        Page<Song> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Song::getTitle, query.getKeyword())
                   .or()
                   .like(Song::getTitleEn, query.getKeyword());
        }
        if (query.getArtistId() != null) {
            wrapper.eq(Song::getArtistId, query.getArtistId());
        }
        if (query.getAlbumId() != null) {
            wrapper.eq(Song::getAlbumId, query.getAlbumId());
        }
        if (StringUtils.hasText(query.getFormat())) {
            wrapper.eq(Song::getFormat, query.getFormat());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Song::getStatus, query.getStatus());
        }
        if (query.getHasLyrics() != null) {
            if (query.getHasLyrics() == 1) {
                wrapper.eq(Song::getHasLyrics, 1);
            } else if (query.getHasLyrics() == 0) {
                wrapper.and(w -> w.eq(Song::getHasLyrics, 0).or().isNull(Song::getLyricsId));
            }
        }
        
        wrapper.orderByAsc(Song::getDiscNumber)
               .orderByAsc(Song::getTrackNumber)
               .orderByDesc(Song::getCreateTime);
        
        Page<Song> songPage = page(page, wrapper);
        
        Page<SongVO> voPage = new Page<>(query.getCurrent(), query.getSize(), songPage.getTotal());
        voPage.setRecords(songPage.getRecords().stream().map(this::convertToVO).toList());
        
        return voPage;
    }
    
    @Override
    public SongVO getDetail(Long id) {
        Song song = getById(id);
        if (song == null) {
            throw BusinessException.of("歌曲不存在");
        }
        return convertToVO(song);
    }
    
    @Override
    public Long create(SongDTO dto) {
        Album album = albumMapper.selectById(dto.getAlbumId());
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }
        
        Artist artist = artistMapper.selectById(dto.getArtistId());
        if (artist == null) {
            throw BusinessException.of("歌手不存在");
        }
        
        Song song = new Song();
        BeanUtils.copyProperties(dto, song);
        if (song.getStatus() == null) {
            song.setStatus(1);
        }
        if (song.getSortOrder() == null) {
            song.setSortOrder(0);
        }
        if (song.getPlayCount() == null) {
            song.setPlayCount(0);
        }
        if (song.getHasLyrics() == null) {
            song.setHasLyrics(0);
        }
        if (song.getDiscNumber() == null) {
            song.setDiscNumber(1);
        }
        save(song);
        return song.getId();
    }
    
    @Override
    public void update(SongDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("歌曲ID不能为空");
        }
        
        Song song = getById(dto.getId());
        if (song == null) {
            throw BusinessException.of("歌曲不存在");
        }
        
        if (dto.getAlbumId() != null) song.setAlbumId(dto.getAlbumId());
        if (dto.getArtistId() != null) song.setArtistId(dto.getArtistId());
        if (dto.getTitle() != null) song.setTitle(dto.getTitle());
        if (dto.getTitleEn() != null) song.setTitleEn(dto.getTitleEn());
        if (dto.getFilePath() != null) song.setFilePath(dto.getFilePath());
        if (dto.getTrackNumber() != null) song.setTrackNumber(dto.getTrackNumber());
        if (dto.getDiscNumber() != null) song.setDiscNumber(dto.getDiscNumber());
        if (dto.getSortOrder() != null) song.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) song.setStatus(dto.getStatus());
        
        updateById(song);
    }
    
    @Override
    public void delete(Long id) {
        removeById(id);
    }
    
    @Override
    public void incrementPlayCount(Long id) {
        Song song = getById(id);
        if (song != null) {
            song.setPlayCount(song.getPlayCount() + 1);
            updateById(song);
        }
    }
    
    @Override
    public List<SongVO> getHotSongs() {
        // 获取所有启用状态的歌曲
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1);
        
        List<Song> allSongs = list(wrapper);
        
        // 智能推荐算法：
        // 1. 按播放次数权重排序
        // 2. 同一歌手最多保留2首歌曲
        // 3. 随机化处理增加多样性
        
        Map<Long, List<Song>> songsByArtist = allSongs.stream()
                .collect(java.util.stream.Collectors.groupingBy(Song::getArtistId));
        
        List<Song> recommendedSongs = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        // 为每个歌手随机选择1-2首歌曲
        for (Map.Entry<Long, List<Song>> entry : songsByArtist.entrySet()) {
            List<Song> artistSongs = entry.getValue();
            // 按播放次数排序
            artistSongs.sort((s1, s2) -> Long.compare(s2.getPlayCount(), s1.getPlayCount()));
            
            // 随机决定选择1首还是2首（70%概率选1首，30%概率选2首）
            int maxSongs = random.nextDouble() < 0.7 ? 1 : 2;
            int songsToAdd = Math.min(maxSongs, artistSongs.size());
            
            for (int i = 0; i < songsToAdd; i++) {
                recommendedSongs.add(artistSongs.get(i));
            }
        }
        
        // 如果推荐歌曲不足20首，补充一些随机歌曲
        if (recommendedSongs.size() < 20) {
            List<Song> remainingSongs = allSongs.stream()
                    .filter(song -> !recommendedSongs.contains(song))
                    .collect(java.util.stream.Collectors.toList());
            
            // 随机打乱剩余歌曲
            java.util.Collections.shuffle(remainingSongs, random);
            
            int songsNeeded = 20 - recommendedSongs.size();
            int songsToAdd = Math.min(songsNeeded, remainingSongs.size());
            
            recommendedSongs.addAll(remainingSongs.subList(0, songsToAdd));
        }
        
        // 最终随机打乱推荐列表
        java.util.Collections.shuffle(recommendedSongs, random);
        
        // 限制返回数量为20首
        int finalSize = Math.min(20, recommendedSongs.size());
        List<Song> result = new java.util.ArrayList<>();
        for (int i = 0; i < finalSize; i++) {
            result.add(recommendedSongs.get(i));
        }
        
        return result.stream().map(this::convertToVO).toList();
    }
    
    @Override
    public java.util.Map<String, Object> autoMatchLyrics(Long songId) {
        Song song = getById(songId);
        if (song == null) {
            throw BusinessException.of("歌曲不存在");
        }
        
        // 获取歌曲名称和歌手名称
        String songName = song.getTitle();
        String artistName = null;
        
        if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                artistName = artist.getName();
            }
        }
        
        if (!StringUtils.hasText(songName) || !StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌曲名称或歌手名称不能为空");
        }
        
        // 调用歌词服务的自动匹配功能
        try {
            // 构建歌词API请求URL
            String apiUrl = lyricsApiPath + "?" + lyricsApiSongNameParam + "=" + songName + "&" + lyricsApiSingerNameParam + "=" + artistName;
            // 调用歌词服务的自动匹配功能
            Map<String, Object> result = lyricsService.autoMatchLyricsFromApi(apiUrl, songId);
            
            // 更新歌曲的歌词信息
            song.setHasLyrics(1);
            song.setLyricsId(result.get("lyricsId") != null ? Long.parseLong(result.get("lyricsId").toString()) : null);
            updateById(song);
            
            return result;
        } catch (Exception e) {
            throw BusinessException.of("歌词匹配失败: " + e.getMessage());
        }
    }
    
    private SongVO convertToVO(Song song) {
        SongVO vo = new SongVO();
        BeanUtils.copyProperties(song, vo);
        
        Artist artist = artistMapper.selectById(song.getArtistId());
        if (artist != null) {
            vo.setArtistName(artist.getName());
        }
        
        Album album = albumMapper.selectById(song.getAlbumId());
        if (album != null) {
            vo.setAlbumName(album.getName());
        }
        
        if (song.getFileSize() != null) {
            vo.setFileSizeFormat(FileUtils.formatFileSize(song.getFileSize()));
        }
        if (song.getDuration() != null) {
            vo.setDurationFormat(FileUtils.formatDuration(song.getDuration()));
        }
        
        // 根据数据库中的歌词记录设置hasLyrics字段
        vo.setHasLyrics(lyricsService.hasLyrics(song.getId()) ? 1 : 0);
        
        return vo;
    }
}
