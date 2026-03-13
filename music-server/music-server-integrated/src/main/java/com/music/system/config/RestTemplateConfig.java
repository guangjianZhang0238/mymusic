package com.music.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * RestTemplate配置类
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(30000);
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(30000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // 设置StringHttpMessageConverter使用UTF-8编码
        restTemplate.getMessageConverters().stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> {
                    StringHttpMessageConverter stringConverter = (StringHttpMessageConverter) converter;
                    stringConverter.setSupportedMediaTypes(Arrays.asList(
                            new org.springframework.http.MediaType("text", "plain", StandardCharsets.UTF_8),
                            new org.springframework.http.MediaType("text", "html", StandardCharsets.UTF_8),
                            // iTunes Search API may respond with Content-Type: text/javascript;charset=utf-8
                            new org.springframework.http.MediaType("text", "javascript", StandardCharsets.UTF_8),
                            new org.springframework.http.MediaType("application", "json", StandardCharsets.UTF_8),
                            org.springframework.http.MediaType.TEXT_PLAIN,
                            org.springframework.http.MediaType.TEXT_HTML,
                            org.springframework.http.MediaType.APPLICATION_JSON
                    ));
                });
        
        return restTemplate;
    }
}
