package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端统计接口（契约 3）：总览、每日统计、贡献排行。
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN"})
public class AdminStatsController {

    private final AdminService adminService;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        return ApiResponse.ok(adminService.statsOverview());
    }

    @GetMapping("/daily")
    public ApiResponse<?> daily(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(adminService.dailyStats(days));
    }

    @GetMapping("/contributors")
    public ApiResponse<?> contributors() {
        return ApiResponse.ok(adminService.contributors());
    }
}
