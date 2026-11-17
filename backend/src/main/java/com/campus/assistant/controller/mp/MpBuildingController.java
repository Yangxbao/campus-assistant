package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.service.BuildingClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 楼宇接口（契约 2.2）：列表、详情（按楼层分组教室）、文字导航。
 */
@RestController
@RequestMapping("/api/mp/buildings")
@RequiredArgsConstructor
public class MpBuildingController {

    private final BuildingClassroomService service;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(service.listBuildings());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable long id) {
        return ApiResponse.ok(service.buildingDetail(id));
    }

    /** 文字导航：从楼入口→楼层→教室 */
    @GetMapping("/{id}/guide")
    public ApiResponse<?> guide(@PathVariable long id,
                                @RequestParam(required = false) Long classroomId) {
        if (classroomId == null) {
            return ApiResponse.ok(service.buildingDetail(id));
        }
        return ApiResponse.ok(service.buildingGuide(id, classroomId));
    }
}
