package com.music.common.constant;

public interface CacheConstants {
    String LOGIN_TOKEN_KEY = "login:token:";
    String USER_INFO_KEY = "user:info:";
    String CAPTCHA_CODE_KEY = "captcha:code:";
    String SONG_DETAIL_KEY = "song:detail:";
    String ALBUM_DETAIL_KEY = "album:detail:";
    String ARTIST_DETAIL_KEY = "artist:detail:";
    String HOT_SONGS_KEY = "songs:hot:";
    String NEW_SONGS_KEY = "songs:new:";
    
    long DEFAULT_EXPIRE_TIME = 3600;
    long TOKEN_EXPIRE_TIME = 7200;
    long CACHE_EXPIRE_TIME = 1800;
}
