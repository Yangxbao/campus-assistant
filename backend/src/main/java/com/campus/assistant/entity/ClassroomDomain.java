package com.campus.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼宇与教室相关实体（t_building / t_floor / t_classroom / t_classroom_equipment_report
 * / t_classroom_image / t_socket_mark / t_classroom_review / t_room_status / t_building_qr）。
 */
public class ClassroomDomain {

    @Data
    @TableName("t_building")
    public static class Building {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String code;
        private String name;
        private Integer floorsCount;
        private String description;
        private String campus;
        private String locationDesc;
        private Long coverImageId;
        private String status;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_floor")
    public static class Floor {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long buildingId;
        private Integer floorNo;
        private String description;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_classroom")
    public static class Classroom {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long buildingId;
        private Long floorId;
        private String roomNo;
        private String name;
        /** LINEAR/SMALL/LAB/MULTIMEDIA/OTHER */
        private String type;
        private Integer seatCount;
        private Integer hasAc;
        private String socketDesc;
        private Integer socketCount;
        private Integer hasProjector;
        private Integer hasPc;
        private Integer hasMic;
        private Integer hasWhiteboard;
        private Integer hasWifi;
        private String otherDevices;
        private Integer envScoreQuiet;
        private String envDesc;
        private String deskDesc;
        private String lightDesc;
        private String ventilationDesc;
        private String cleanDesc;
        private String status;
        private Long latestContributorId;
        private LocalDateTime updatedContentAt;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_classroom_equipment_report")
    public static class EquipmentReport {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long classroomId;
        private Long userId;
        private String fieldName;
        private String newValue;
        private String remark;
        private String auditStatus;
        private Long auditorId;
        private String auditRemark;
        private LocalDateTime auditTime;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_classroom_image")
    public static class ClassroomImage {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long classroomId;
        private Long userId;
        private String ossKey;
        /** OVERALL/FRONT/BACK/SOCKET/AC/PROJECTOR/PC/WHITEBOARD/OTHER */
        private String category;
        private String description;
        private String auditStatus;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_socket_mark")
    public static class SocketMark {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long classroomId;
        private Long userId;
        private Long imageId;
        private BigDecimal posX;
        private BigDecimal posY;
        /** FRONT/BACK/LEFT/RIGHT/NEAR_PODIUM/WINDOW */
        private String wallPos;
        private Integer accuracyUp;
        private Integer accuracyDown;
        private String auditStatus;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_classroom_review")
    public static class Review {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long classroomId;
        private Long userId;
        private Integer rating;
        private String content;
        private String auditStatus;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_room_status")
    public static class RoomStatus {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long classroomId;
        private Long userId;
        /** FREE/PARTLY/FULL/UNKNOWN */
        private String occupancy;
        private Integer quietLevel;
        private Integer acWorking;
        private Integer socketWorking;
        private String occupiedReason;
        private LocalDateTime expiresAt;
        private String auditStatus;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_building_qr")
    public static class BuildingQr {
        @TableId(type = IdType.AUTO)
        private Long id;
        /** BUILDING/CLASSROOM */
        private String targetType;
        private Long targetId;
        private String qrToken;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }
}
