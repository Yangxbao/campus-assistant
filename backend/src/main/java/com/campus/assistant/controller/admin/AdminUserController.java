package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端用户管理（契约 3）：分页查询、封禁/解封、详情+违规记录。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN"})
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        var p = adminService.listUsers(keyword, status, page, size);
        return ApiResponse.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable long id) {
        return ApiResponse.ok(adminService.userDetail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminService.updateUserStatus(id, (String) body.get("status"));
        return ApiResponse.ok();
    }
}
