package com.music.miniprogram.controller;

import com.music.common.core.domain.Result;
import com.music.miniprogram.dto.MpPhoneDTO;
import com.music.miniprogram.service.MiniprogramAuthService;
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

@Tag(name = "小程序手机号接口")
@RestController
@RequestMapping("/api/mp/phone")
@RequiredArgsConstructor
public class MiniprogramPhoneController {

    private static final Logger log = LoggerFactory.getLogger(MiniprogramPhoneController.class);

    private final MiniprogramAuthService miniprogramAuthService;

    @Operation(summary = "解密手机号")
    @PostMapping("/decode")
    public Result<String> decode(@Valid @RequestBody MpPhoneDTO dto) {
        log.info("访问接口：小程序手机号解密");
        return Result.success(miniprogramAuthService.getPhoneByCode(dto.getCode()));
    }
}
