package com.music.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量切换歌手返回结构（包含路径变更明细）
 */
@Data
public class BatchSwitchArtistResultVO {
    private List<SuccessItem> successList = new ArrayList<>();
    private List<SkipItem> skipList = new ArrayList<>();

    @Data
    public static class SuccessItem {
        private Long id;
        private String oldPath;
        private String newPath;

        public static SuccessItem of(Long id, String oldPath, String newPath) {
            SuccessItem item = new SuccessItem();
            item.setId(id);
            item.setOldPath(oldPath);
            item.setNewPath(newPath);
            return item;
        }
    }

    @Data
    public static class SkipItem {
        private Long id;
        private String reason;

        public static SkipItem of(Long id, String reason) {
            SkipItem item = new SkipItem();
            item.setId(id);
            item.setReason(reason);
            return item;
        }
    }
}

