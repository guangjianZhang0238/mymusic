"# mymusic" 

## 后端存储路径配置（Windows / Linux 通用）

- **根配置文件**：`music-server/music-server-integrated/src/main/resources/application.yml`
- **核心配置项**（所有文件读写都基于这些路径，已支持 Windows 与 Linux）：

```yaml
music:
  storage:
    # 数据根目录，可以是绝对路径或相对路径（相对于应用启动目录）
    # Windows 示例：D:/mymusic-data
    # Linux  示例：/data/mymusic
    # 相对路径示例（推荐开发环境使用）：data
    data-root-url: D:/mymusic-data
    # 在数据根目录下的业务存储根目录和临时目录（相对路径）
    base-path: music_source
    temp-path: music_temp
    # 单个文件最大上传大小（字节），默认 2GB
    max-file-size: 2147483648
```

- **说明**：
  - 无论是 Windows 还是 Linux，只需要根据实际环境修改 `data-root-url`，其余代码无需改动即可正常运行。
  - `base-path` 和 `temp-path` 始终被当作 **相对路径** 处理，真实磁盘路径会在运行时通过 Java 的 `Path/Paths` 组合，自动适配 `/` 或 `\`。
  - 所有业务 Service（上传、扫描、歌词文件、数据清理等）已经统一使用该配置，不再依赖硬编码盘符。
