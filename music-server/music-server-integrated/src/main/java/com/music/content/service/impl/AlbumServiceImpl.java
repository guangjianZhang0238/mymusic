package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.AlbumDTO;
import com.music.api.vo.AlbumVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Album;
import com.music.content.entity.AlbumSong;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.AlbumSongMapper;
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
    private final AlbumSongMapper albumSongMapper;
    
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
            
            int songCount = countSongsInAlbum(album.getId());
            vo.setSongCount(songCount);
            
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
        
        int songCount = countSongsInAlbum(id);
        vo.setSongCount(songCount);
        
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
        int songCount = countSongsInAlbum(id);
        if (songCount > 0) {
            throw BusinessException.of("该专辑下存在歌曲，无法删除");
        }
        
        removeById(id);
    }

    @Override
    public void bindSongs(Long albumId, java.util.List<Long> songIds) {
        if (albumId == null) {
            throw BusinessException.of("专辑ID不能为空");
        }
        if (songIds == null || songIds.isEmpty()) {
            return;
        }

        Album album = getById(albumId);
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }

        // 查询已存在的关联，避免重复插入
        java.util.List<AlbumSong> existingLinks = albumSongMapper.selectList(
                new LambdaQueryWrapper<AlbumSong>()
                        .eq(AlbumSong::getAlbumId, albumId)
                        .in(AlbumSong::getSongId, songIds)
        );
        java.util.Set<Long> existingSongIds = new java.util.HashSet<>();
        for (AlbumSong link : existingLinks) {
            if (link.getSongId() != null) {
                existingSongIds.add(link.getSongId());
            }
        }

        // 为新收录的歌曲生成连续的 sortOrder
        Long count = albumSongMapper.selectCount(
                new LambdaQueryWrapper<AlbumSong>().eq(AlbumSong::getAlbumId, albumId)
        );
        int baseOrder = count != null ? count.intValue() : 0;
        int offset = 0;

        for (Long songId : songIds) {
            if (songId == null || existingSongIds.contains(songId)) {
                continue;
            }

            AlbumSong link = new AlbumSong();
            link.setAlbumId(albumId);
            link.setSongId(songId);
            link.setSortOrder(baseOrder + offset);
            offset++;
            albumSongMapper.insert(link);
        }
    }

    /**
     * 统计专辑下的歌曲数量（兼容旧的 content_song.album_id 字段和新的 content_album_song 多对多关联）
     */
    private int countSongsInAlbum(Long albumId) {
        if (albumId == null) {
            return 0;
        }
        java.util.Set<Long> songIds = new java.util.HashSet<>();

        // 旧模型：直接挂在 content_song.album_id 下的歌曲
        songMapper.selectList(new LambdaQueryWrapper<Song>()
                        .eq(Song::getAlbumId, albumId))
                .forEach(song -> {
                    if (song.getId() != null) {
                        songIds.add(song.getId());
                    }
                });

        // 新模型：content_album_song 多对多关联的歌曲
        albumSongMapper.selectList(new LambdaQueryWrapper<AlbumSong>()
                        .eq(AlbumSong::getAlbumId, albumId))
                .forEach(link -> {
                    if (link.getSongId() != null) {
                        songIds.add(link.getSongId());
                    }
                });

        return songIds.size();
    }
}
