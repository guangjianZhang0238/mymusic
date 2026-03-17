package com.music.api.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作通用返回结构
 */
@Data
public class BatchOperationResultVO {
    /**
     * 成功的对象ID列表
     */
    private List<Long> successList = new ArrayList<>();

    /**
     * 跳过/失败的对象列表
     */
    private List<SkipItem> skipList = new ArrayList<>();

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

