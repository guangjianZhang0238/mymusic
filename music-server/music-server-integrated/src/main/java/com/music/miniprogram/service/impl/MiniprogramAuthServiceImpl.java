package com.music.miniprogram.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.music.api.vo.UserVO;
import com.music.common.exception.BusinessException;
import com.music.common.utils.JwtUtils;
import com.music.miniprogram.dto.MpLoginDTO;
import com.music.miniprogram.service.MiniprogramAuthService;
import com.music.miniprogram.vo.MpLoginVO;
import com.music.miniprogram.vo.WxPhoneResult;
import com.music.miniprogram.vo.WxSessionResult;
import com.music.system.entity.User;
import com.music.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MiniprogramAuthServiceImpl implements MiniprogramAuthService {

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Value("${music.miniprogram.appid}")
    private String appId;

    @Value("${music.miniprogram.secret}")
    private String secret;

    @Override
    public MpLoginVO login(MpLoginDTO dto) {
        WxSessionResult session = fetchSession(dto.getCode());
        if (session == null || !StringUtils.hasText(session.getOpenId())) {
            throw BusinessException.of("微信登录失败");
        }
        String phone = getPhoneByCode(dto.getPhoneCode());
        if (!StringUtils.hasText(phone)) {
            throw BusinessException.of("手机号获取失败");
        }

        User user = findUserByUnionOrPhone(session.getUnionId(), phone);
        if (user == null) {
            user = registerUser(dto, session, phone);
        } else {
            updateUserWechatInfo(user, dto, session, phone);
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        MpLoginVO loginVO = new MpLoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userVO);
        return loginVO;
    }

    @Override
    public String getPhoneByCode(String phoneCode) {
        String accessToken = fetchAccessToken();
        if (!StringUtils.hasText(accessToken)) {
            throw BusinessException.of("获取微信AccessToken失败");
        }
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + encode(accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("code", phoneCode);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        WxPhoneResult result = restTemplate.postForObject(url, request, WxPhoneResult.class);
        if (result == null || result.getPhoneInfo() == null || !StringUtils.hasText(result.getPhoneInfo().getPhoneNumber())) {
            return null;
        }
        return result.getPhoneInfo().getPhoneNumber();
    }

    private WxSessionResult fetchSession(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + encode(appId) +
                "&secret=" + encode(secret) +
                "&js_code=" + encode(code) +
                "&grant_type=authorization_code";
        return restTemplate.getForObject(url, WxSessionResult.class);
    }

    private String fetchAccessToken() {
        String url = "https://api.weixin.qq.com/cgi-bin/token" +
                "?grant_type=client_credential" +
                "&appid=" + encode(appId) +
                "&secret=" + encode(secret);
        Map<?, ?> result = restTemplate.getForObject(url, Map.class);
        if (result == null) {
            return null;
        }
        Object token = result.get("access_token");
        return token == null ? null : token.toString();
    }

    private User findUserByUnionOrPhone(String unionId, String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(unionId)) {
            wrapper.eq(User::getUnionId, unionId);
        } else {
            wrapper.eq(User::getPhone, phone);
        }
        return userMapper.selectOne(wrapper);
    }

    private User registerUser(MpLoginDTO dto, WxSessionResult session, String phone) {
        User user = new User();
        user.setUsername(buildUsername(phone));
        user.setPassword("");
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "音乐听友");
        user.setAvatar(dto.getAvatar());
        user.setPhone(phone);
        user.setUnionId(session.getUnionId());
        user.setOpenId(session.getOpenId());
        user.setStatus(1);
        user.setRole(0);
        userMapper.insert(user);
        return user;
    }

    private void updateUserWechatInfo(User user, MpLoginDTO dto, WxSessionResult session, String phone) {
        if (StringUtils.hasText(session.getUnionId())) {
            user.setUnionId(session.getUnionId());
        }
        if (StringUtils.hasText(session.getOpenId())) {
            user.setOpenId(session.getOpenId());
        }
        if (StringUtils.hasText(phone)) {
            user.setPhone(phone);
        }
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
    }

    private String buildUsername(String phone) {
        return "mp_" + phone;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
