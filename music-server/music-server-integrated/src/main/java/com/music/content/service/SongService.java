package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.BatchSongIdsDTO;
import com.music.api.dto.BatchSwitchArtistDTO;
import com.music.api.dto.SongDTO;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.BatchOperationResultVO;
import com.music.api.vo.BatchSwitchArtistResultVO;
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

    /**
     * 批量删除歌曲（含清理关联关系）
     */
    BatchOperationResultVO batchDelete(BatchSongIdsDTO dto);

    /**
     * 批量切换歌曲歌手（可选专辑，空则默认专辑）
     */
    BatchSwitchArtistResultVO batchSwitchArtist(BatchSwitchArtistDTO dto);
}
