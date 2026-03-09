package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.SongDTO;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.SongVO;
import com.music.content.entity.Song;
import java.util.List;
import java.util.Map;

public interface SongService extends IService<Song> {
    
    Page<SongVO> pageList(SongQueryDTO query);
    
    SongVO getDetail(Long id);
    
    Long create(SongDTO dto);
    
    void update(SongDTO dto);
    
    void delete(Long id);
    
    void incrementPlayCount(Long id);
    
    List<SongVO> getHotSongs();
    
    Map<String, Object> autoMatchLyrics(Long songId);
}
