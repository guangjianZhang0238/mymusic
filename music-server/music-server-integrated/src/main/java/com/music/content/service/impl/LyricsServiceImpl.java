package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.LyricsDTO;
import com.music.api.vo.LyricsVO;
import com.music.common.exception.BusinessException;
import com.music.common.utils.FileUtils;
import com.music.content.entity.Lyrics;
import com.music.content.entity.Song;
import com.music.content.entity.Artist;
import com.music.content.entity.Album;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.LyricsMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.LyricsService;
import com.music.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class LyricsServiceImpl extends ServiceImpl<LyricsMapper, Lyrics> implements LyricsService {
    
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final StorageConfig storageConfig;
    private final RestTemplate restTemplate;
    private final com.music.content.config.LyricsConfig lyricsConfig;
    private final com.music.content.config.Lyrics2Config lyrics2Config;
    
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)?");
    private static final Pattern LRC_PATTERN_WITHOUT_TEXT = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]");
    private static final String LYRICS_FOLDER = "lyrics";
    
    @Override
    public Page<LyricsVO> pageList(String keyword, Long songId, int current, int size) {
        Page<Lyrics> page = new Page<>(current, size);
        
        // 构建查询条件
        LambdaQueryWrapper<Lyrics> wrapper = new LambdaQueryWrapper<>();
        
        if (songId != null) {
            wrapper.eq(Lyrics::getSongId, songId);
        }
        
        // 如果有搜索关键词，构建复杂的模糊搜索条件
        if (StringUtils.hasText(keyword)) {
            // 使用子查询来实现跨表模糊搜索
            wrapper.and(w -> {
                // 1. 搜索歌词内容
                w.like(Lyrics::getContent, keyword);
                
                // 2. 搜索歌曲名称 - 通过子查询
                List<Long> songIdsByTitle = songMapper.selectList(new LambdaQueryWrapper<Song>()
                        .like(Song::getTitle, keyword))
                        .stream().map(Song::getId).toList();
                if (!songIdsByTitle.isEmpty()) {
                    w.or().in(Lyrics::getSongId, songIdsByTitle);
                }
                
                // 3. 搜索歌手名称 - 通过子查询
                List<Long> artistIdsByName = artistMapper.selectList(new LambdaQueryWrapper<Artist>()
                        .like(Artist::getName, keyword))
                        .stream().map(Artist::getId).toList();
                if (!artistIdsByName.isEmpty()) {
                    List<Long> songIdsByArtist = songMapper.selectList(new LambdaQueryWrapper<Song>()
                            .in(Song::getArtistId, artistIdsByName))
                            .stream().map(Song::getId).toList();
                    if (!songIdsByArtist.isEmpty()) {
                        w.or().in(Lyrics::getSongId, songIdsByArtist);
                    }
                }
                
                // 4. 搜索专辑名称 - 通过子查询
                List<Long> albumIdsByName = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                        .like(Album::getName, keyword))
                        .stream().map(Album::getId).toList();
                if (!albumIdsByName.isEmpty()) {
                    List<Long> songIdsByAlbum = songMapper.selectList(new LambdaQueryWrapper<Song>()
                            .in(Song::getAlbumId, albumIdsByName))
                            .stream().map(Song::getId).toList();
                    if (!songIdsByAlbum.isEmpty()) {
                        w.or().in(Lyrics::getSongId, songIdsByAlbum);
                    }
                }
            });
        }
        
        Page<Lyrics> lyricsPage = page(page, wrapper);
        
        Page<LyricsVO> voPage = new Page<>(current, size, lyricsPage.getTotal());
        voPage.setRecords(lyricsPage.getRecords().stream().map(lyrics -> {
            LyricsVO vo = new LyricsVO();
            BeanUtils.copyProperties(lyrics, vo);
            
            Song song = songMapper.selectById(lyrics.getSongId());
            if (song != null) {
                vo.setSongTitle(song.getTitle());
                
                // 获取歌手名称
                if (song.getArtistId() != null) {
                    Artist artist = artistMapper.selectById(song.getArtistId());
                    if (artist != null) {
                        vo.setArtistName(artist.getName());
                    }
                }
                
                // 获取专辑名称
                if (song.getAlbumId() != null) {
                    Album album = albumMapper.selectById(song.getAlbumId());
                    if (album != null) {
                        vo.setAlbumName(album.getName());
                    }
                }
            }
            
            return vo;
        }).toList());
        
        return voPage;
    }
    
    @Override
    public LyricsVO getBySongId(Long songId) {
        Lyrics lyrics = getOne(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, songId));
        
        if (lyrics == null) {
            return null;
        }
        
        LyricsVO vo = new LyricsVO();
        BeanUtils.copyProperties(lyrics, vo);
        
        Song song = songMapper.selectById(songId);
        if (song != null) {
            vo.setSongTitle(song.getTitle());
            
            // 获取歌手名称
            if (song.getArtistId() != null) {
                Artist artist = artistMapper.selectById(song.getArtistId());
                if (artist != null) {
                    vo.setArtistName(artist.getName());
                }
            }
            
            // 获取专辑名称
            if (song.getAlbumId() != null) {
                Album album = albumMapper.selectById(song.getAlbumId());
                if (album != null) {
                    vo.setAlbumName(album.getName());
                }
            }
        }
        
        if (lyrics.getLyricsType() == 1 && lyrics.getContent() != null) {
            vo.setLines(parseLrc(lyrics.getContent()));
        }
        
        return vo;
    }
    
    @Override
    public Long create(LyricsDTO dto) {
        Song song = songMapper.selectById(dto.getSongId());
        if (song == null) {
            throw BusinessException.of("歌曲不存在");
        }
        
        // 查找是否已有该歌曲的歌词
        Lyrics existing = getOne(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, dto.getSongId()));
        
        if (existing != null) {
            // 如果已有歌词，则更新
            if (dto.getLyricsType() != null) existing.setLyricsType(dto.getLyricsType());
            if (dto.getContent() != null) existing.setContent(dto.getContent());
            if (dto.getTranslation() != null) existing.setTranslation(dto.getTranslation());
            if (dto.getSource() != null) existing.setSource(dto.getSource());
            if (dto.getSourceUrl() != null) existing.setSourceUrl(dto.getSourceUrl());
            if (dto.getFilePath() != null) existing.setFilePath(dto.getFilePath());
            if (dto.getLyricsOffset() != null) existing.setLyricsOffset(dto.getLyricsOffset());
            
            updateById(existing);

            song.setHasLyrics(1);
            song.setLyricsId(existing.getId());
            songMapper.updateById(song);
            return existing.getId();
        } else {
            // 如果没有歌词，则创建
            Lyrics lyrics = new Lyrics();
            BeanUtils.copyProperties(dto, lyrics);
            if (lyrics.getLyricsType() == null) {
                lyrics.setLyricsType(0);
            }
            if (lyrics.getLyricsOffset() == null) {
                lyrics.setLyricsOffset(0.0);
            }
            save(lyrics);
            
            song.setHasLyrics(1);
            song.setLyricsId(lyrics.getId());
            songMapper.updateById(song);
            
            return lyrics.getId();
        }
    }

    @Override
    public void update(LyricsDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of("歌词ID不能为空");
        }
        
        Lyrics lyrics = getById(dto.getId());
        if (lyrics == null) {
            throw BusinessException.of("歌词不存在");
        }
        
        if (dto.getLyricsType() != null) lyrics.setLyricsType(dto.getLyricsType());
        if (dto.getContent() != null) lyrics.setContent(dto.getContent());
        if (dto.getTranslation() != null) lyrics.setTranslation(dto.getTranslation());
        if (dto.getSource() != null) lyrics.setSource(dto.getSource());
        if (dto.getSourceUrl() != null) lyrics.setSourceUrl(dto.getSourceUrl());
        if (dto.getFilePath() != null) lyrics.setFilePath(dto.getFilePath());
        if (dto.getLyricsOffset() != null) lyrics.setLyricsOffset(dto.getLyricsOffset());
        
        updateById(lyrics);
    }
    
    @Override
    public void delete(Long id) {
        Lyrics lyrics = getById(id);
        if (lyrics != null) {
            Song song = songMapper.selectById(lyrics.getSongId());
            if (song != null) {
                song.setHasLyrics(0);
                song.setLyricsId(null);
                songMapper.updateById(song);
            }
        }
        removeById(id);
    }
    
    @Override
    public List<LyricsVO.LyricsLine> parseLrc(String content) {
        List<LyricsVO.LyricsLine> lines = new ArrayList<>();
        
        for (String line : content.split("\n")) {
            Matcher matcher = LRC_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                String millisStr = matcher.group(3);
                int millis = millisStr.length() == 2 
                        ? Integer.parseInt(millisStr) * 10 
                        : Integer.parseInt(millisStr);
                String text = matcher.group(4).trim();
                
                long time = minutes * 60 * 1000L + seconds * 1000L + millis;
                
                LyricsVO.LyricsLine lyricsLine = new LyricsVO.LyricsLine();
                lyricsLine.setTime(time);
                lyricsLine.setText(text);
                lines.add(lyricsLine);
            }
        }
        
        lines.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));
        return lines;
    }
    
    @Override
    public Map<String, Object> uploadLyricsFile(MultipartFile file, Long songId) {
        if (file.isEmpty()) {
            throw BusinessException.of("文件不能为空");
        }
        
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw BusinessException.of("歌曲不存在");
        }
        
        // 确保歌词文件夹存在
        String basePath = storageConfig.getBasePath();
        String lyricsFolderPath = basePath + File.separator + LYRICS_FOLDER;
        File lyricsFolder = new File(lyricsFolderPath);
        if (!lyricsFolder.exists() && !lyricsFolder.mkdirs()) {
            throw BusinessException.of("无法创建歌词文件夹");
        }
        
        // 生成歌词文件路径
        String fileName = file.getOriginalFilename();
        String filePath = lyricsFolderPath + File.separator + fileName;
        File destFile = new File(filePath);
        
        // 保存文件
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw BusinessException.of("文件保存失败: " + e.getMessage());
        }
        
        // 读取歌词内容
        String content;
        try {
            content = Files.readString(destFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw BusinessException.of("读取歌词文件失败: " + e.getMessage());
        }
        
        // 构建相对路径
        String relativeFilePath = LYRICS_FOLDER + File.separator + fileName;
        
        // 查找或创建歌词记录
        Lyrics existing = getOne(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, songId));
        
        if (existing != null) {
            // 更新现有歌词
            existing.setContent(content);
            existing.setFilePath(relativeFilePath);
            existing.setLyricsType(1); // 默认为LRC格式
            updateById(existing);
        } else {
            // 创建新歌词
            Lyrics lyrics = new Lyrics();
            lyrics.setSongId(songId);
            lyrics.setContent(content);
            lyrics.setFilePath(relativeFilePath);
            lyrics.setLyricsType(1); // 默认为LRC格式
            save(lyrics);
            
            // 更新歌曲信息
            song.setHasLyrics(1);
            song.setLyricsId(lyrics.getId());
            songMapper.updateById(song);
        }
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("fileName", fileName);
        result.put("filePath", relativeFilePath);
        
        return result;
    }
    
    @Override
    public void updateLyricsOffset(Long songId, Double lyricsOffset) {
        Lyrics lyrics = getOne(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, songId));
        
        if (lyrics != null) {
            lyrics.setLyricsOffset(lyricsOffset);
            updateById(lyrics);
        }
    }
    
    @Override
    public Map<String, Object> autoSyncLyrics() {
        int successCount = 0;
        int totalCount = 0;
        
        try {
            // 查询未同步歌词的歌曲（无歌词标记或未关联歌词ID）
            List<Song> songsWithoutLyrics = songMapper.selectList(new LambdaQueryWrapper<Song>()
                    .and(wrapper -> wrapper
                            .eq(Song::getHasLyrics, 0)
                            .or()
                            .isNull(Song::getLyricsId)));
            
            totalCount = songsWithoutLyrics.size();
            
            for (Song song : songsWithoutLyrics) {
                try {
                    // 获取歌曲名称和歌手名称
                    String songName = song.getTitle();
                    String artistName = null;
                    
                    if (song.getArtistId() != null) {
                        Artist artist = artistMapper.selectById(song.getArtistId());
                        if (artist != null) {
                            artistName = artist.getName();
                        }
                    }
                    
                    if (StringUtils.hasText(songName) && StringUtils.hasText(artistName)) {
                        // 构建回退API请求URL（主流程优先走备用API）
                        String apiUrl = lyricsConfig.getPath() + "?" + lyricsConfig.getSongName() + "=" + 
                                songName + "&" + 
                                lyricsConfig.getSingerName() + "=" + 
                                artistName;

                        Map<String, Object> matchResult = autoMatchLyricsFromApi(apiUrl, song.getId());
                        if (Boolean.TRUE.equals(matchResult.get("success"))) {
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    // 忽略单首歌曲的错误，继续处理其他歌曲
                    log.warn("同步歌曲ID {} 的歌词失败: {}", song.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("自动同步歌词失败: {}", e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        return result;
    }
    
    @Override
    public Map<String, Object> autoMatchLyricsFromApi(String apiUrl, Long songId) {
        // 优先使用备用API
        Map<String, Object> backupResult = tryBackupLyricsApi(songId);
        Object backupSuccess = backupResult.get("success");
        if (Boolean.TRUE.equals(backupSuccess)) {
            return backupResult;
        }

        log.info("备用API未匹配到歌词，回退到第一个API。songId={}, reason={}", songId, backupResult.get("message"));

        // 备用API失败后，回退到第一个API
        try {
            String lyricsContent = restTemplate.getForObject(apiUrl, String.class);
            log.info("为歌曲ID {} 自动匹配歌词（回退API），API链接: {}", songId, apiUrl);
            lyricsContent = normalizeLrcContent(lyricsContent);
            if (StringUtils.hasText(lyricsContent) && isLrcFormat(lyricsContent)) {
                Lyrics lyrics = saveOrUpdateLyricsBySongId(songId, lyricsContent, "自动匹配", apiUrl);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "歌词匹配成功（回退API）");
                result.put("lyricsId", lyrics.getId());
                return result;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "备用API和回退API都未匹配到歌词");
            return result;
        } catch (Exception e) {
            log.error("回退API匹配歌词失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "备用API和回退API都匹配失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 尝试使用备用歌词API获取歌词
     */
    private Map<String, Object> tryBackupLyricsApi(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "歌曲不存在");
            return result;
        }
        
        String songName = song.getTitle();
        String artistName = null;
        
        if (song.getArtistId() != null) {
            Artist artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                artistName = artist.getName();
            }
        }
        
        if (!StringUtils.hasText(songName) || !StringUtils.hasText(artistName)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "歌曲名称或歌手名称不能为空");
            return result;
        }
        
        try {
            // 第一步：使用关键词搜索歌曲
            String searchUrl = lyrics2Config.getPath() + "?" + lyrics2Config.getSongName() + "=" + 
                    songName + "-" + artistName;
            log.info("备用API搜索URL: {}", searchUrl);
            
            String searchResult = restTemplate.getForObject(searchUrl, String.class);
            log.info("备用API搜索结果: {}", searchResult);
            
            if (!StringUtils.hasText(searchResult)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API搜索失败");
                return result;
            }
            
            // 解析搜索结果，获取第一个匹配的歌曲ID
            Map<String, Object> searchResponse = parseJsonResponse(searchResult);
            if (searchResponse == null || !Integer.valueOf(1).equals(searchResponse.get("code"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API搜索结果解析失败");
                return result;
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) searchResponse.get("data");
            if (data == null || data.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API未找到匹配的歌曲");
                return result;
            }
            
            // 使用第一个匹配结果
            Map<String, Object> firstResult = data.get(0);
            String songIdStr = firstResult.get("id").toString();
            String matchedSongName = firstResult.get("name").toString();
            @SuppressWarnings("unchecked")
            List<String> singers = (List<String>) firstResult.get("singer");
            String matchedArtistName = singers != null && !singers.isEmpty() ? singers.get(0) : "";
            
            log.info("匹配到歌曲: {} - {}", matchedSongName, matchedArtistName);
            
            // 第二步：使用歌曲ID获取歌词
            String lyricsUrl = lyrics2Config.getPath() + "?" + lyrics2Config.getSongName() + "=" + 
                    songName + "-" + artistName + 
                    "&id=" + songIdStr;
            log.info("备用API歌词URL: {}", lyricsUrl);
            
            String lyricsResult = restTemplate.getForObject(lyricsUrl, String.class);
            log.info("备用API歌词结果: {}", lyricsResult);
            
            if (!StringUtils.hasText(lyricsResult)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API获取歌词失败");
                return result;
            }
            
            // 解析歌词结果
            Map<String, Object> lyricsResponse = parseJsonResponse(lyricsResult);
            if (lyricsResponse == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API歌词结果解析失败");
                return result;
            }
            
            // 获取歌词明文
            String lyricsContent = null;
            if (lyricsResponse.containsKey("conteng")) {
                lyricsContent = lyricsResponse.get("conteng").toString();
            } else if (lyricsResponse.containsKey("content")) {
                lyricsContent = lyricsResponse.get("content").toString();
            } else if (lyricsResponse.containsKey("message")) {
                lyricsContent = lyricsResponse.get("message").toString();
            } else if (lyricsResponse.containsKey("data") && lyricsResponse.get("data") instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) lyricsResponse.get("data");
                if (dataMap.containsKey("conteng")) {
                    lyricsContent = dataMap.get("conteng").toString();
                } else if (dataMap.containsKey("content")) {
                    lyricsContent = dataMap.get("content").toString();
                }
            }

            // 将字符串中的"\\n"、"\\r\\n"等转为真实换行，避免校验误判
            lyricsContent = normalizeLrcContent(lyricsContent);

            if (!StringUtils.hasText(lyricsContent)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API未找到歌词内容");
                return result;
            }
            
            // 验证是否为LRC格式
            if (!isLrcFormat(lyricsContent)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "备用API返回的歌词格式不正确");
                return result;
            }
            
            // 覆盖保存歌词记录（按song_id唯一）
            String sourceUrl = searchUrl + ";" + lyricsUrl;
            Lyrics lyrics = saveOrUpdateLyricsBySongId(songId, lyricsContent, "自动匹配(备用)", sourceUrl);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "歌词匹配成功（备用API）");
            result.put("lyricsId", lyrics.getId());
            result.put("matchedSong", matchedSongName + " - " + matchedArtistName);
            return result;
            
        } catch (Exception e) {
            log.error("备用API匹配歌词失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "备用API匹配失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 按song_id覆盖保存歌词（song_id唯一）
     */
    private Lyrics saveOrUpdateLyricsBySongId(Long songId, String lyricsContent, String source, String sourceUrl) {
        Lyrics existingLyrics = getOne(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, songId)
                .last("LIMIT 1"));

        Lyrics lyrics;
        if (existingLyrics != null) {
            lyrics = existingLyrics;
            lyrics.setContent(lyricsContent);
            lyrics.setLyricsType(1);
            lyrics.setSource(source);
            lyrics.setSourceUrl(sourceUrl);
            lyrics.setLyricsOffset(0.0);
            updateById(lyrics);
        } else {
            lyrics = new Lyrics();
            lyrics.setSongId(songId);
            lyrics.setContent(lyricsContent);
            lyrics.setLyricsType(1);
            lyrics.setSource(source);
            lyrics.setSourceUrl(sourceUrl);
            lyrics.setLyricsOffset(0.0);
            save(lyrics);
        }

        Song song = songMapper.selectById(songId);
        if (song != null) {
            song.setHasLyrics(1);
            song.setLyricsId(lyrics.getId());
            songMapper.updateById(song);
        }

        return lyrics;
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 将转义换行符标准化为真实换行符，并统一换行格式
     */
    private String normalizeLrcContent(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }

        String normalized = content
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        return normalized.trim();
    }

    /**
     * 检查是否为LRC格式歌词
     */
    private boolean isLrcFormat(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }

        String normalized = normalizeLrcContent(content);
        String[] lines = normalized.split("\n");
        int lrcLineCount = 0;
        int metadataTagCount = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher matcher = LRC_PATTERN.matcher(line);
            if (matcher.find()) {
                lrcLineCount++;
                continue;
            }

            Matcher matcherWithoutText = LRC_PATTERN_WITHOUT_TEXT.matcher(line);
            if (matcherWithoutText.find()) {
                lrcLineCount++;
                continue;
            }

            if (line.matches("^\\[(ti|ar|al|by|offset):.*]$")) {
                metadataTagCount++;
            }
        }

        // 规则：至少3行时间轴歌词，或存在较完整LRC头信息且至少1行时间轴
        return lrcLineCount >= 3 || (metadataTagCount >= 2 && lrcLineCount >= 1);
    }
    
    @Override
    public boolean hasLyrics(Long songId) {
        return count(new LambdaQueryWrapper<Lyrics>()
                .eq(Lyrics::getSongId, songId)) > 0;
    }
}
