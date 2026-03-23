package com.music.miniprogram.controller;

import com.music.common.core.domain.Result;
import com.music.miniprogram.dto.MpLoginDTO;
import com.music.miniprogram.service.MiniprogramAuthService;
import com.music.miniprogram.vo.MpLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "小程序认证接口")
@RestController
@RequestMapping("/api/mp/auth")
@RequiredArgsConstructor
public class MiniprogramAuthController {

    private static final Logger log = LoggerFactory.getLogger(MiniprogramAuthController.class);

    private final MiniprogramAuthService miniprogramAuthService;

    @Operation(summary = "小程序登录（自动注册并绑定手机号）")
    @PostMapping("/login")
    public Result<MpLoginVO> login(@Valid @RequestBody MpLoginDTO dto) {
        log.info("访问接口：小程序登录");
        return Result.success(miniprogramAuthService.login(dto));
    }
}
