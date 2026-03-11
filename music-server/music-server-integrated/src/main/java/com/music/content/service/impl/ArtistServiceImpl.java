package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.ArtistDTO;
import com.music.api.vo.ArtistVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.entity.SongArtist;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.ArtistService;
import com.music.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements ArtistService {
    
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;
    private final SongArtistMapper songArtistMapper;
    private final StorageConfig storageConfig;
    
    @Override
    public Page<ArtistVO> pageList(String keyword, int current, int size) {
        Page<Artist> page = new Page<>(current, size);
        LambdaQueryWrapper<Artist> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Artist::getName, keyword)
                   .or()
                   .like(Artist::getNameEn, keyword);
        }
        
        wrapper.orderByDesc(Artist::getCreateTime);
        
        Page<Artist> artistPage = page(page, wrapper);
        
        Page<ArtistVO> voPage = new Page<>(current, size, artistPage.getTotal());
        voPage.setRecords(artistPage.getRecords().stream().map(artist -> {
            ArtistVO vo = new ArtistVO();
            BeanUtils.copyProperties(artist, vo);
            
            // 直接返回原始头像路径，前端会在显示时添加/static/前缀
            // if (StringUtils.hasText(vo.getAvatar()) && !vo.getAvatar().startsWith("/")) {
            //     vo.setAvatar("/static/" + vo.getAvatar());
            // }
            
            Long albumCount = albumMapper.selectCount(new LambdaQueryWrapper<Album>()
                    .eq(Album::getArtistId, artist.getId()));
            int songCount = countSongsByArtist(artist.getId());
            
            vo.setAlbumCount(albumCount.intValue());
            vo.setSongCount(songCount);
            
            return vo;
        }).toList());
        
        return voPage;
    }
    
    @Override
    public ArtistVO getDetail(Long id) {
        Artist artist = getById(id);
        if (artist == null) {
            throw BusinessException.of("歌手不存在");
        }
        
        ArtistVO vo = new ArtistVO();
        BeanUtils.copyProperties(artist, vo);
        
        // 直接返回原始头像路径，前端会在显示时添加/static/前缀
        // if (StringUtils.hasText(vo.getAvatar()) && !vo.getAvatar().startsWith("/")) {
        //     vo.setAvatar("/static/" + vo.getAvatar());
        // }
        
        Long albumCount = albumMapper.selectCount(new LambdaQueryWrapper<Album>()
                .eq(Album::getArtistId, id));
        int songCount = countSongsByArtist(id);
        
        vo.setAlbumCount(albumCount.intValue());
        vo.setSongCount(songCount);
        
        return vo;
    }
    
    @Override
    public Long create(ArtistDTO dto) {
        Artist artist = new Artist();
        BeanUtils.copyProperties(dto, artist);
        if (artist.getStatus() == null) {
            artist.setStatus(1);
        }
        if (artist.getType() == null) {
            artist.setType(0);
        }
        if (artist.getSortOrder() == null) {
            artist.setSortOrder(0);
        }
        save(artist);
        return artist.getId();
    }
    
    @Override
    public void update(ArtistDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("歌手ID不能为空");
        }
        
        Artist artist = getById(dto.getId());
        if (artist == null) {
            throw BusinessException.of("歌手不存在");
        }
        
        if (dto.getName() != null) artist.setName(dto.getName());
        if (dto.getNameEn() != null) artist.setNameEn(dto.getNameEn());
        if (dto.getAvatar() != null) artist.setAvatar(dto.getAvatar());
        if (dto.getDescription() != null) artist.setDescription(dto.getDescription());
        if (dto.getRegion() != null) artist.setRegion(dto.getRegion());
        if (dto.getType() != null) artist.setType(dto.getType());
        if (dto.getSortOrder() != null) artist.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) artist.setStatus(dto.getStatus());
        
        updateById(artist);
    }
    
    @Override
    public void delete(Long id) {
        Long albumCount = albumMapper.selectCount(new LambdaQueryWrapper<Album>()
                .eq(Album::getArtistId, id));
        if (albumCount > 0) {
            throw BusinessException.of("该歌手下存在专辑，无法删除");
        }
        
        removeById(id);
    }
    
    @Override
    public Map<String, Object> scanArtists() {
        // 获取base-path路径
        String basePath = storageConfig.getBasePath();
        File baseDir = new File(basePath);
        
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw BusinessException.of("音乐库路径不存在或不是目录");
        }
        
        // 扫描结果
        int addedCount = 0;
        int skippedCount = 0;
        
        // 获取所有现有歌手的名称，用于快速查找
        Map<String, Artist> existingArtists = new HashMap<>();
        list().forEach(artist -> existingArtists.put(artist.getName(), artist));
        
        // 扫描base-path下的文件夹
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // 只处理文件夹，排除lyrics、.accelerate和upload文件夹
                if (file.isDirectory() && !"lyrics".equals(file.getName()) && !".accelerate".equals(file.getName()) && !"upload".equals(file.getName())) {
                    String artistName = file.getName();
                    
                    // 检查是否已存在该歌手
                    if (!existingArtists.containsKey(artistName)) {
                        // 创建新歌手
                        Artist artist = new Artist();
                        artist.setName(artistName);
                        artist.setNameEn(artistName);
                        artist.setStatus(1);
                        artist.setType(0);
                        artist.setSortOrder(0);
                        save(artist);
                        addedCount++;
                    } else {
                        skippedCount++;
                    }
                }
            }
        }
        
        // 返回扫描结果
        Map<String, Object> result = new HashMap<>();
        result.put("addedCount", addedCount);
        result.put("skippedCount", skippedCount);
        result.put("totalCount", addedCount + skippedCount);
        
        return result;
    }

    /** 统计歌手参与的歌曲数（主唱 + content_song_artist 中的合唱） */
    private int countSongsByArtist(Long artistId) {
        Set<Long> songIds = new HashSet<>();
        songMapper.selectList(new LambdaQueryWrapper<Song>().eq(Song::getArtistId, artistId))
                .forEach(s -> songIds.add(s.getId()));
        songArtistMapper.selectList(new LambdaQueryWrapper<SongArtist>().eq(SongArtist::getArtistId, artistId))
                .forEach(sa -> songIds.add(sa.getSongId()));
        return songIds.size();
    }
}
