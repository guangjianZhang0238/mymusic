package com.music.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.music.api.dto.ArtistDTO;
import com.music.api.dto.AlbumDTO;
import com.music.api.vo.ArtistVO;
import com.music.api.vo.AlbumVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Artist;
import com.music.content.entity.Album;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.AlbumMapper;
import com.music.content.service.ArtistService;
import com.music.content.service.AlbumService;
import com.music.file.config.StorageConfig;
import com.music.file.service.MusicMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 音乐元数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicMetadataServiceImpl implements MusicMetadataService {
    
    private final ArtistService artistService;
    private final AlbumService albumService;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final StorageConfig storageConfig;
    
    @Override
    public List<ArtistVO> searchArtists(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        
        LambdaQueryWrapper<Artist> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Artist::getName, keyword)
               .or()
               .like(Artist::getNameEn, keyword)
               .orderByAsc(Artist::getName)
               .last("LIMIT " + limit);
        
        return artistMapper.selectList(wrapper).stream()
                .map(this::convertToArtistVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AlbumVO> searchAlbums(Long artistId, String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(artistId != null, Album::getArtistId, artistId)
               .like(Album::getName, keyword)
               .orderByAsc(Album::getName)
               .last("LIMIT " + limit);
        
        return albumMapper.selectList(wrapper).stream()
                .map(this::convertToAlbumVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ArtistVO autoMatchOrCreateArtist(String artistName) {
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        
        // 先尝试精确匹配
        LambdaQueryWrapper<Artist> exactWrapper = new LambdaQueryWrapper<>();
        exactWrapper.eq(Artist::getName, artistName.trim());
        Artist existingArtist = artistMapper.selectOne(exactWrapper);
        
        if (existingArtist != null) {
            log.info("找到匹配的歌手: {} (ID: {})", artistName, existingArtist.getId());
            return convertToArtistVO(existingArtist);
        }
        
        // 再尝试模糊匹配（去除空格后比较）
        String cleanName = artistName.trim().replaceAll("\\s+", "");
        LambdaQueryWrapper<Artist> fuzzyWrapper = new LambdaQueryWrapper<>();
        fuzzyWrapper.apply("REPLACE(name, ' ', '') = {0}", cleanName);
        existingArtist = artistMapper.selectOne(fuzzyWrapper);
        
        if (existingArtist != null) {
            log.info("找到模糊匹配的歌手: {} (ID: {})", artistName, existingArtist.getId());
            return convertToArtistVO(existingArtist);
        }
        
        // 创建新歌手
        log.info("未找到匹配歌手，创建新歌手: {}", artistName);
        ArtistDTO artistDTO = new ArtistDTO();
        artistDTO.setName(artistName.trim());
        artistDTO.setType(0); // 默认个人歌手
        artistDTO.setStatus(1); // 默认启用
        
        Long artistId = artistService.create(artistDTO);
        Artist newArtist = artistService.getById(artistId);
        
        // 创建歌手目录
        ensureArtistDirectoryExists(artistName.trim());
        
        return convertToArtistVO(newArtist);
    }
    
    @Override
    public AlbumVO autoMatchOrCreateAlbum(Long artistId, String albumName) {
        if (artistId == null) {
            throw BusinessException.of("歌手ID不能为空");
        }
        
        if (!StringUtils.hasText(albumName)) {
            // 如果专辑名为空，使用默认专辑
            return getDefaultAlbum(artistId);
        }
        
        // 先尝试精确匹配
        LambdaQueryWrapper<Album> exactWrapper = new LambdaQueryWrapper<>();
        exactWrapper.eq(Album::getArtistId, artistId)
                   .eq(Album::getName, albumName.trim());
        Album existingAlbum = albumMapper.selectOne(exactWrapper);
        
        if (existingAlbum != null) {
            log.info("找到匹配的专辑: {} (ID: {})", albumName, existingAlbum.getId());
            return convertToAlbumVO(existingAlbum);
        }
        
        // 再尝试模糊匹配
        String cleanName = albumName.trim().replaceAll("\\s+", "");
        LambdaQueryWrapper<Album> fuzzyWrapper = new LambdaQueryWrapper<>();
        fuzzyWrapper.eq(Album::getArtistId, artistId)
                   .apply("REPLACE(name, ' ', '') = {0}", cleanName);
        existingAlbum = albumMapper.selectOne(fuzzyWrapper);
        
        if (existingAlbum != null) {
            log.info("找到模糊匹配的专辑: {} (ID: {})", albumName, existingAlbum.getId());
            return convertToAlbumVO(existingAlbum);
        }
        
        // 创建新专辑
        log.info("未找到匹配专辑，创建新专辑: {} for artistId: {}", albumName, artistId);
        AlbumDTO albumDTO = new AlbumDTO();
        albumDTO.setArtistId(artistId);
        albumDTO.setName(albumName.trim());
        albumDTO.setAlbumType(0); // 默认专辑类型
        albumDTO.setStatus(1); // 默认启用

        // 先创建目录并写入folderPath，避免数据库中路径为空
        Artist artist = artistService.getById(artistId);
        if (artist != null) {
            String folderPath = ensureAlbumDirectoryExists(artist.getName(), albumName.trim());
            albumDTO.setFolderPath(folderPath);
        }

        Long albumId = albumService.create(albumDTO);
        Album newAlbum = albumService.getById(albumId);

        return convertToAlbumVO(newAlbum);
    }
    
    @Override
    public String ensureArtistDirectoryExists(String artistName) {
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        
        String basePath = storageConfig.getBasePath();
        String artistDirName = sanitizeDirectoryName(artistName);
        String artistPath = basePath + File.separator + artistDirName;
        
        File artistDir = new File(artistPath);
        if (!artistDir.exists()) {
            if (!artistDir.mkdirs()) {
                throw BusinessException.of("无法创建歌手目录: " + artistPath);
            }
            log.info("创建歌手目录: {}", artistPath);
        }
        
        return artistDirName;
    }
    
    @Override
    public String ensureAlbumDirectoryExists(String artistName, String albumName) {
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        
        String basePath = storageConfig.getBasePath();
        String artistDirName = sanitizeDirectoryName(artistName);
        String albumDirName = StringUtils.hasText(albumName) ? 
            sanitizeDirectoryName(albumName) : "默认";
        
        String albumPath = basePath + File.separator + artistDirName + File.separator + albumDirName;
        
        File albumDir = new File(albumPath);
        if (!albumDir.exists()) {
            if (!albumDir.mkdirs()) {
                throw BusinessException.of("无法创建专辑目录: " + albumPath);
            }
            log.info("创建专辑目录: {}", albumPath);
        }
        
        return artistDirName + File.separator + albumDirName;
    }
    
    /**
     * 获取默认专辑
     */
    private AlbumVO getDefaultAlbum(Long artistId) {
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Album::getArtistId, artistId)
               .eq(Album::getName, "默认");
        
        Album defaultAlbum = albumMapper.selectOne(wrapper);
        if (defaultAlbum != null) {
            return convertToAlbumVO(defaultAlbum);
        }
        
        // 创建默认专辑
        AlbumDTO albumDTO = new AlbumDTO();
        albumDTO.setArtistId(artistId);
        albumDTO.setName("默认");
        albumDTO.setAlbumType(0);
        albumDTO.setStatus(1);
        
        // 先创建默认专辑目录并写入folderPath
        Artist artist = artistService.getById(artistId);
        if (artist != null) {
            String folderPath = ensureAlbumDirectoryExists(artist.getName(), "默认");
            albumDTO.setFolderPath(folderPath);
        }

        Long albumId = albumService.create(albumDTO);
        Album newAlbum = albumService.getById(albumId);

        return convertToAlbumVO(newAlbum);
    }
    
    /**
     * 清理目录名称，移除非法字符
     */
    private String sanitizeDirectoryName(String name) {
        if (!StringUtils.hasText(name)) {
            return "Unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
    
    private ArtistVO convertToArtistVO(Artist artist) {
        ArtistVO vo = new ArtistVO();
        BeanUtils.copyProperties(artist, vo);
        return vo;
    }
    
    private AlbumVO convertToAlbumVO(Album album) {
        AlbumVO vo = new AlbumVO();
        BeanUtils.copyProperties(album, vo);
        
        // 设置歌手名称
        if (album.getArtistId() != null) {
            Artist artist = artistService.getById(album.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
            }
        }
        
        return vo;
    }
}