package com.campus.assistant.controller.admin;

import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.RequireRole;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.AdminDto.QrResp;
import com.campus.assistant.entity.ClassroomDomain.Classroom;
import com.campus.assistant.service.AdminService;
import com.campus.assistant.service.BuildingClassroomService;
import com.campus.assistant.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 管理端教室接口（契约 3）：CRUD、Excel/CSV 导入导出、二维码 PNG base64、照片管理。
 */
@RestController
@RequestMapping("/api/admin/classrooms")
@RequiredArgsConstructor
@RequireRole({"SUPER_ADMIN", "MAINTAINER"})
public class AdminClassroomController {

    private final AdminService adminService;
    private final BuildingClassroomService buildingClassroomService;
    private final UploadService uploadService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) Long buildingId,
                               @RequestParam(required = false) Long floorId,
                               @RequestParam(required = false) String roomNo,
                               @RequestParam(required = false) String type,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        var p = adminService.listClassrooms(buildingId, floorId, roomNo, type, page, size);
        return ApiResponse.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        Classroom c = fromBody(body);
        Integer floorNo = intOf(body.get("floorNo"));
        return ApiResponse.ok(Map.of("id", adminService.createClassroom(c, floorNo)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminService.updateClassroom(id, fromBody(body), intOf(body.get("floorNo")));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        adminService.deleteClassroom(id);
        return ApiResponse.ok();
    }

    @PostMapping("/import")
    public ApiResponse<?> importFile(@RequestPart("file") MultipartFile file) {
        try {
            return ApiResponse.ok(adminService.importClassrooms(file.getOriginalFilename(), file.getBytes()));
        } catch (Exception e) {
            return ApiResponse.fail(400, "文件读取失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] data = adminService.exportClassrooms();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"classrooms.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{id}/qrcode")
    public ApiResponse<?> qrcode(@PathVariable long id) {
        return ApiResponse.ok(adminService.classroomQrcode(id));
    }

    // ==================== 照片管理（管理员上传直接生效，不走审核） ====================
    //
    // 【新手导读】一张管理员照片从浏览器到数据库的完整旅程：
    //   1. 管理后台点「照片」按钮 → 选图 → axios 发 multipart 表单（file=图片文件 + category + description）
    //      到本类的 uploadImage()（进 Controller 前，AuthInterceptor 已验证过 token 和角色）；
    //   2. uploadImage() 先调 UploadService 上传：限流检查 → 校验格式/大小 → EXIF 清洗 → 存进
    //      upload-dir/img/20261116/xxx.png，拿到相对路径 ossKey；
    //   3. 再调 BuildingClassroomService.addImageAdmin()：往 t_classroom_image 表插一行，
    //      audit_status 直接写 APPROVED（管理员自己就是发布者，不需要别人再审核）；
    //      对比：小程序用户上传走的 addImage() 是 PENDING，要等审核员在「内容审核」页通过才公开；
    //   4. 前端拿到 url（/api/files/ossKey），<img> 一挂就能显示 —— /api/files/** 在
    //      WebMvcConfig 里映射成了 upload-dir 目录的静态资源，可直接当图片访问。
    // =================================================================================

    /** GET /admin/classrooms/{id}/images —— 全部状态照片列表（管理端能看到待审核的，小程序端只看得到 APPROVED） */
    @GetMapping("/{id}/images")
    public ApiResponse<?> listImages(@PathVariable long id) {
        return ApiResponse.ok(buildingClassroomService.listImagesAdmin(id));
    }

    /**
     * POST /admin/classrooms/{id}/images —— 上传照片并直接发布
     * @param file        图片文件本体（multipart 里的 file 字段，jpg/png/webp，≤8MB，后端会再校验一次）
     * @param category    照片分类（OVERALL 整体 / SOCKET 插座 / AC 空调…，前端下拉框传）
     * @param description 描述可空，如"讲台侧面插座"
     */
    @PostMapping("/{id}/images")
    public ApiResponse<?> uploadImage(@PathVariable long id,
                                      @RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false, defaultValue = "OTHER") String category,
                                      @RequestParam(required = false) String description) {
        // 第一步：把图片存到磁盘，拿到 ossKey（如 img/20261116/48601e0a....png）
        var uploaded = uploadService.uploadImage(UserContext.adminId(), file);
        // 第二步：往 t_classroom_image 插一条记录，状态 APPROVED，小程序端立即可见
        Long imageId = buildingClassroomService.addImageAdmin(UserContext.adminId(), id,
                String.valueOf(uploaded.get("ossKey")), category, description);
        // 第三步：把 id 和访问地址还给前端，前端刷新照片墙
        return ApiResponse.ok(Map.of(
                "id", imageId,
                "ossKey", uploaded.get("ossKey"),
                "url", uploaded.get("url")));
    }

    /** DELETE /admin/classrooms/images/{imageId} —— 逻辑删除（数据库里 deleted 置 1，数据不真删） */
    @DeleteMapping("/images/{imageId}")
    public ApiResponse<?> deleteImage(@PathVariable long imageId) {
        buildingClassroomService.deleteImageAdmin(imageId);
        return ApiResponse.ok();
    }

    private static Classroom fromBody(Map<String, Object> body) {
        Classroom c = new Classroom();
        c.setBuildingId(body.get("buildingId") == null ? null
                : Long.valueOf(String.valueOf(body.get("buildingId"))));
        c.setRoomNo((String) body.get("roomNo"));
        c.setName((String) body.get("name"));
        c.setType((String) body.get("type"));
        c.setSeatCount(intOf(body.get("seatCount")));
        c.setHasAc(intOf(body.get("hasAc")));
        c.setSocketDesc((String) body.get("socketDesc"));
        c.setSocketCount(intOf(body.get("socketCount")));
        c.setHasProjector(intOf(body.get("hasProjector")));
        c.setHasPc(intOf(body.get("hasPc")));
        c.setHasMic(intOf(body.get("hasMic")));
        c.setHasWhiteboard(intOf(body.get("hasWhiteboard")));
        c.setHasWifi(intOf(body.get("hasWifi")));
        c.setOtherDevices((String) body.get("otherDevices"));
        c.setEnvScoreQuiet(intOf(body.get("envScoreQuiet")));
        c.setEnvDesc((String) body.get("envDesc"));
        c.setDeskDesc((String) body.get("deskDesc"));
        return c;
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
