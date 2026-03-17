package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.AlbumDTO;
import com.music.api.dto.SwitchAlbumArtistDTO;
import com.music.api.vo.AlbumVO;
import com.music.api.vo.SwitchAlbumArtistResultVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Album;
import com.music.content.entity.AlbumSong;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.entity.SongArtist;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.AlbumSongMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.AlbumService;
import com.music.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {
    
    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;
    private final AlbumSongMapper albumSongMapper;
    private final SongArtistMapper songArtistMapper;
    private final StorageConfig storageConfig;
    
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
            Objects.requireNonNull(album, "album");
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

    @Override
    public SwitchAlbumArtistResultVO switchArtist(Long albumId, SwitchAlbumArtistDTO dto) {
        if (albumId == null) {
            throw BusinessException.of("专辑ID不能为空");
        }
        if (dto == null || dto.getTargetArtistId() == null) {
            throw BusinessException.of("目标歌手ID不能为空");
        }

        Album album = getById(albumId);
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }
        if (!StringUtils.hasText(album.getFolderPath())) {
            return SwitchAlbumArtistResultVO.fail(albumId, "专辑folderPath为空，无法迁移文件夹");
        }

        String oldFolder = normalizeToSlash(album.getFolderPath());

        // 目标歌手：优先使用ID，名称仅用于创建目录（若为空则从库中取）
        Artist targetArtist = artistMapper.selectById(dto.getTargetArtistId());
        if (targetArtist == null) {
            throw BusinessException.of("目标歌手不存在");
        }

        // 目标专辑目录（同名专辑）
        // 注意：这里不能提前创建目录，否则后续 targetDir.exists() 永远为 true
        String newFolder = buildAlbumFolderPath(targetArtist.getName(), album.getName());

        File sourceDir = new File(storageConfig.getBasePath(), oldFolder.replace("/", File.separator));
        File targetDir = new File(storageConfig.getBasePath(), newFolder.replace("/", File.separator));

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return SwitchAlbumArtistResultVO.fail(albumId, "源专辑目录不存在");
        }
        if (targetDir.exists()) {
            // 兼容历史失败导致的“空目录残留”
            if (targetDir.isDirectory() && isDirectoryEmpty(targetDir.toPath())) {
                try {
                    Files.delete(targetDir.toPath());
                } catch (Exception e) {
                    return SwitchAlbumArtistResultVO.fail(albumId, "目标专辑目录已存在且为空，但删除失败");
                }
            } else {
                return SwitchAlbumArtistResultVO.fail(albumId, "目标专辑目录已存在，已跳过");
            }
        }

        // 确保目标父目录存在
        File targetParent = targetDir.getParentFile();
        if (targetParent != null && !targetParent.exists() && !targetParent.mkdirs()) {
            return SwitchAlbumArtistResultVO.fail(albumId, "无法创建目标父目录");
        }

        try {
            moveDir(sourceDir.toPath(), targetDir.toPath());
        } catch (Exception e) {
            return SwitchAlbumArtistResultVO.fail(albumId, e.getMessage() == null ? "迁移目录失败" : e.getMessage());
        }

        // 更新专辑：artistId + folderPath
        album.setArtistId(dto.getTargetArtistId());
        album.setFolderPath(newFolder);

        // 同步封面路径（如果使用folderPath前缀）
        if (StringUtils.hasText(album.getCoverImage())) {
            String cover = album.getCoverImage().replace("\\", "/");
            if (cover.startsWith(oldFolder + "/")) {
                album.setCoverImage(newFolder + cover.substring(oldFolder.length()));
            }
        }
        updateById(album);

        // 同步该专辑下歌曲的 file_path（兼容旧模型 album_id 与新模型 album_song）
        java.util.Set<Long> songIds = new java.util.HashSet<>();
        songMapper.selectList(new LambdaQueryWrapper<Song>().eq(Song::getAlbumId, albumId))
                .forEach(s -> { if (s.getId() != null) songIds.add(s.getId()); });
        albumSongMapper.selectList(new LambdaQueryWrapper<AlbumSong>().eq(AlbumSong::getAlbumId, albumId))
                .forEach(link -> { if (link.getSongId() != null) songIds.add(link.getSongId()); });

        for (Long sid : songIds) {
            Song song = songMapper.selectById(sid);
            if (song == null || !StringUtils.hasText(song.getFilePath())) continue;
            String fp = song.getFilePath().replace("\\", "/");
            if (fp.startsWith(oldFolder + "/")) {
                song.setFilePath(newFolder + fp.substring(oldFolder.length()));
            }

            // 同步歌曲所属歌手，确保 App 端展示为新歌手
            song.setArtistId(dto.getTargetArtistId());

            // 同步 content_song_artist：只替换主歌手(sortOrder=0)，保留合唱等其它关联
            try {
                songArtistMapper.delete(new LambdaQueryWrapper<SongArtist>()
                        .eq(SongArtist::getSongId, sid)
                        .eq(SongArtist::getSortOrder, 0));
                SongArtist main = new SongArtist();
                main.setSongId(sid);
                main.setArtistId(dto.getTargetArtistId());
                main.setSortOrder(0);
                songArtistMapper.insert(main);

                // 维护 artistNames：新主歌手 + 现有其它关联歌手
                java.util.List<SongArtist> others = songArtistMapper.selectList(
                        new LambdaQueryWrapper<SongArtist>()
                                .eq(SongArtist::getSongId, sid)
                                .ne(SongArtist::getSortOrder, 0)
                                .orderByAsc(SongArtist::getSortOrder)
                );
                java.util.List<String> names = new java.util.ArrayList<>();
                names.add(targetArtist.getName());
                for (SongArtist sa : others) {
                    if (sa.getArtistId() == null) continue;
                    Artist a = artistMapper.selectById(sa.getArtistId());
                    if (a != null && StringUtils.hasText(a.getName())) {
                        names.add(a.getName());
                    }
                }
                song.setArtistNames(String.join(" / ", names));
            } catch (Exception e) {
                // 关联表同步失败不影响主流程：至少保证 song.artist_id 已更新
            }

            songMapper.updateById(song);
        }

        return SwitchAlbumArtistResultVO.ok(albumId, oldFolder, newFolder);
    }

    private String buildAlbumFolderPath(String artistName, String albumName) {
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        String artistDir = sanitizeDirectoryName(artistName);
        String albumDir = StringUtils.hasText(albumName) ? sanitizeDirectoryName(albumName) : "默认";
        return (artistDir + "/" + albumDir).replace("\\", "/");
    }

    private String sanitizeDirectoryName(String name) {
        if (!StringUtils.hasText(name)) {
            return "Unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private boolean isDirectoryEmpty(Path dir) {
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream.findFirst().isEmpty();
        } catch (Exception e) {
            // 无法判断时，按“非空”处理，避免误删
            return false;
        }
    }

    private String normalizeToSlash(String path) {
        if (!StringUtils.hasText(path)) return "";
        String p = path.replace("\\", "/");
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private void moveDir(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(source, target);
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
