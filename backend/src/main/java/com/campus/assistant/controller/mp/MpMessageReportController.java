package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.entity.CommunityDomain.Message;
import com.campus.assistant.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消息与举报接口（契约 2.5）。
 */
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpMessageReportController {

    private final CommunityService communityService;

    // ---------- 消息 ----------

    @GetMapping("/messages")
    public ApiResponse<?> messages(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        var p = communityService.listMessages(UserContext.uid(), page, size);
        return ApiResponse.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PutMapping("/messages/{id}/read")
    public ApiResponse<?> read(@PathVariable long id) {
        communityService.readMessage(UserContext.uid(), id);
        return ApiResponse.ok();
    }

    @PutMapping("/messages/read-all")
    public ApiResponse<?> readAll() {
        communityService.readAll(UserContext.uid());
        return ApiResponse.ok();
    }

    @GetMapping("/messages/unread-count")
    public ApiResponse<?> unreadCount() {
        return ApiResponse.ok(Map.of("count", communityService.unreadCount(UserContext.uid())));
    }

    // ---------- 举报 ----------

    @PostMapping("/reports")
    public ApiResponse<?> report(@RequestBody Map<String, Object> body) {
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(String.valueOf(body.get("targetId")));
        return ApiResponse.ok(Map.of("id", communityService.report(UserContext.uid(),
                (String) body.get("targetType"), targetId,
                (String) body.get("reasonType"), (String) body.get("reasonDesc"))));
    }
}
