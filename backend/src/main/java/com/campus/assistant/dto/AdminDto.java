package com.campus.assistant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 DTO。
 */
public class AdminDto {

    @Data
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private List<RowError> failRows;
    }

    @Data
    public static class RowError {
        private int row;
        private String reason;
    }

    @Data
    public static class AuditHandleReq {
        /** CLASSROOM_IMAGE/EQUIPMENT_REPORT/LOST_ITEM/ROOM_STATUS/REVIEW */
        private String targetType;
        private Long targetId;
        /** APPROVE/REJECT/OFFLINE/DELETE */
        private String action;
        private String remark;
    }

    /** 统一审核列表项（跨表合并） */
    @Data
    public static class AuditItemVO {
        private String targetType;
        private Long targetId;
        private Long submitterId;
        private String summary;
        private String auditStatus;
        private LocalDateTime createdAt;
    }

    @Data
    public static class StatsOverview {
        private long userCount;
        private long buildingCount;
        private long classroomCount;
        private long imageCount;
        private long lostItemCount;
        private long recoveredCount;
    }

    @Data
    public static class DailyStat {
        private String date;
        private long classroomAdded;
        private long lostPublished;
        private long auditCount;
    }

    @Data
    public static class ContributorRow {
        private Long userId;
        private String nickname;
        private long photoCount;
        private long reportCount;
        private long reviewCount;
        private long total;
    }

    @Data
    public static class UserAdminVO {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private String openid;
        private String role;
        private String status;
        private Integer contributeCount;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginTime;
    }

    @Data
    public static class QrResp {
        /** PNG base64（data: 前缀，可直接用于 <img> src） */
        private String imageBase64;
        private String qrUrl;
        private String token;
    }
}
