package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.LostDto.ClaimHandleReq;
import com.campus.assistant.service.LostItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认领处理接口（契约 2.4）：发布者接受/拒绝。
 */
@RestController
@RequestMapping("/api/mp/claims")
@RequiredArgsConstructor
public class MpClaimController {

    private final LostItemService service;

    @PutMapping("/{id}/handle")
    public ApiResponse<?> handle(@PathVariable long id, @RequestBody ClaimHandleReq req) {
        service.handleClaim(UserContext.uid(), id, req.getAction(), req.getRemark());
        return ApiResponse.ok();
    }
}
