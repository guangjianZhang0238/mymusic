package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.AlbumDTO;
import com.music.api.vo.AlbumVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {
    
    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;
    
    @Override
    public Page<AlbumVO> pageList(String keyword, Long artistId, int current, int size) {
        Page<Album> page = new Page<>(current, size);
        LambdaQueryWrapper<Album> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Album::getName, keyword);
        }
        if (artistId != null) {
            wrapper.eq(Album::getArtistId, artistId);
        }
        
        wrapper.orderByDesc(Album::getCreateTime);
        
        Page<Album> albumPage = page(page, wrapper);
        
        Page<AlbumVO> voPage = new Page<>(current, size, albumPage.getTotal());
        voPage.setRecords(albumPage.getRecords().stream().map(album -> {
            AlbumVO vo = new AlbumVO();
            BeanUtils.copyProperties(album, vo);
            
            Artist artist = artistMapper.selectById(album.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
            }
            
            Long songCount = songMapper.selectCount(new LambdaQueryWrapper<Song>()
                    .eq(Song::getAlbumId, album.getId()));
            vo.setSongCount(songCount.intValue());
            
            return vo;
        }).toList());
        
        return voPage;
    }
    
    @Override
    public AlbumVO getDetail(Long id) {
        Album album = getById(id);
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }
        
        AlbumVO vo = new AlbumVO();
        BeanUtils.copyProperties(album, vo);
        
        Artist artist = artistMapper.selectById(album.getArtistId());
        if (artist != null) {
            vo.setArtistName(artist.getName());
        }
        
        Long songCount = songMapper.selectCount(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id));
        vo.setSongCount(songCount.intValue());
        
        return vo;
    }
    
    @Override
    public Long create(AlbumDTO dto) {
        Artist artist = artistMapper.selectById(dto.getArtistId());
        if (artist == null) {
            throw BusinessException.of("歌手不存在");
        }
        
        Album album = new Album();
        BeanUtils.copyProperties(dto, album);
        if (album.getStatus() == null) {
            album.setStatus(1);
        }
        if (album.getAlbumType() == null) {
            album.setAlbumType(0);
        }
        if (album.getSortOrder() == null) {
            album.setSortOrder(0);
        }
        save(album);
        return album.getId();
    }
    
    @Override
    public void update(AlbumDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("专辑ID不能为空");
        }
        
        Album album = getById(dto.getId());
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }
        
        // 保存原始的folderPath，避免被覆盖
        String originalFolderPath = album.getFolderPath();
        
        if (dto.getArtistId() != null) album.setArtistId(dto.getArtistId());
        if (dto.getName() != null) album.setName(dto.getName());
        if (dto.getCoverImage() != null) album.setCoverImage(dto.getCoverImage());
        if (dto.getReleaseDate() != null) album.setReleaseDate(dto.getReleaseDate());
        if (dto.getDescription() != null) album.setDescription(dto.getDescription());
        if (dto.getAlbumType() != null) album.setAlbumType(dto.getAlbumType());
        if (dto.getSortOrder() != null) album.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) album.setStatus(dto.getStatus());
        
        // 恢复原始的folderPath
        album.setFolderPath(originalFolderPath);
        
        updateById(album);
    }
    
    @Override
    public void delete(Long id) {
        Long songCount = songMapper.selectCount(new LambdaQueryWrapper<Song>()
                .eq(Song::getAlbumId, id));
        if (songCount > 0) {
            throw BusinessException.of("该专辑下存在歌曲，无法删除");
        }
        
        removeById(id);
    }
}
