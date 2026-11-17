package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.service.BuildingClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扫码进入（契约 2.2）：qr_token → 目标教室/楼宇 id。
 */
@RestController
@RequestMapping("/api/mp/qr")
@RequiredArgsConstructor
public class MpQrController {

    private final BuildingClassroomService service;

    @GetMapping("/{token}")
    public ApiResponse<?> resolve(@PathVariable String token) {
        return ApiResponse.ok(service.resolveQr(token));
    }
}
