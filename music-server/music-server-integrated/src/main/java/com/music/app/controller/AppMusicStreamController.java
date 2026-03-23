package com.music.app.controller;

import com.music.content.entity.Song;
import com.music.content.service.SongService;
import com.music.file.config.StorageConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "App端音频流接口")
@RestController
@RequestMapping("/api/app/music")
@RequiredArgsConstructor
public class AppMusicStreamController {

    private static final int CACHE_SECONDS = 120;

    private final SongService songService;
    private final StorageConfig storageConfig;

    @Operation(summary = "App端按歌曲ID流式播放（支持Range分片）")
    @GetMapping("/song/{songId}/stream")
    public ResponseEntity<Resource> streamSong(@PathVariable Long songId, HttpServletRequest request) {
        Song song = songService.getById(songId);
        if (song == null || !StringUtils.hasText(song.getFilePath())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        File file = resolveSongFile(song.getFilePath());
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        long fileLength = file.length();
        String range = request.getHeader(HttpHeaders.RANGE);
        String contentType = detectContentType(file);
        String etag = buildEtag(file);
        long lastModified = file.lastModified();

        HttpHeaders baseHeaders = buildBaseHeaders(contentType, etag, lastModified);

        if (!StringUtils.hasText(range) && isNotModified(request, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .headers(baseHeaders)
                    .build();
        }

        if (!StringUtils.hasText(range)) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(baseHeaders);
            headers.setContentLength(fileLength);
            return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
        }

        long[] parsed;
        try {
            parsed = parseRange(range, fileLength);
        } catch (IllegalArgumentException ex) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(baseHeaders);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength);
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        long start = parsed[0];
        long end = parsed[1];
        long contentLength = end - start + 1;

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(baseHeaders);
        headers.set(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileLength));
        headers.setContentLength(contentLength);

        Resource region = new FileRegionResource(file, start, contentLength);
        return new ResponseEntity<>(region, headers, HttpStatus.PARTIAL_CONTENT);
    }

    private File resolveSongFile(String rawPath) {
        String normalizedPath = rawPath.replace("\\", "/");
        Path path = Paths.get(normalizedPath);

        if (!path.isAbsolute()) {
            path = Paths.get(storageConfig.getBasePath()).resolve(path).normalize();
        }

        return path.toFile();
    }

    private @NonNull String detectContentType(File file) {
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    private HttpHeaders buildBaseHeaders(@NonNull String contentType, @NonNull String etag, long lastModified) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_SECONDS);
        headers.setETag(etag);
        headers.setLastModified(lastModified);
        headers.setContentType(MediaType.parseMediaType(contentType));
        return headers;
    }

    private @NonNull String buildEtag(File file) {
        // 弱 ETag：文件长度 + 最后修改时间
        return "W/\"" + file.length() + "-" + file.lastModified() + "\"";
    }

    private boolean isNotModified(HttpServletRequest request, @NonNull String etag) {
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (!StringUtils.hasText(ifNoneMatch)) {
            return false;
        }

        String[] tags = ifNoneMatch.split(",");
        for (String tag : tags) {
            if (tag == null) continue;
            String trimmed = tag.trim();
            if ("*".equals(trimmed) || etag.equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private long[] parseRange(String rangeHeader, long fileLength) {
        // 仅处理单个 bytes 范围，格式：bytes=start-end
        if (!StringUtils.hasText(rangeHeader)) {
            throw new IllegalArgumentException("Range unit not supported");
        }

        String rangeValue = rangeHeader.trim().toLowerCase();
        if (!rangeValue.startsWith("bytes=")) {
            throw new IllegalArgumentException("Range unit not supported");
        }

        String[] ranges = rangeValue.substring("bytes=".length()).split(",");
        if (ranges.length != 1) {
            throw new IllegalArgumentException("Multiple ranges are not supported");
        }

        int dashIndex = ranges[0].indexOf('-');
        if (dashIndex < 0) {
            throw new IllegalArgumentException("Invalid range format");
        }

        String startPart = ranges[0].substring(0, dashIndex).trim();
        String endPart = ranges[0].substring(dashIndex + 1).trim();

        long start;
        long end;

        try {
            if (startPart.isEmpty()) {
                // bytes=-500，取最后500字节
                if (endPart.isEmpty()) {
                    throw new IllegalArgumentException("Invalid suffix range");
                }
                long suffixLength = Long.parseLong(endPart);
                if (suffixLength <= 0) {
                    throw new IllegalArgumentException("Invalid suffix range length");
                }
                start = Math.max(fileLength - suffixLength, 0);
                end = fileLength - 1;
            } else {
                start = Long.parseLong(startPart);
                if (start < 0 || start >= fileLength) {
                    throw new IllegalArgumentException("Range start out of bounds");
                }

                if (endPart.isEmpty()) {
                    // bytes=500-：按标准返回到文件末尾
                    end = fileLength - 1;
                } else {
                    end = Long.parseLong(endPart);
                }
            }

            if (end < start) {
                throw new IllegalArgumentException("Range end before start");
            }

            end = Math.min(end, fileLength - 1);
            return new long[]{start, end};
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid range number", ex);
        }
    }
}
