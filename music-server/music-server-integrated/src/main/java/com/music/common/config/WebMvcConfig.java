package com.music.common.config;

import com.music.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final StorageConfig storageConfig;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径，将base-path目录映射到/static/**路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + storageConfig.getBasePath() + "/")
                .setCachePeriod(3600);
    }
}
