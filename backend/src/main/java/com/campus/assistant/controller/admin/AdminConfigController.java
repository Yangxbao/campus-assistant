package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端系统配置（契约 3）：key-value 查询与更新。
 */
@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN"})
public class AdminConfigController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminService.listConfigs());
    }

    @PutMapping
    public ApiResponse<?> update(@RequestBody Map<String, String> configs) {
        adminService.updateConfigs(configs);
        return ApiResponse.ok();
    }
}
