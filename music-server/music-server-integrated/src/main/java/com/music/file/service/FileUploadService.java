package com.music.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FileUploadService {
    
    Map<String, Object> uploadFile(MultipartFile file, Long userId);
    
    Map<String, Object> uploadFileWithAlbum(MultipartFile file, Long userId, Long albumId);
    
    Map<String, Object> uploadFiles(List<MultipartFile> files, Long userId);
    
    Map<String, Object> uploadArtistAvatar(MultipartFile file, Long artistId, String artistName);
    
    Map<String, Object> uploadAlbumCover(MultipartFile file, Long albumId, String folderPath);

    Map<String, Object> uploadAlbumCoverByBytes(byte[] fileBytes, String originalFilename, String contentType, Long albumId, String folderPath);
    
    String saveFile(MultipartFile file, String relativePath);
    
    void deleteFile(String filePath);
    
    File getFile(String filePath);
}
