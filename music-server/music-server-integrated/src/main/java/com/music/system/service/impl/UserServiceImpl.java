package com.music.system.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.api.dto.LoginDTO;
import com.music.api.dto.RegisterDTO;
import com.music.api.vo.LoginVO;
import com.music.api.vo.UserVO;
import com.music.common.exception.BusinessException;
import com.music.common.utils.JwtUtils;
import com.music.system.entity.User;
import com.music.system.mapper.UserMapper;
import com.music.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final JwtUtils jwtUtils;
    
    @Override
    public LoginVO login(LoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw BusinessException.of("密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw BusinessException.of("账号已被禁用");
        }
        
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userVO);
        
        return loginVO;
    }
    
    @Override
    public void register(RegisterDTO dto) {
        Long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        
        if (count > 0) {
            throw BusinessException.of("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        user.setRole(0);
        
        save(user);
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
    
    @Override
    public UserVO updateUserInfo(Long userId, UserVO userVO) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        
        if (userVO.getNickname() != null) {
            user.setNickname(userVO.getNickname());
        }
        if (userVO.getAvatar() != null) {
            user.setAvatar(userVO.getAvatar());
        }
        if (userVO.getPhone() != null) {
            user.setPhone(userVO.getPhone());
        }
        if (userVO.getEmail() != null) {
            user.setEmail(userVO.getEmail());
        }
        
        updateById(user);
        
        UserVO result = new UserVO();
        BeanUtils.copyProperties(user, result);
        return result;
    }
    
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw BusinessException.of("原密码错误");
        }
        
        user.setPassword(BCrypt.hashpw(newPassword));
        updateById(user);
    }
}
