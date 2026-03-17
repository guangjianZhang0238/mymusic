package com.music.api.vo;

import lombok.Data;

/**
 * 切换专辑歌手返回
 */
@Data
public class SwitchAlbumArtistResultVO {
    private boolean success;
    private Long albumId;
    private String oldFolderPath;
    private String newFolderPath;
    private String reason;

    public static SwitchAlbumArtistResultVO ok(Long albumId, String oldFolderPath, String newFolderPath) {
        SwitchAlbumArtistResultVO vo = new SwitchAlbumArtistResultVO();
        vo.setSuccess(true);
        vo.setAlbumId(albumId);
        vo.setOldFolderPath(oldFolderPath);
        vo.setNewFolderPath(newFolderPath);
        return vo;
    }

    public static SwitchAlbumArtistResultVO fail(Long albumId, String reason) {
        SwitchAlbumArtistResultVO vo = new SwitchAlbumArtistResultVO();
        vo.setSuccess(false);
        vo.setAlbumId(albumId);
        vo.setReason(reason);
        return vo;
    }
}

