package com.campus.assistant.controller.mp;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.dto.TimetableDto.CourseReq;
import com.campus.assistant.dto.TimetableDto.OcrConfirmReq;
import com.campus.assistant.dto.TimetableDto.RemindConfigReq;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.service.TimetableService;
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
 * 课程表接口（契约 2.3 / 2.7）：学期/节次/课程 CRUD、周次过滤、今日课程、OCR 链路、提醒配置。
 */
@RestController
@RequestMapping("/api/mp/timetable")
@RequiredArgsConstructor
public class MpTimetableController {

    private final TimetableService service;

    @GetMapping("/semesters")
    public ApiResponse<?> semesters() {
        return ApiResponse.ok(service.listSemesters());
    }

    /** 仅管理员（小程序 auditor/maintainer 角色）可创建学期 */
    @PostMapping("/semesters")
    public ApiResponse<?> createSemester(@RequestBody Map<String, Object> body) {
        String role = UserContext.get() == null ? "user" : UserContext.get().getRole();
        if (!java.util.List.of("auditor", "maintainer").contains(role)) {
            return com.campus.assistant.common.ApiResponse.fail(403, "当前角色无权创建学期");
        }
        Long id = service.createSemester((String) body.get("name"), (String) body.get("startDate"),
                intOf(body.get("totalWeeks")), intOf(body.get("isCurrent")));
        return com.campus.assistant.common.ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping("/periods")
    public ApiResponse<?> periods(@RequestParam(required = false) Long semesterId) {
        return ApiResponse.ok(service.listPeriods(semesterId));
    }

    @GetMapping("/courses")
    public ApiResponse<?> courses(@RequestParam(required = false) Long semesterId,
                                  @RequestParam(required = false) Integer week) {
        return ApiResponse.ok(service.listCourses(UserContext.uid(), semesterId, week));
    }

    @PostMapping("/courses")
    public ApiResponse<?> addCourse(@RequestBody CourseReq req) {
        return ApiResponse.ok(Map.of("id", service.addCourse(UserContext.uid(), req)));
    }

    @PutMapping("/courses/{id}")
    public ApiResponse<?> updateCourse(@PathVariable long id, @RequestBody CourseReq req) {
        service.updateCourse(UserContext.uid(), id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<?> deleteCourse(@PathVariable long id) {
        service.deleteCourse(UserContext.uid(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/today")
    public ApiResponse<?> today(@RequestParam(required = false) Long semesterId,
                                @RequestParam(required = false) Integer week) {
        return ApiResponse.ok(service.today(UserContext.uid(), semesterId, week));
    }

    @GetMapping("/weeks-current")
    public ApiResponse<?> weeksCurrent() {
        var sem = service.currentSemester();
        return ApiResponse.ok(Map.of(
                "semesterId", sem == null ? null : sem.getId(),
                "week", sem == null ? 0 : service.weekOf(sem, java.time.LocalDate.now())));
    }

    @PostMapping("/ocr")
    public ApiResponse<?> ocr(@RequestBody Map<String, Object> body) {
        Long taskId = service.createOcrTask(UserContext.uid(), (String) body.get("ossKey"));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @GetMapping("/ocr/{taskId}")
    public ApiResponse<?> ocrResult(@PathVariable long taskId) {
        return ApiResponse.ok(service.ocrResult(UserContext.uid(), taskId));
    }

    /** 用户在校正页核对修改后正式保存 */
    @PostMapping("/ocr/{taskId}/confirm")
    public ApiResponse<?> ocrConfirm(@PathVariable long taskId, @RequestBody OcrConfirmReq req) {
        int saved = service.confirmOcrCourses(UserContext.uid(), taskId,
                req == null ? null : req.getCourses());
        return ApiResponse.ok(Map.of("saved", saved));
    }

    // ---------- 提醒（契约 2.7） ----------

    @PostMapping("/remind-config")
    public ApiResponse<?> remindConfig(@RequestBody RemindConfigReq req) {
        service.saveRemindConfig(UserContext.uid(), req.getEnabled(), req.getMinutesBefore());
        return ApiResponse.ok();
    }

    /** 记录订阅；微信模板未配置时标记 notConfigured，不假装已推送 */
    @PostMapping("/remind/subscribe-record")
    public ApiResponse<?> subscribeRecord(@RequestBody Map<String, Object> body) {
        String templateId = (String) body.get("templateId");
        boolean notConfigured = templateId == null || templateId.isBlank();
        return ApiResponse.ok(Map.of("recorded", true, "notConfigured", notConfigured));
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
