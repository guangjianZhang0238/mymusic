package com.music.common.constant;

public interface MusicConstants {
    String DEFAULT_STORAGE_PATH = "D:\\music_source";
    String DEFAULT_TEMP_PATH = "D:\\music_temp";
    
    String[] SUPPORTED_AUDIO_FORMATS = {"wav", "flac", "mp3", "aac", "m4a", "ogg", "dsf"};
    String[] SUPPORTED_LYRICS_FORMATS = {"lrc", "txt"};
    String[] SUPPORTED_IMAGE_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};
    
    int[] SUPPORTED_SAMPLE_RATES = {44100, 48000, 88200, 96000, 176400, 192000, 2822400, 5644800};
    int[] SUPPORTED_BIT_DEPTHS = {1, 16, 24, 32};
    
    int STATUS_DISABLED = 0;
    int STATUS_ENABLED = 1;
    
    int ARTIST_TYPE_SOLO = 0;
    int ARTIST_TYPE_GROUP = 1;
    
    int ALBUM_TYPE_ALBUM = 0;
    int ALBUM_TYPE_EP = 1;
    int ALBUM_TYPE_SINGLE = 2;
    int ALBUM_TYPE_COMPILATION = 3;
    
    int LYRICS_TYPE_PLAIN = 0;
    int LYRICS_TYPE_LRC = 1;
    
    int PLAY_MODE_SEQUENCE = 0;
    int PLAY_MODE_LOOP = 1;
    int PLAY_MODE_SINGLE = 2;
    int PLAY_MODE_SHUFFLE = 3;
    
    int FAVORITE_TYPE_SONG = 1;
    int FAVORITE_TYPE_ALBUM = 2;
    int FAVORITE_TYPE_ARTIST = 3;
}
