package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.LyricsDTO;
import com.music.api.vo.LyricsVO;
import com.music.content.entity.Lyrics;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface LyricsService extends IService<Lyrics> {
    
    Page<LyricsVO> pageList(String keyword, Long songId, int current, int size);
    
    LyricsVO getBySongId(Long songId);
    
    Long create(LyricsDTO dto);
    
    void update(LyricsDTO dto);
    
    void delete(Long id);
    
    List<LyricsVO.LyricsLine> parseLrc(String content);
    
    Map<String, Object> uploadLyricsFile(MultipartFile file, Long songId);
    
    void updateLyricsOffset(Long songId, Double lyricsOffset);
    
    Map<String, Object> autoSyncLyrics();
    
    Map<String, Object> autoMatchLyricsFromApi(String apiUrl, Long songId);
    
    boolean hasLyrics(Long songId);
}
