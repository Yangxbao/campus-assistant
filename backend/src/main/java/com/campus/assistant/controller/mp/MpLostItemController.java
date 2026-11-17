package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.LostDto.ClaimHandleReq;
import com.campus.assistant.dto.LostDto.ClaimReq;
import com.campus.assistant.dto.LostDto.LostItemReq;
import com.campus.assistant.service.LostItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 失物招领接口（契约 2.4）：发布/列表/详情/编辑/删除/状态/认领/匹配。
 */
@RestController
@RequestMapping("/api/mp/lost-items")
@RequiredArgsConstructor
public class MpLostItemController {

    private final LostItemService service;

    @PostMapping
    public ApiResponse<?> publish(@RequestBody LostItemReq req) {
        return ApiResponse.ok(Map.of("id", service.publish(UserContext.uid(), req),
                "auditStatus", "PENDING"));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String kind,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) Long buildingId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "time_desc") String sort,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) Integer mine,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        var p = service.list(kind, category, buildingId, keyword, status, mine, UserContext.uid(), page, size);
        return ApiResponse.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    /** 匹配流（需先于 /{id} 映射注册路径语义，Spring 按精确度匹配无冲突） */
    @GetMapping("/match-feeds")
    public ApiResponse<?> matchFeeds() {
        return ApiResponse.ok(service.matchFeeds(UserContext.uid()));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable long id) {
        return ApiResponse.ok(service.detail(UserContext.uid(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @RequestBody LostItemReq req) {
        service.update(UserContext.uid(), id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        service.delete(UserContext.uid(), id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> markStatus(@PathVariable long id, @RequestBody Map<String, Object> body) {
        service.markStatus(UserContext.uid(), id, (String) body.get("status"));
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/claims")
    public ApiResponse<?> applyClaim(@PathVariable long id, @RequestBody ClaimReq req) {
        return ApiResponse.ok(Map.of("id", service.applyClaim(UserContext.uid(), id,
                req.getProofDesc(), req.getContact())));
    }

    @GetMapping("/{id}/claims")
    public ApiResponse<?> listClaims(@PathVariable long id) {
        return ApiResponse.ok(service.listClaims(UserContext.uid(), id));
    }

    @GetMapping("/{id}/matches")
    public ApiResponse<?> matches(@PathVariable long id) {
        return ApiResponse.ok(service.matches(UserContext.uid(), id));
    }
}
