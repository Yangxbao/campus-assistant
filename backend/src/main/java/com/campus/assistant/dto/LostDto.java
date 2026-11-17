package com.campus.assistant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失物招领 DTO。
 */
public class LostDto {

    @Data
    public static class LostItemReq {
        /** LOST/FOUND */
        private String kind;
        private String title;
        private String category;
        private String description;
        private String lostTime;
        private Long buildingId;
        private String locationDesc;
        private List<String> ossKeys;
        private Integer contactVisible;
        private String contactValue;
    }

    @Data
    public static class LostItemVO {
        private Long id;
        private Long userId;
        private String nickname;
        private String kind;
        private String title;
        private String category;
        private String description;
        private LocalDateTime lostTime;
        private Long buildingId;
        private String buildingName;
        private String locationDesc;
        private Integer contactVisible;
        /** 联系方式：仅发布者/管理员可见；公开时前端提示脱敏 */
        private String contactValue;
        private String status;
        private List<String> images;
        private Integer viewCount;
        private String auditStatus;
        private LocalDateTime createdAt;
    }

    /** 自动匹配结果（纯规则实现，非 AI） */
    @Data
    public static class MatchVO {
        private Long id;
        private String kind;
        private String title;
        private String category;
        private String description;
        private LocalDateTime lostTime;
        private String locationDesc;
        private String buildingName;
        private List<String> images;
        private List<String> matchReasons;
        private Integer score;
    }

    @Data
    public static class ClaimReq {
        private String proofDesc;
        private String contact;
    }

    @Data
    public static class ClaimHandleReq {
        /** ACCEPTED / REJECTED */
        private String action;
        private String remark;
    }

    @Data
    public static class ClaimVO {
        private Long id;
        private Long lostItemId;
        private Long claimantId;
        private String claimantNickname;
        private String proofDesc;
        private String contact;
        private String status;
        private String handleRemark;
        private LocalDateTime createdAt;
    }
}
