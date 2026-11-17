package com.campus.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失物招领与社区相关实体（t_lost_item / t_item_image / t_claim / t_message
 * / t_favorite / t_report / t_audit_record / t_op_log）。
 */
public class CommunityDomain {

    @Data
    @TableName("t_lost_item")
    public static class LostItem {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        /** LOST/FOUND */
        private String kind;
        private String title;
        private String category;
        private String description;
        private LocalDateTime lostTime;
        private Long buildingId;
        private String locationDesc;
        private Integer contactVisible;
        private String contactValue;
        /** OPEN/MATCHED/DONE/CLOSED/EXPIRED */
        private String status;
        private LocalDateTime expireAt;
        private Integer viewCount;
        /** 补充字段：公开检索需 APPROVED，发布后先 PENDING */
        private String auditStatus;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_item_image")
    public static class ItemImage {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long lostItemId;
        private String ossKey;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_claim")
    public static class Claim {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long lostItemId;
        private Long claimantId;
        private String proofDesc;
        private String contact;
        /** PENDING/ACCEPTED/REJECTED/CANCELLED */
        private String status;
        private Long handlerId;
        private LocalDateTime handleTime;
        private String handleRemark;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_message")
    public static class Message {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        /** SYSTEM/LOST_CONTACT/CLAIM/MATCH/AUDIT/REMIND */
        private String type;
        private String title;
        private String content;
        private String relatedType;
        private Long relatedId;
        private Integer isRead;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_favorite")
    public static class Favorite {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        /** CLASSROOM/LOST_ITEM */
        private String targetType;
        private Long targetId;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_report")
    public static class Report {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long reporterId;
        /** CLASSROOM_IMAGE/LOST_ITEM/REVIEW/ROOM_STATUS/USER */
        private String targetType;
        private Long targetId;
        /** PORN/AD/PRIVACY/FALSE/OTHER */
        private String reasonType;
        private String reasonDesc;
        /** PENDING/HANDLED/REJECTED */
        private String status;
        private Long handlerId;
        private String handleResult;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_audit_record")
    public static class AuditRecord {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String targetType;
        private Long targetId;
        private Long auditorId;
        /** APPROVE/REJECT/OFFLINE/DELETE */
        private String action;
        private String remark;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_op_log")
    public static class OpLog {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String actorType;
        private Long actorId;
        private String module;
        private String action;
        private String detail;
        private String ip;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }
}
