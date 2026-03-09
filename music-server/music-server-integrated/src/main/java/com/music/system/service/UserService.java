package com.music.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.LoginDTO;
import com.music.api.dto.RegisterDTO;
import com.music.api.vo.LoginVO;
import com.music.api.vo.UserVO;
import com.music.system.entity.User;

public interface UserService extends IService<User> {
    
    LoginVO login(LoginDTO dto);
    
    void register(RegisterDTO dto);
    
    UserVO getUserInfo(Long userId);
    
    UserVO updateUserInfo(Long userId, UserVO userVO);
    
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
