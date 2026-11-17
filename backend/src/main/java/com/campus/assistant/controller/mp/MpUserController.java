package com.campus.assistant.controller.mp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.common.ApiResponse;
import com.campus.assistant.common.PageResult;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.AdminDto.UserAdminVO;
import com.campus.assistant.dto.AuthDto.UpdateMeReq;
import com.campus.assistant.entity.ClassroomDomain.ClassroomImage;
import com.campus.assistant.entity.ClassroomDomain.EquipmentReport;
import com.campus.assistant.entity.UserDomain.User;
import com.campus.assistant.mapper.ClassroomImageMapper;
import com.campus.assistant.mapper.EquipmentReportMapper;
import com.campus.assistant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序用户接口（契约 2.1）：个人信息、贡献记录、注销（逻辑删除）。
 */
@RestController
@RequestMapping("/api/mp/user")
@RequiredArgsConstructor
public class MpUserController {

    private final UserMapper userMapper;
    private final ClassroomImageMapper classroomImageMapper;
    private final EquipmentReportMapper equipmentReportMapper;

    @GetMapping("/me")
    public ApiResponse<?> me() {
        User u = userMapper.selectById(UserContext.uid());
        if (u == null || u.getDeleted() == 1) {
            return ApiResponse.ok(null);
        }
        UserAdminVO vo = new UserAdminVO();
        vo.setId(u.getId());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setOpenid(u.getOpenid());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setContributeCount(u.getContributeCount());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setLastLoginTime(u.getLastLoginTime());
        return ApiResponse.ok(vo);
    }

    @PutMapping("/me")
    public ApiResponse<?> updateMe(@RequestBody UpdateMeReq req) {
        User u = userMapper.selectById(UserContext.uid());
        if (u == null || u.getDeleted() == 1) {
            return ApiResponse.fail(404, "用户不存在");
        }
        if (req.getNickname() != null) {
            u.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            u.setAvatarUrl(req.getAvatarUrl());
        }
        userMapper.updateById(u);
        return ApiResponse.ok();
    }

    /** 我的贡献记录：教室照片 + 资料修正（分页） */
    @GetMapping("/me/contributions")
    public ApiResponse<?> contributions(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        long uid = UserContext.uid();
        Page<ClassroomImage> images = classroomImageMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(size, 50)),
                new LambdaQueryWrapper<ClassroomImage>()
                        .eq(ClassroomImage::getUserId, uid)
                        .orderByDesc(ClassroomImage::getCreatedAt));
        List<EquipmentReport> reports = equipmentReportMapper.selectList(
                new LambdaQueryWrapper<EquipmentReport>()
                        .eq(EquipmentReport::getUserId, uid)
                        .orderByDesc(EquipmentReport::getCreatedAt)
                        .last("LIMIT " + Math.min(size, 50)));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassroomImage img : images.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "CLASSROOM_IMAGE");
            m.put("id", img.getId());
            m.put("ossKey", img.getOssKey());
            m.put("category", img.getCategory());
            m.put("auditStatus", img.getAuditStatus());
            m.put("createdAt", img.getCreatedAt());
            list.add(m);
        }
        for (EquipmentReport r : reports) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "EQUIPMENT_REPORT");
            m.put("id", r.getId());
            m.put("fieldName", r.getFieldName());
            m.put("newValue", r.getNewValue());
            m.put("auditStatus", r.getAuditStatus());
            m.put("createdAt", r.getCreatedAt());
            list.add(m);
        }
        return ApiResponse.ok(PageResult.of(list, images.getTotal(), page, size));
    }

    /** 注销：逻辑删除 */
    @DeleteMapping("/me")
    public ApiResponse<?> deleteMe() {
        userMapper.deleteById(UserContext.uid());
        return ApiResponse.ok();
    }
}
