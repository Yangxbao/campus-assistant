package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.service.BuildingClassroomService;
import com.campus.assistant.service.CommunityService;
import com.campus.assistant.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 收藏、公告、上传接口（契约 2.5 / 2.6）。
 */
@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpCommunityController {

    private final CommunityService communityService;
    private final UploadService uploadService;
    private final BuildingClassroomService buildingClassroomService;

    // ---------- 收藏 ----------

    @PostMapping("/favorites")
    public ApiResponse<?> addFavorite(@RequestBody Map<String, Object> body) {
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(String.valueOf(body.get("targetId")));
        return ApiResponse.ok(Map.of("id", communityService.addFavorite(UserContext.uid(),
                (String) body.get("targetType"), targetId)));
    }

    @DeleteMapping("/favorites")
    public ApiResponse<?> removeFavorite(@RequestParam String targetType, @RequestParam Long targetId) {
        communityService.removeFavorite(UserContext.uid(), targetType, targetId);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites")
    public ApiResponse<?> favorites(@RequestParam(required = false) String type) {
        return ApiResponse.ok(communityService.listFavorites(UserContext.uid(), type));
    }

    // ---------- 公告 ----------

    @GetMapping("/announcements")
    public ApiResponse<?> announcements() {
        return ApiResponse.ok(communityService.announcements());
    }

    // ---------- 插座标记反馈（契约 2.2） ----------

    @PostMapping("/socket-marks/{id}/feedback")
    public ApiResponse<?> socketMarkFeedback(@PathVariable long id, @RequestBody Map<String, Object> body) {
        boolean accurate = Boolean.parseBoolean(String.valueOf(body.get("accurate")));
        buildingClassroomService.feedbackSocketMark(UserContext.uid(), id, accurate);
        return ApiResponse.ok();
    }

    // ---------- 上传（契约 2.6） ----------

    @PostMapping("/upload/image")
    public ApiResponse<?> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(uploadService.uploadImage(UserContext.uid(), file));
    }

    @PostMapping("/upload/image-base64")
    public ApiResponse<?> uploadBase64(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(uploadService.uploadBase64(UserContext.uid(),
                (String) body.get("base64"), (String) body.get("ext")));
    }
}
