package com.music.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lyrics")
public class LyricsConfig {
    
    private String path;
    private String songName;
    private String singerName;
}
