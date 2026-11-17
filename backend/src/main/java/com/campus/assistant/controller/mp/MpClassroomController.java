package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.Dto.SearchQuery;
import com.campus.assistant.service.BuildingClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 教室接口（契约 2.2）：搜索筛选、详情、照片、插座标记、评价、临时状态、申请新增、
 * 最近浏览、最新更新。管理端 CRUD 复用 AdminService 的校验逻辑。
 */
@RestController
@RequestMapping("/api/mp/classrooms")
@RequiredArgsConstructor
public class MpClassroomController {

    private final BuildingClassroomService service;

    @GetMapping("/search")
    public ApiResponse<?> search(SearchQuery q) {
        return ApiResponse.ok(service.search(q));
    }

    @GetMapping("/recent")
    public ApiResponse<?> recent(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.recentViewed(UserContext.uid(), limit));
    }

    @GetMapping("/latest-updated")
    public ApiResponse<?> latestUpdated(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(service.latestUpdated(limit));
    }

    @PostMapping("/apply-new")
    public ApiResponse<?> applyNew(@RequestBody Map<String, Object> body) {
        Long buildingId = body.get("buildingId") == null ? null : Long.valueOf(String.valueOf(body.get("buildingId")));
        Integer floorNo = body.get("floorNo") == null ? null : Integer.valueOf(String.valueOf(body.get("floorNo")));
        String roomNo = (String) body.get("roomNo");
        String remark = (String) body.get("remark");
        return ApiResponse.ok(service.applyNewClassroom(UserContext.uid(), buildingId, floorNo, roomNo, remark));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable long id) {
        return ApiResponse.ok(service.classroomDetail(UserContext.uid(), id));
    }

    @GetMapping("/{id}/images")
    public ApiResponse<?> images(@PathVariable long id) {
        return ApiResponse.ok(service.listImages(id));
    }

    @PostMapping("/{id}/images")
    public ApiResponse<?> addImage(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Long newId = service.addImage(UserContext.uid(), id,
                (String) body.get("ossKey"), (String) body.get("category"), (String) body.get("description"));
        return ApiResponse.ok(Map.of("id", newId, "auditStatus", "PENDING"));
    }

    @PostMapping("/{id}/socket-marks")
    public ApiResponse<?> addSocketMark(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Long imageId = body.get("imageId") == null ? null : Long.valueOf(String.valueOf(body.get("imageId")));
        BigDecimal posX = body.get("posX") == null ? null : new BigDecimal(String.valueOf(body.get("posX")));
        BigDecimal posY = body.get("posY") == null ? null : new BigDecimal(String.valueOf(body.get("posY")));
        Long newId = service.addSocketMark(UserContext.uid(), id, imageId, posX, posY,
                (String) body.get("wallPos"), (String) body.get("note"));
        return ApiResponse.ok(Map.of("id", newId));
    }

    @GetMapping("/{id}/socket-marks")
    public ApiResponse<?> socketMarks(@PathVariable long id) {
        return ApiResponse.ok(service.listSocketMarks(id));
    }

    @PostMapping("/{id}/equipment-reports")
    public ApiResponse<?> equipmentReport(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Long newId = service.addEquipmentReport(UserContext.uid(), id,
                (String) body.get("fieldName"), (String) body.get("newValue"), (String) body.get("remark"));
        return ApiResponse.ok(Map.of("id", newId, "auditStatus", "PENDING"));
    }

    @PostMapping("/{id}/reviews")
    public ApiResponse<?> addReview(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Integer rating = body.get("rating") == null ? null : Integer.valueOf(String.valueOf(body.get("rating")));
        Long newId = service.addReview(UserContext.uid(), id, rating, (String) body.get("content"));
        return ApiResponse.ok(Map.of("id", newId));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<?> reviews(@PathVariable long id,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.listReviews(id, page, size));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<?> postStatus(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Integer hoursValid = body.get("hoursValid") == null ? null
                : Integer.valueOf(String.valueOf(body.get("hoursValid")));
        Integer quietLevel = body.get("quietLevel") == null ? null
                : Integer.valueOf(String.valueOf(body.get("quietLevel")));
        return ApiResponse.ok(service.postStatus(UserContext.uid(), id,
                (String) body.get("occupancy"), quietLevel,
                intOf(body.get("acWorking")), intOf(body.get("socketWorking")),
                (String) body.get("occupiedReason"), hoursValid));
    }

    @GetMapping("/{id}/status/latest")
    public ApiResponse<?> latestStatus(@PathVariable long id) {
        return ApiResponse.ok(service.latestStatus(id));
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
