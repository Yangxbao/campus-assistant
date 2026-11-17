package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.entity.ClassroomDomain.Building;
import com.campus.assistant.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端楼宇接口（契约 3）：CRUD（含楼层）。
 */
@RestController
@RequestMapping("/api/admin/buildings")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "MAINTAINER"})
public class AdminBuildingController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(adminService.listBuildings());
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        Building b = new Building();
        b.setCode((String) body.get("code"));
        b.setName((String) body.get("name"));
        b.setFloorsCount(intOf(body.get("floorsCount")));
        b.setDescription((String) body.get("description"));
        b.setCampus((String) body.get("campus"));
        b.setLocationDesc((String) body.get("locationDesc"));
        @SuppressWarnings("unchecked")
        List<Object> floorObjs = (List<Object>) body.get("floors");
        List<Integer> floors = floorObjs == null ? null
                : floorObjs.stream().map(o -> Integer.valueOf(String.valueOf(o))).toList();
        return ApiResponse.ok(Map.of("id", adminService.createBuilding(b, floors)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Building b = new Building();
        b.setCode((String) body.get("code"));
        b.setName((String) body.get("name"));
        b.setFloorsCount(intOf(body.get("floorsCount")));
        b.setDescription((String) body.get("description"));
        b.setCampus((String) body.get("campus"));
        b.setLocationDesc((String) body.get("locationDesc"));
        @SuppressWarnings("unchecked")
        List<Object> floorObjs = (List<Object>) body.get("floors");
        List<Integer> floors = floorObjs == null ? null
                : floorObjs.stream().map(o -> Integer.valueOf(String.valueOf(o))).toList();
        adminService.updateBuilding(id, b, floors);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        adminService.deleteBuilding(id);
        return ApiResponse.ok();
    }

    private static Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
