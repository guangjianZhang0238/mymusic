package com.music.player.dto;

import lombok.Data;
import java.util.List;

@Data
public class PlaybackPlaylistDTO {
    private List<Long> songIds;
    private Integer currentIndex;
}
