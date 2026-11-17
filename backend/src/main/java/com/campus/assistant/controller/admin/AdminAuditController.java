package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.dto.AdminDto.AuditHandleReq;
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
 * 管理端审核与举报接口（契约 3）：待审列表、审核处理（写 t_audit_record + 发消息）、举报处理。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "AUDITOR"})
public class AdminAuditController {

    private final AdminService adminService;

    @GetMapping("/audits")
    public ApiResponse<?> audits(@RequestParam(required = false) String targetType,
                                 @RequestParam(defaultValue = "PENDING") String status,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.auditList(targetType, page, size));
    }

    @PutMapping("/audits/handle")
    public ApiResponse<?> handle(@RequestBody AuditHandleReq req) {
        adminService.handleAudit(req);
        return ApiResponse.ok();
    }

    @GetMapping("/reports")
    public ApiResponse<?> reports(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        var p = adminService.listReports(status, page, size);
        return ApiResponse.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PutMapping("/reports/{id}/handle")
    public ApiResponse<?> handleReport(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminService.handleReport(id, (String) body.get("handleResult"),
                (String) body.getOrDefault("status", "HANDLED"));
        return ApiResponse.ok();
    }
}
