package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.dto.AuthDto.AdminLoginReq;
import com.campus.assistant.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台登录（契约 3）：bcrypt 校验 + 失败计数锁定。
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody AdminLoginReq req) {
        return ApiResponse.ok(authService.adminLogin(req.getUsername(), req.getPassword()));
    }
}
