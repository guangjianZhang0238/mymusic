package com.music.miniprogram.service;

import com.music.miniprogram.dto.MpLoginDTO;
import com.music.miniprogram.vo.MpLoginVO;

public interface MiniprogramAuthService {

    MpLoginVO login(MpLoginDTO dto);

    String getPhoneByCode(String phoneCode);
}
