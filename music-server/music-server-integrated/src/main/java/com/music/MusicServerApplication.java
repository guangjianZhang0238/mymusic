package com.music;

import com.music.file.config.StorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@ComponentScan(basePackages = "com.music")
@EnableConfigurationProperties({StorageConfig.class})
@EnableAsync
@EnableScheduling
@org.springframework.cache.annotation.EnableCaching
public class MusicServerApplication {

    public static void main(String[] args) {
        // 设置系统属性，确保使用 UTF-8 编码
        System.setProperty("file.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("sun.jnu.encoding", StandardCharsets.UTF_8.name());
        System.setProperty("java.awt.headless", "true");

        // 设置控制台编码
        System.setProperty("console.encoding", StandardCharsets.UTF_8.name());

        // 强制设置标准输入输出的编码
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SpringApplication.run(MusicServerApplication.class, args);
    }
}
