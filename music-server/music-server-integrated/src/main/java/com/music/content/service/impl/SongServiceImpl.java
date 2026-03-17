package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.BatchSongIdsDTO;
import com.music.api.dto.BatchSwitchArtistDTO;
import com.music.api.dto.SongDTO;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.BatchOperationResultVO;
import com.music.api.vo.BatchSwitchArtistResultVO;
import com.music.api.vo.SongVO;
import com.music.common.exception.BusinessException;
import com.music.common.utils.FileUtils;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.AlbumSong;
import com.music.content.entity.Song;
import com.music.content.entity.SongArtist;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.AlbumSongMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.LyricsService;
import com.music.content.service.SongService;
import com.music.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {
    
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final SongArtistMapper songArtistMapper;
    private final AlbumSongMapper albumSongMapper;
    private final LyricsService lyricsService;
    private final StorageConfig storageConfig;
    
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
            // 包含主歌手或合唱歌手的歌曲（content_song_artist 关联）
            wrapper.and(w -> w.eq(Song::getArtistId, query.getArtistId())
                    .or().inSql(Song::getId, "SELECT song_id FROM content_song_artist WHERE artist_id = " + query.getArtistId() + " AND deleted = 0"));
        }
        if (query.getAlbumId() != null) {
            // 支持：既包含挂在 content_song.album_id 下的歌曲，也包含通过 content_album_song 关联到该专辑的歌曲
            Long albumId = query.getAlbumId();
            wrapper.and(w -> w.eq(Song::getAlbumId, albumId)
                    .or()
                    .inSql(Song::getId,
                            "SELECT song_id FROM content_album_song WHERE album_id = "
                                    + albumId + " AND deleted = 0"));
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
    public BatchOperationResultVO batchDelete(BatchSongIdsDTO dto) {
        BatchOperationResultVO result = new BatchOperationResultVO();
        if (dto == null || dto.getSongIds() == null || dto.getSongIds().isEmpty()) {
            return result;
        }

        for (Long songId : dto.getSongIds()) {
            if (songId == null) {
                continue;
            }
            try {
                Song song = getById(songId);
                if (song == null) {
                    result.getSkipList().add(BatchOperationResultVO.SkipItem.of(songId, "歌曲不存在"));
                    continue;
                }

                // 1) 删除歌曲-歌手关联
                LambdaQueryWrapper<SongArtist> saWrapper = new LambdaQueryWrapper<>();
                saWrapper.eq(SongArtist::getSongId, songId);
                songArtistMapper.delete(saWrapper);

                // 2) 删除专辑-歌曲关联（多对多）
                LambdaQueryWrapper<AlbumSong> asWrapper = new LambdaQueryWrapper<>();
                asWrapper.eq(AlbumSong::getSongId, songId);
                albumSongMapper.delete(asWrapper);

                // 3) 删除歌曲主记录（歌词记录保留，由清理任务/歌词模块处理）
                removeById(songId);
                result.getSuccessList().add(songId);
            } catch (Exception e) {
                result.getSkipList().add(BatchOperationResultVO.SkipItem.of(songId, e.getMessage() == null ? "删除失败" : e.getMessage()));
            }
        }

        return result;
    }

    @Override
    public BatchSwitchArtistResultVO batchSwitchArtist(BatchSwitchArtistDTO dto) {
        BatchSwitchArtistResultVO result = new BatchSwitchArtistResultVO();
        if (dto == null || dto.getSongIds() == null || dto.getSongIds().isEmpty()) {
            return result;
        }
        if (dto.getTargetArtistId() == null) {
            throw BusinessException.of("目标歌手ID不能为空");
        }

        // 解析/创建目标专辑（空则默认专辑）。这里不依赖 MusicMetadataService，避免循环依赖。
        Album targetAlbum = getOrCreateAlbum(dto.getTargetArtistId(), dto.getTargetAlbumName());
        if (targetAlbum == null || !StringUtils.hasText(targetAlbum.getFolderPath())) {
            throw BusinessException.of("目标专辑目录路径为空");
        }

        for (Long songId : dto.getSongIds()) {
            if (songId == null) continue;

            try {
                Song song = getById(songId);
                if (song == null) {
                    result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, "歌曲不存在"));
                    continue;
                }
                String oldPath = song.getFilePath();
                if (!StringUtils.hasText(oldPath)) {
                    result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, "歌曲文件路径为空"));
                    continue;
                }

                File sourceFile = toAbsoluteFile(oldPath);
                if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
                    result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, "源文件不存在"));
                    continue;
                }

                String fileName = sourceFile.getName();
                String targetFolder = normalizeToSlash(targetAlbum.getFolderPath());
                String newRelativePath = targetFolder + "/" + fileName;

                File targetFile = new File(storageConfig.getBasePath(), newRelativePath.replace("/", File.separator));
                if (targetFile.exists()) {
                    result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, "目标路径已存在，已跳过"));
                    continue;
                }
                // 确保目标目录存在
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, "无法创建目标目录"));
                    continue;
                }

                // 移动文件
                moveFile(sourceFile.toPath(), targetFile.toPath());

                // 更新DB：主表 + 关系表
                song.setArtistId(dto.getTargetArtistId());
                song.setAlbumId(targetAlbum.getId());
                song.setFilePath(newRelativePath);
                updateById(song);

                // 专辑收录关系：改为只保留目标专辑
                albumSongMapper.delete(new LambdaQueryWrapper<AlbumSong>().eq(AlbumSong::getSongId, songId));
                AlbumSong link = new AlbumSong();
                link.setAlbumId(targetAlbum.getId());
                link.setSongId(songId);
                albumSongMapper.insert(link);

                result.getSuccessList().add(BatchSwitchArtistResultVO.SuccessItem.of(songId, oldPath, newRelativePath));
            } catch (Exception e) {
                result.getSkipList().add(BatchSwitchArtistResultVO.SkipItem.of(songId, e.getMessage() == null ? "切换失败" : e.getMessage()));
            }
        }

        return result;
    }

    private Album getOrCreateAlbum(Long artistId, String albumName) {
        if (artistId == null) {
            throw BusinessException.of("目标歌手ID不能为空");
        }
        String name = StringUtils.hasText(albumName) ? albumName.trim() : "默认";

        // 1) 精确匹配（同歌手下同名专辑）
        LambdaQueryWrapper<Album> exact = new LambdaQueryWrapper<>();
        exact.eq(Album::getArtistId, artistId).eq(Album::getName, name);
        Album existing = albumMapper.selectOne(exact);
        if (existing != null) {
            return existing;
        }

        // 2) 模糊匹配（去空格）
        String clean = name.replaceAll("\\s+", "");
        LambdaQueryWrapper<Album> fuzzy = new LambdaQueryWrapper<>();
        fuzzy.eq(Album::getArtistId, artistId).apply("REPLACE(name, ' ', '') = {0}", clean);
        existing = albumMapper.selectOne(fuzzy);
        if (existing != null) {
            return existing;
        }

        // 3) 创建新专辑 + 创建目录
        Artist artist = artistMapper.selectById(artistId);
        if (artist == null || !StringUtils.hasText(artist.getName())) {
            throw BusinessException.of("目标歌手不存在");
        }

        String folderPath = ensureAlbumDirectoryExists(artist.getName(), name);

        Album album = new Album();
        album.setArtistId(artistId);
        album.setName(name);
        album.setFolderPath(folderPath);
        album.setAlbumType(0);
        album.setSortOrder(0);
        album.setStatus(1);
        albumMapper.insert(album);
        return album;
    }

    private String ensureAlbumDirectoryExists(String artistName, String albumName) {
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }

        String artistDir = sanitizeDirectoryName(artistName);
        String albumDir = StringUtils.hasText(albumName) ? sanitizeDirectoryName(albumName) : "默认";
        File dir = new File(storageConfig.getBasePath(), artistDir + File.separator + albumDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw BusinessException.of("无法创建专辑目录: " + dir.getAbsolutePath());
        }

        // DB 中统一用 "/" 分隔
        return (artistDir + "/" + albumDir).replace("\\", "/");
    }

    private String sanitizeDirectoryName(String name) {
        if (!StringUtils.hasText(name)) {
            return "Unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private File toAbsoluteFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        File f = new File(filePath);
        if (f.isAbsolute()) {
            return f;
        }
        String rel = filePath.replace("/", File.separator).replace("\\", File.separator);
        return new File(storageConfig.getBasePath(), rel);
    }

    private String normalizeToSlash(String path) {
        if (!StringUtils.hasText(path)) return "";
        String p = path.replace("\\", "/");
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private void moveFile(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            // Windows/跨盘等可能不支持 ATOMIC_MOVE，降级
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
        Objects.requireNonNull(song, "song");
        SongVO vo = new SongVO();
        BeanUtils.copyProperties(song, vo);
        
        Artist artist = artistMapper.selectById(song.getArtistId());
        if (artist != null) {
            vo.setArtistName(artist.getName());
        }
        // 如果数据库中已经维护了多歌手名称，则优先使用聚合后的名称用于展示
        if (song.getArtistNames() != null && !song.getArtistNames().isEmpty()) {
            vo.setArtistNames(song.getArtistNames());
        } else if (artist != null) {
            vo.setArtistNames(artist.getName());
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
