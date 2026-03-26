package com.music.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.app.service.AppMusicService;
import com.music.app.vo.AppAlbumVO;
import com.music.app.vo.AppArtistVO;
import com.music.app.vo.AppSongVO;
import com.music.app.vo.SearchSuggestionVO;
import com.music.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "App端音乐接口")

@Tag(name = "App端音乐接口")
@RestController
@RequestMapping("/api/app/music")
@RequiredArgsConstructor
public class AppMusicController {

    private static final Logger log = LoggerFactory.getLogger(AppMusicController.class);
    
    private final AppMusicService appMusicService;

    @Operation(summary = "App端歌曲分页")
    @GetMapping("/song/page")
    public Result<Page<AppSongVO>> songPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Long artistId
    ) {
        log.info("访问接口：开始查询歌曲分页，关键词: {}, 专辑ID: {}, 歌手ID: {}", keyword, albumId, artistId);
        return Result.success(appMusicService.pageSongs(current, size, keyword, albumId, artistId));
    }

    @Operation(summary = "App端热门歌曲")
    @GetMapping("/song/hot")
    public Result<List<AppSongVO>> hotSongs() {
        log.info("访问接口：开始查询热门歌曲");
        return Result.success(appMusicService.hotSongs());
    }

    @Operation(summary = "App端热门歌手")
    @GetMapping("/artist/hot")
    public Result<List<AppArtistVO>> hotArtists(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("访问接口：查询热门歌手，数量: {}", limit);
        return Result.success(appMusicService.hotArtists(limit));
    }

    @Operation(summary = "App端专辑分页")
    @GetMapping("/album/page")
    public Result<Page<AppAlbumVO>> albumPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long artistId
    ) {
        log.info("访问接口：开始查询专辑分页，关键词: {}, 歌手ID: {}", keyword, artistId);
        return Result.success(appMusicService.pageAlbums(current, size, keyword, artistId));
    }

    @Operation(summary = "App端歌手分页")
    @GetMapping("/artist/page")
    public Result<Page<AppArtistVO>> artistPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword
    ) {
        log.info("访问接口：开始查询歌手分页，关键词: {}", keyword);
        return Result.success(appMusicService.pageArtists(current, size, keyword));
    }

    @Operation(summary = "App端根据ID列表获取歌曲")
    @GetMapping("/song/by-ids")
    public Result<List<AppSongVO>> songsByIds(
            @RequestParam(value = "ids", required = false) String idsRaw,
            HttpServletRequest request) {
        // 避免 Spring 直接把 ids 转成 List<Long> 时触发 MethodArgumentTypeMismatchException
        // 例如 ids=hot / ids=[hot] 时应当返回空而不是 500。
        List<Long> finalIds = idsRaw != null ? parseJsonLikeIds(idsRaw) : parseJsonLikeIds(request.getParameter("ids"));
        log.info("访问接口：开始根据ID列表获取歌曲，ID数量: {}", finalIds != null ? finalIds.size() : 0);
        return Result.success(appMusicService.getSongsByIds(finalIds));
    }

    /**
     * 兼容小程序把 ids 作为 "[1,2,3]" 传入 query string 的场景。
     */
    private List<Long> parseJsonLikeIds(String rawIds) {
        if (rawIds == null || rawIds.isBlank()) {
            return List.of();
        }
        String normalized = rawIds.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] parts = normalized.split(",");
        List<Long> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String token = part == null ? "" : part.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.parseLong(token));
            } catch (NumberFormatException ex) {
                log.warn("忽略非法歌曲ID参数: {}", token);
            }
        }
        return result;
    }

    @Operation(summary = "App端专辑详情")
    @GetMapping("/album/{albumId}")
    public Result<AppAlbumVO> albumDetail(@PathVariable Long albumId) {
        log.info("访问接口：开始查询专辑详情，专辑ID: {}", albumId);
        return Result.success(appMusicService.getAlbumDetail(albumId));
    }

    @Operation(summary = "App端歌手详情")
    @GetMapping("/artist/{artistId:\\d+}")
    public Result<AppArtistVO> artistDetail(@PathVariable Long artistId) {
        log.info("访问接口：开始查询歌手详情，歌手ID: {}", artistId);
        return Result.success(appMusicService.getArtistDetail(artistId));
    }
    
    @Operation(summary = "App端歌手热门歌曲")
    @GetMapping("/artist/{artistId:\\d+}/top-songs")
    public Result<List<AppSongVO>> artistTopSongs(
            @PathVariable Long artistId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("访问接口：查询歌手热门歌曲，歌手 ID: {}, 数量: {}", artistId, limit);
        return Result.success(appMusicService.getArtistTopSongs(artistId, limit));
    }

    @Operation(summary = "App端搜索联想")
    @GetMapping("/search/suggestions")
    public Result<List<SearchSuggestionVO>> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("访问接口：开始获取搜索联想，关键字: {}, 限制数量: {}", keyword, limit);
        return Result.success(appMusicService.getSuggestions(keyword, limit));
    }
}
