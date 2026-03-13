package com.music.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.ArtistDTO;
import com.music.api.vo.ArtistVO;
import com.music.api.vo.MatchAvatarResultVO;
import com.alibaba.fastjson2.JSONObject;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements ArtistService {
    
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;
    private final SongArtistMapper songArtistMapper;
    private final StorageConfig storageConfig;
    private final RestTemplate restTemplate;
    
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

    @Override
    public MatchAvatarResultVO matchAvatar(Long id) {
        if (id == null) {
            throw BusinessException.of("歌手ID不能为空");
        }
        Artist artist = getById(id);
        if (artist == null) {
            throw BusinessException.of("歌手不存在");
        }
        if (StringUtils.hasText(artist.getAvatar())) {
            throw BusinessException.of("歌手头像已存在，无需匹配");
        }

        String artworkUrl = findItunesArtworkUrl(artist.getName());
        if (!StringUtils.hasText(artworkUrl) && StringUtils.hasText(artist.getNameEn())) {
            artworkUrl = findItunesArtworkUrl(artist.getNameEn());
        }
        if (!StringUtils.hasText(artworkUrl)) {
            throw BusinessException.of("未匹配到歌手头像");
        }

        String largeArtworkUrl = upscaleItunesArtworkUrl(artworkUrl, 600);
        byte[] imageBytes = downloadImageBytesWithLimit(largeArtworkUrl, 5 * 1024 * 1024);

        String avatarPath = saveArtistAvatarBytes(imageBytes, artist.getName());

        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        // ArtistDTO.name is @NotBlank, must set it even if only updating avatar
        dto.setName(artist.getName());
        dto.setAvatar(avatarPath);
        update(dto);

        MatchAvatarResultVO vo = new MatchAvatarResultVO();
        vo.setAvatar(avatarPath);
        return vo;
    }

    private String saveArtistAvatarBytes(byte[] bytes, String artistName) {
        if (bytes == null || bytes.length == 0) {
            throw BusinessException.of("头像文件不能为空");
        }
        if (bytes.length > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        if (!StringUtils.hasText(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        String cleanedArtistName = artistName.replaceAll("[\\\\/:*?\"<>|]", "");
        if (!StringUtils.hasText(cleanedArtistName)) {
            throw BusinessException.of("歌手名称包含非法字符");
        }

        String relativePath = cleanedArtistName + "/cover.jpg";
        String normalizedRelativePath = relativePath.replace('/', File.separatorChar);
        Path destPath = Paths.get(storageConfig.getBasePath(), normalizedRelativePath);

        try {
            Path parent = destPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(destPath, bytes);
        } catch (Exception e) {
            throw BusinessException.of("文件保存失败: " + e.getMessage());
        }
        return relativePath;
    }

    private String findItunesArtworkUrl(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        // iTunes 对中文歌手在 musicArtist 维度经常返回空，这里做多轮兜底：
        // 1) 先搜 musicArtist
        // 2) 再搜 musicTrack（用歌曲/专辑封面作为头像），命中率更高
        // 3) 多地区 country 依次尝试
        List<String> countries = List.of("CN", "HK", "TW", "US");
        for (String country : countries) {
            String artistArtwork = queryItunesArtwork(keyword, "musicArtist", "artistTerm", country);
            if (StringUtils.hasText(artistArtwork)) return artistArtwork;
        }
        for (String country : countries) {
            String trackArtwork = queryItunesArtwork(keyword, "musicTrack", "artistTerm", country);
            if (StringUtils.hasText(trackArtwork)) return trackArtwork;
        }
        return null;
    }

    private String queryItunesArtwork(String term, String entity, String attribute, String country) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://itunes.apple.com/search")
                .queryParam("term", term)
                .queryParam("media", "music")
                .queryParam("entity", entity)
                .queryParam("attribute", attribute)
                .queryParam("limit", 10)
                .queryParam("country", country)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        // iTunes 可能返回 Content-Type: text/javascript;charset=utf-8
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, new MediaType("text", "javascript")));
        ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String body = resp.getBody();
        if (!StringUtils.hasText(body)) return null;

        JSONObject json = JSONObject.parseObject(body);
        if (json == null) return null;

        var results = json.getJSONArray("results");
        if (results == null || results.isEmpty()) return null;

        for (int i = 0; i < results.size(); i++) {
            JSONObject item = results.getJSONObject(i);
            if (item == null) continue;
            String artwork = item.getString("artworkUrl100");
            if (!StringUtils.hasText(artwork)) {
                artwork = item.getString("artworkUrl60");
            }
            if (StringUtils.hasText(artwork)) {
                return artwork;
            }
        }
        return null;
    }

    private String upscaleItunesArtworkUrl(String artworkUrl, int size) {
        if (!StringUtils.hasText(artworkUrl)) return artworkUrl;
        // typical: .../100x100bb.jpg -> .../600x600bb.jpg
        return artworkUrl.replaceAll("/\\d+x\\d+bb\\.(jpg|jpeg|png)$", "/" + size + "x" + size + "bb.$1");
    }

    private byte[] downloadImageBytesWithLimit(String url, int maxBytes) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw BusinessException.of("头像地址非法");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw BusinessException.of("头像地址不安全（仅允许 https）");
        }
        String host = uri.getHost();
        if (!isAllowedImageHost(host)) {
            throw BusinessException.of("头像地址不受信任");
        }

        ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, null, byte[].class);
        long contentLength = resp.getHeaders().getContentLength();
        if (contentLength > maxBytes) {
            throw BusinessException.of("头像文件过大");
        }
        byte[] bytes = resp.getBody();
        if (bytes == null || bytes.length == 0) {
            throw BusinessException.of("头像下载失败");
        }
        if (bytes.length > maxBytes) {
            throw BusinessException.of("头像文件过大");
        }
        return bytes;
    }

    private boolean isAllowedImageHost(String host) {
        if (!StringUtils.hasText(host)) return false;
        String h = host.toLowerCase();
        return h.endsWith(".mzstatic.com") || h.equals("mzstatic.com") ||
               h.endsWith(".apple.com") || h.equals("apple.com");
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
