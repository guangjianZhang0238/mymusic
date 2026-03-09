package com.music.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lyrics2")
public class Lyrics2Config {
    
    private String path;
    private String songName;
}
