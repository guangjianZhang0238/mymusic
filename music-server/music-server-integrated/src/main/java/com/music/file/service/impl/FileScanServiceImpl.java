package com.music.file.service.impl;

import com.music.api.dto.SongDTO;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.SongService;
import com.music.file.config.StorageConfig;
import com.music.file.entity.TranscodingResult;
import com.music.file.service.AudioTranscodingService;
import com.music.file.service.FileScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文件扫描服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileScanServiceImpl implements FileScanService {

    private final StorageConfig storageConfig;
    private final SongService songService;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;
    private final AudioTranscodingService transcodingService;

    @Override
    public ScanResult scanMusicLibrary() {
        log.info("开始扫描音乐库...");
        
        // 创建扫描结果对象
        final ScanResultImpl result = new ScanResultImpl();
        
        // 获取base-path路径
        String basePath = storageConfig.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            result.addError("base-path未配置");
            log.error("base-path未配置");
            return result;
        }
        
        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            result.addError("base-path目录不存在或不是目录: " + basePath);
            log.error("base-path目录不存在或不是目录: {}", basePath);
            return result;
        }
        
        // 预加载所有歌手、专辑和歌曲数据，减少数据库查询次数
        List<Artist> allArtists = artistMapper.selectList(null);
        List<Album> allAlbums = albumMapper.selectList(null);
        List<Song> allSongs = songMapper.selectList(null);
        
        // 扫描歌手文件夹
        File[] artistDirs = baseDir.listFiles(File::isDirectory);
        if (artistDirs == null || artistDirs.length == 0) {
            result.addError("base-path目录下没有歌手文件夹");
            log.error("base-path目录下没有歌手文件夹");
            return result;
        }
        
        for (File artistDir : artistDirs) {
            // 忽略名为"upload"和".accelerate"的文件夹
            String artistName = artistDir.getName();
            if ("upload".equals(artistName) || ".accelerate".equals(artistName)) {
                log.info("忽略文件夹: {}", artistName);
                continue;
            }
            
            try {
                // 扫描歌手文件夹
                scanArtistDirectory(artistDir, allArtists, allAlbums, allSongs, result);
            } catch (Exception e) {
                String errorMsg = "扫描歌手文件夹失败: " + artistDir.getName() + ", 错误: " + e.getMessage();
                result.addError(errorMsg);
                log.error(errorMsg, e);
            }
        }
        
        log.info("音乐库扫描完成，新增歌曲: {}, 更新歌曲: {}, 跳过歌曲: {}, 错误数: {}", 
                result.getAddedSongs(), result.getUpdatedSongs(), result.getSkippedSongs(), result.getErrors().size());
        
        return result;
    }
    
    /**
     * 扫描歌手文件夹
     * @param artistDir 歌手文件夹
     * @param allArtists 所有歌手列表
     * @param allAlbums 所有专辑列表
     * @param allSongs 所有歌曲列表
     * @param result 扫描结果
     */
    private void scanArtistDirectory(File artistDir, List<Artist> allArtists, List<Album> allAlbums, List<Song> allSongs, ScanResultImpl result) {
        // 歌手名称
        String artistName = artistDir.getName();
        log.info("扫描歌手文件夹: {}", artistName);
        
        // 查找或创建歌手
        Artist artist = findOrCreateArtist(artistName, allArtists);
        if (artist == null) {
            result.addError("创建歌手失败: " + artistName);
            return;
        }
        
        // 扫描专辑文件夹
        File[] albumDirs = artistDir.listFiles(File::isDirectory);
        if (albumDirs == null || albumDirs.length == 0) {
            log.warn("歌手文件夹下没有专辑文件夹: {}", artistName);
            return;
        }
        
        for (File albumDir : albumDirs) {
            try {
                // 扫描专辑文件夹
                scanAlbumDirectory(albumDir, artist, allAlbums, allSongs, result);
            } catch (Exception e) {
                String errorMsg = "扫描专辑文件夹失败: " + albumDir.getName() + ", 错误: " + e.getMessage();
                result.addError(errorMsg);
                log.error(errorMsg, e);
            }
        }
    }
    
    /**
     * 扫描专辑文件夹
     * @param albumDir 专辑文件夹
     * @param artist 歌手
     * @param allAlbums 所有专辑列表
     * @param allSongs 所有歌曲列表
     * @param result 扫描结果
     */
    private void scanAlbumDirectory(File albumDir, Artist artist, List<Album> allAlbums, List<Song> allSongs, ScanResultImpl result) {
        // 专辑名称
        String albumName = albumDir.getName();
        log.info("扫描专辑文件夹: {}/{}", artist.getName(), albumName);
        
        // 查找或创建专辑
        Album album = findOrCreateAlbum(albumDir, artist.getId(), allAlbums);
        if (album == null) {
            result.addError("创建专辑失败: " + albumName);
            return;
        }
        
        // 扫描歌曲文件
        List<File> songFiles = findSongFiles(albumDir);
        if (songFiles.isEmpty()) {
            log.warn("专辑文件夹下没有歌曲文件: {}/{}", artist.getName(), albumName);
            return;
        }
        
        for (File songFile : songFiles) {
            try {
                // 处理歌曲文件
                processSongFile(songFile, artist, album, allSongs, result);
            } catch (Exception e) {
                String errorMsg = "处理歌曲文件失败: " + songFile.getName() + ", 错误: " + e.getMessage();
                result.addError(errorMsg);
                log.error(errorMsg, e);
            }
        }
    }
    
    /**
     * 处理歌曲文件
     * @param songFile 歌曲文件
     * @param artist 歌手
     * @param album 专辑
     * @param allSongs 所有歌曲列表
     * @param result 扫描结果
     */
    private void processSongFile(File songFile, Artist artist, Album album, List<Song> allSongs, ScanResultImpl result) {
        // 获取歌曲文件名（不含扩展名）
        String fileName = songFile.getName();
        String songTitle = StringUtils.stripFilenameExtension(fileName);
        
        // 构建歌曲文件路径
        String filePath = songFile.getAbsolutePath().replace(storageConfig.getBasePath(), "");
        if (filePath.startsWith(File.separator)) {
            filePath = filePath.substring(1);
        }
        
        // 检查歌曲是否已存在
        if (songExists(filePath, allSongs)) {
            log.info("歌曲已存在: {}", filePath);
            result.incrementSkippedSongs();
            return;
        }
        
        // 提取歌曲信息
        try {
            // 检查是否需要转码（DSF和WAV文件）
            File processedFile = songFile;
            String originalFormat = StringUtils.getFilenameExtension(fileName);
            boolean needsTranscoding = transcodingService.needsTranscoding(songFile);
            File sourceFileToDelete = null; // 转码成功后需要删除的原始文件
            
            if (needsTranscoding) {
                String fileType = StringUtils.getFilenameExtension(fileName);
                log.info("检测到需要转码的文件（{}），开始自动转码: {}", fileType, filePath);
                
                // 执行自动转码（优先FLAC，失败则AAC）
                TranscodingResult transcodingResult = transcodingService.autoTranscode(
                    songFile, songFile.getParentFile());
                
                if (transcodingResult.isSuccess()) {
                    log.info("转码成功: {}", transcodingResult.formatInfo());
                    
                    // 更新文件引用为转码后的文件
                    processedFile = new File(transcodingResult.getTargetPath());
                    fileName = processedFile.getName();
                    filePath = processedFile.getAbsolutePath().replace(storageConfig.getBasePath(), "");
                    if (filePath.startsWith(File.separator)) {
                        filePath = filePath.substring(1);
                    }
                    
                    // 更新格式信息
                    originalFormat = transcodingResult.getTargetFormat();
                    
                    // 如果转码服务未自动删除原文件，则在入库成功后删除
                    if (!transcodingResult.isSourceDeleted()) {
                        sourceFileToDelete = songFile;
                    }
                } else {
                    log.warn("转码失败: {}，将继续使用原始文件", transcodingResult.getErrorMessage());
                }
            }
            
            // 创建歌曲DTO
            SongDTO songDTO = new SongDTO();
            songDTO.setArtistId(artist.getId());
            songDTO.setAlbumId(album.getId());
            songDTO.setTitle(songTitle);
            songDTO.setTitleEn("");
            songDTO.setFilePath(filePath);
            songDTO.setFileName(fileName);
            songDTO.setFileSize(processedFile.length());
            songDTO.setFormat(originalFormat);
            
            // 提取歌曲时长
            int duration = 0;
            try {
                AudioFile audioFile = AudioFileIO.read(processedFile);
                if (audioFile != null && audioFile.getAudioHeader() != null) {
                    duration = audioFile.getAudioHeader().getTrackLength();
                    log.info("提取歌曲时长: {}, 时长: {}秒, 格式: {}, 采样率: {}, 声道数: {}", 
                            filePath, 
                            duration,
                            audioFile.getAudioHeader().getFormat(),
                            audioFile.getAudioHeader().getSampleRateAsNumber(),
                            audioFile.getAudioHeader().getChannels());
                } else {
                    log.warn("无法读取音频文件: {}", filePath);
                }
            } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
                log.error("提取歌曲时长失败: {}, 错误: {}", filePath, e.getMessage(), e);
                // 对于WAV格式，尝试使用不同的方法提取时长
                if ("WAV".equalsIgnoreCase(StringUtils.getFilenameExtension(fileName))) {
                    log.info("尝试使用备用方法提取WAV格式时长: {}", filePath);
                    duration = extractWavDuration(processedFile);
                    if (duration <= 0) {
                        log.info("尝试使用Java AudioSystem提取WAV格式时长: {}", filePath);
                        duration = extractWavDurationUsingAudioSystem(processedFile);
                    }
                }
                // 对于FLAC格式，使用专门的方法提取时长
                else if ("FLAC".equalsIgnoreCase(StringUtils.getFilenameExtension(fileName))) {
                    log.info("尝试使用备用方法提取FLAC格式时长: {}", filePath);
                    duration = extractFlacDuration(processedFile);
                }
                // 对于DSF格式，使用专门的方法提取采样率和时长
                else if ("DSF".equalsIgnoreCase(StringUtils.getFilenameExtension(fileName))) {
                    log.info("尝试提取DSF格式信息: {}", filePath);
                    duration = extractDsfDuration(processedFile);
                }
            }
            songDTO.setDuration(duration);
            
            // 保存歌曲
            Long songId = songService.create(songDTO);
            if (songId != null) {
                log.info("新增歌曲成功: {}/{}/{}", artist.getName(), album.getName(), songTitle);
                result.incrementAddedSongs();
                
                // 入库成功后，如果有需要删除的原始文件（WAV/DSF转码后），则删除之
                if (sourceFileToDelete != null && sourceFileToDelete.exists()) {
                    if (sourceFileToDelete.delete()) {
                        log.info("入库核对无误，已删除原始文件: {}", sourceFileToDelete.getAbsolutePath());
                    } else {
                        log.warn("入库成功但删除原始文件失败: {}", sourceFileToDelete.getAbsolutePath());
                    }
                }
                
                // 将新歌曲添加到allSongs列表中，避免重复处理
                Song newSong = new Song();
                newSong.setId(songId);
                newSong.setArtistId(artist.getId());
                newSong.setAlbumId(album.getId());
                newSong.setTitle(songTitle);
                newSong.setTitleEn("");
                newSong.setFilePath(filePath);
                newSong.setFileName(fileName);
                newSong.setFileSize(processedFile.length());
                newSong.setFormat(originalFormat);
                newSong.setDuration(songDTO.getDuration());
                newSong.setStatus(1);
                allSongs.add(newSong);
            } else {
                result.addError("保存歌曲失败: " + filePath);
                log.error("保存歌曲失败: {}", filePath);
            }
        } catch (Exception e) {
            String errorMsg = "提取歌曲信息失败: " + filePath + ", 错误: " + e.getMessage();
            result.addError(errorMsg);
            log.error(errorMsg, e);
        }
    }
    
    /**
     * 提取FLAC格式歌曲时长
     * @param songFile 歌曲文件
     * @return 时长（秒）
     */
    private int extractFlacDuration(File songFile) {
        try {
            // 使用Java AudioSystem作为备用方法
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(songFile)) {
                AudioFormat format = audioStream.getFormat();
                long frames = audioStream.getFrameLength();
                double durationSeconds = (double) frames / format.getFrameRate();
                
                log.info("使用AudioSystem提取FLAC时长: {}, 时长: {}秒, 帧数: {}, 帧率: {}, 格式: {}", 
                        songFile.getAbsolutePath(),
                        durationSeconds,
                        frames,
                        format.getFrameRate(),
                        format);
                
                return (int) durationSeconds;
            }
        } catch (Exception e) {
            log.error("使用AudioSystem提取FLAC格式时长失败: {}, 错误: {}", songFile.getAbsolutePath(), e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * 提取WAV格式歌曲时长
     * @param songFile 歌曲文件
     * @return 时长（秒）
     */
    private int extractWavDuration(File songFile) {
        try {
            AudioFile audioFile = AudioFileIO.read(songFile);
            if (audioFile != null && audioFile.getAudioHeader() != null) {
                // WAV格式的时长可能需要通过其他方式获取
                // 尝试使用getAudioHeader().getTrackLength()或通过音频数据计算
                int duration = audioFile.getAudioHeader().getTrackLength();
                log.info("WAV备用方法提取时长: {}, 时长: {}秒, 格式: {}, 采样率: {}, 声道数: {}", 
                        songFile.getAbsolutePath(),
                        duration,
                        audioFile.getAudioHeader().getFormat(),
                        audioFile.getAudioHeader().getSampleRateAsNumber(),
                        audioFile.getAudioHeader().getChannels());
                if (duration > 0) {
                    return duration;
                }
                
                // 如果getTrackLength()返回0，尝试通过音频数据计算
                // WAV文件的时长可以通过文件大小、采样率、位深、声道数计算
                // 公式：时长(秒) = (文件大小-头信息大小) / (采样率 * 位深/8 * 声道数 * 1.0)
                // 但由于WAV文件有头信息，需要减去头信息的大小
                // 这是一个近似计算，可能不准确
                
                // 尝试获取音频头信息
                log.info("WAV文件时长计算: 文件大小={}, 采样率={}, 声道数={}", 
                        songFile.length(), 
                        audioFile.getAudioHeader().getSampleRateAsNumber(),
                        audioFile.getAudioHeader().getChannels());
                
                // 对于WAV格式，如果getTrackLength()返回0，可以尝试使用其他方法
                // 由于Jaudiotagger对WAV的支持有限，这里返回0，表示无法提取时长
                return 0;
            }
        } catch (Exception e) {
            log.error("提取WAV格式时长失败: {}, 错误: {}", songFile.getAbsolutePath(), e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * 使用Java AudioSystem提取WAV格式歌曲时长
     * 这是Jaudiotagger失败后的备用方法
     * @param songFile 歌曲文件
     * @return 时长（秒）
     */
    private int extractWavDurationUsingAudioSystem(File songFile) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(songFile)) {
            AudioFormat format = audioStream.getFormat();
            long frames = audioStream.getFrameLength();
            double durationSeconds = (double) frames / format.getFrameRate();
            
            log.info("使用AudioSystem提取WAV时长: {}, 时长: {}秒, 帧数: {}, 帧率: {}", 
                    songFile.getAbsolutePath(),
                    durationSeconds,
                    frames,
                    format.getFrameRate());
            
            return (int) durationSeconds;
        } catch (Exception e) {
            log.error("使用AudioSystem提取WAV格式时长失败: {}, 错误: {}", songFile.getAbsolutePath(), e.getMessage(), e);
        }
        return 0;
    }
    
    /**
     * 查找或创建歌手
     * @param artistName 歌手名称
     * @param allArtists 所有歌手列表
     * @return 歌手对象
     */
    private Artist findOrCreateArtist(String artistName, List<Artist> allArtists) {
        // 查找歌手
        for (Artist artist : allArtists) {
            if (Objects.equals(artist.getName(), artistName)) {
                return artist;
            }
        }
        
        // 创建歌手
        Artist artist = new Artist();
        artist.setName(artistName);
        artist.setNameEn("");
        artist.setAvatar("");
        artist.setDescription("");
        artist.setRegion("");
        artist.setType(1);
        artist.setSortOrder(0);
        artist.setStatus(1);
        
        try {
            artistMapper.insert(artist);
            log.info("创建歌手成功: {}", artistName);
            
            // 将新歌手添加到allArtists列表中，避免重复处理
            allArtists.add(artist);
            
            return artist;
        } catch (Exception e) {
            log.error("创建歌手失败: {}", artistName, e);
            return null;
        }
    }
    
    /**
     * 查找或创建专辑
     * @param albumDir 专辑文件夹
     * @param artistId 歌手ID
     * @param allAlbums 所有专辑列表
     * @return 专辑对象
     */
    private Album findOrCreateAlbum(File albumDir, Long artistId, List<Album> allAlbums) {
        // 专辑名称
        String albumName = albumDir.getName();
        
        // 计算专辑的文件夹路径
        String folderPath = albumDir.getAbsolutePath().replace(storageConfig.getBasePath(), "");
        if (folderPath.startsWith(File.separator)) {
            folderPath = folderPath.substring(1);
        }
        
        // 查找专辑，优先使用folderPath字段
        for (Album album : allAlbums) {
            if (Objects.equals(album.getFolderPath(), folderPath)) {
                return album;
            }
        }
        
        // 创建专辑
        Album album = new Album();
        album.setArtistId(artistId);
        album.setName(albumName);
        album.setFolderPath(folderPath);
        
        // 查找封面图片
        String coverImage = findCoverImage(albumDir);
        album.setCoverImage(coverImage);
        
        album.setReleaseDate(null);
        album.setDescription("");
        album.setAlbumType(1);
        album.setSortOrder(0);
        album.setStatus(1);
        
        try {
            albumMapper.insert(album);
            log.info("创建专辑成功: {}, folderPath={}", albumName, folderPath);
            
            // 将新专辑添加到allAlbums列表中，避免重复处理
            allAlbums.add(album);
            
            return album;
        } catch (Exception e) {
            log.error("创建专辑失败: {}", albumName, e);
            return null;
        }
    }
    
    /**
     * 查找专辑文件夹中的封面图片
     * @param albumDir 专辑文件夹
     * @return 封面图片路径
     */
    private String findCoverImage(File albumDir) {
        // 支持的图片文件扩展名
        List<String> imageExtensions = List.of("jpg", "jpeg", "png", "gif", "bmp");
        
        // 查找专辑文件夹中的封面文件
        File[] files = albumDir.listFiles();
        if (files == null) {
            return "";
        }
        
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                String extension = StringUtils.getFilenameExtension(fileName);
                if (extension != null && imageExtensions.contains(extension) && 
                    (fileName.startsWith("cover.") || fileName.startsWith("folder."))) {
                    // 构建封面图片路径
                    String coverPath = file.getAbsolutePath().replace(storageConfig.getBasePath(), "");
                    if (coverPath.startsWith(File.separator)) {
                        coverPath = coverPath.substring(1);
                    }
                    // 将路径分隔符统一替换为正斜杠，确保在URL中正确使用
                    coverPath = coverPath.replace(File.separator, "/");
                    return coverPath;
                }
            }
        }
        
        // 如果没有找到封面图片，返回空字符串
        return "";
    }
    
    /**
     * 检查歌曲是否已存在
     * @param filePath 歌曲文件路径
     * @param allSongs 所有歌曲列表
     * @return 是否存在
     */
    private boolean songExists(String filePath, List<Song> allSongs) {
        for (Song song : allSongs) {
            if (Objects.equals(song.getFilePath(), filePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 查找专辑文件夹中的歌曲文件
     * @param albumDir 专辑文件夹
     * @return 歌曲文件列表
     */
    private List<File> findSongFiles(File albumDir) {
        List<File> songFiles = new ArrayList<>();
        
        // 支持的音频文件扩展名
        List<String> audioExtensions = List.of("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "dsf");
        
        // 遍历专辑文件夹中的所有文件
        File[] files = albumDir.listFiles();
        if (files == null) {
            return songFiles;
        }
        
        for (File file : files) {
            if (file.isFile()) {
                String extension = StringUtils.getFilenameExtension(file.getName());
                if (extension != null && audioExtensions.contains(extension.toLowerCase())) {
                    songFiles.add(file);
                }
            }
        }
        
        return songFiles;
    }
    
    /**
     * 扫描结果实现
     */
    private static class ScanResultImpl implements ScanResult {
        private int addedSongs;
        private int updatedSongs;
        private int skippedSongs;
        private final List<String> errors;
        
        public ScanResultImpl() {
            this.addedSongs = 0;
            this.updatedSongs = 0;
            this.skippedSongs = 0;
            this.errors = new ArrayList<>();
        }
        
        public void incrementAddedSongs() {
            this.addedSongs++;
        }
        
        public void incrementUpdatedSongs() {
            this.updatedSongs++;
        }
        
        public void incrementSkippedSongs() {
            this.skippedSongs++;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        @Override
        public int getAddedSongs() {
            return addedSongs;
        }
        
        @Override
        public int getUpdatedSongs() {
            return updatedSongs;
        }
        
        @Override
        public int getSkippedSongs() {
            return skippedSongs;
        }
        
        @Override
        public List<String> getErrors() {
            return errors;
        }
    }
    
    /**
     * 提取DSF格式文件的时长
     * @param file DSF文件
     * @return 时长（秒），如果提取失败返回0
     */
    private int extractDsfDuration(File file) {
        try {
            // DSF文件头部结构：
            // 偏移量0x00-0x03: "DSD "标识符
            // 偏移量0x04-0x0B: 文件大小(8字节，小端序)
            // 偏移量0x0C-0x13: ID3块大小(8字节，小端序)
            // 偏移量0x14-0x1B: 格式版本(8字节，小端序)
            // 偏移量0x1C-0x1F: 采样率(4字节，小端序)
            // 偏移量0x20-0x23: 声道数(4字节，小端序)
            // 偏移量0x24-0x2B: 位数/样本(8字节，小端序)
            // 偏移量0x2C-0x33: 采样块数(8字节，小端序)
            
            byte[] header = new byte[64];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                if (fis.read(header) >= 64) {
                    // 检查DSF标识符
                    if (header[0] == 'D' && header[1] == 'S' && header[2] == 'D' && header[3] == ' ') {
                        // 读取采样率 (偏移量0x1C，小端序)
                        int sampleRate = ((header[28] & 0xFF) | 
                                        ((header[29] & 0xFF) << 8) | 
                                        ((header[30] & 0xFF) << 16) | 
                                        ((header[31] & 0xFF) << 24));
                        
                        // 读取声道数 (偏移量0x20，小端序)
                        int channels = ((header[32] & 0xFF) | 
                                      ((header[33] & 0xFF) << 8) | 
                                      ((header[34] & 0xFF) << 16) | 
                                      ((header[35] & 0xFF) << 24));
                        
                        // 读取采样块数 (偏移量0x2C-0x33，小端序)
                        long sampleCount = ((header[44] & 0xFFL) |
                                          ((header[45] & 0xFFL) << 8) |
                                          ((header[46] & 0xFFL) << 16) |
                                          ((header[47] & 0xFFL) << 24) |
                                          ((header[48] & 0xFFL) << 32) |
                                          ((header[49] & 0xFFL) << 40) |
                                          ((header[50] & 0xFFL) << 48) |
                                          ((header[51] & 0xFFL) << 56));
                        
                        // DSF文件的采样块数需要除以声道数得到实际的采样点数
                        long actualSampleCount = sampleCount / Math.max(1, channels);
                        
                        // 计算时长（秒）
                        if (sampleRate > 0) {
                            double durationSeconds = (double) actualSampleCount / sampleRate;
                            int duration = (int) Math.round(durationSeconds);
                            
                            // 验证时长合理性（避免异常值）
                            if (duration > 0 && duration < 86400) { // 24小时以内
                                log.info("DSF文件信息 - 采样率: {}Hz, 声道数: {}, 总采样块数: {}, 实际采样数: {}, 时长: {}秒", 
                                       sampleRate, channels, sampleCount, actualSampleCount, duration);
                                return duration;
                            } else {
                                log.warn("DSF文件时长异常: {}秒，使用默认值", duration);
                                // 使用文件大小估算时长作为备选方案
                                return estimateDsfDurationFromFileSize(file, sampleRate);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取DSF文件时长失败: {}", e.getMessage());
        }
        
        // 备用方案：根据文件大小估算时长
        return estimateDsfDurationFromFileSize(file, 2822400); // 默认DSD64采样率
    }
    
    /**
     * 根据文件大小估算DSF时长
     * @param file DSF文件
     * @param sampleRate 采样率
     * @return 估算的时长（秒）
     */
    private int estimateDsfDurationFromFileSize(File file, int sampleRate) {
        try {
            long fileSize = file.length();
            // DSF文件大约每秒占用 2822400 * 1 * 8 / 8 = 2822400 字节（单声道）
            // 实际占用会更大，因为包含头部和其他数据
            double bytesPerSecond = sampleRate * 1; // 1位深度
            if (bytesPerSecond > 0) {
                int estimatedDuration = (int) (fileSize / bytesPerSecond);
                log.info("DSF文件时长估算 - 文件大小: {} bytes, 采样率: {}Hz, 估算时长: {}秒", 
                       fileSize, sampleRate, estimatedDuration);
                return estimatedDuration;
            }
        } catch (Exception e) {
            log.warn("估算DSF文件时长失败: {}", e.getMessage());
        }
        return 0;
    }
}