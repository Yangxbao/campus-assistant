package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.dto.AuthDto.WxLoginReq;
import com.campus.assistant.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序登录（契约 2.1）：未配置微信 AppSecret 时走开发模式，响应明示 devMode:true。
 */
@RestController
@RequestMapping("/api/mp/auth")
@RequiredArgsConstructor
public class MpAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody WxLoginReq req) {
        return ApiResponse.ok(authService.wxLogin(req == null ? null : req.getCode()));
    }
}
