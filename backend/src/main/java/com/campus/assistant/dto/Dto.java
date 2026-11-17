package com.campus.assistant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索/展示相关 DTO。
 */
public class Dto {

    /** 教室搜索条件（对应 GET /mp/classrooms/search） */
    @Data
    public static class SearchQuery {
        private String keyword;
        private Long buildingId;
        private Integer floorNo;
        private String roomNo;
        private String type;
        private Integer hasAc;
        /** hasSocket: 最少插座数量 */
        private Integer hasSocket;
        private Integer hasProjector;
        private Integer hasPc;
        private Integer hasMic;
        private Integer hasWhiteboard;
        private Integer hasWifi;
        private Integer minQuiet;
        /** 适合自习：有插座 + 安静度>=4（派生条件） */
        private Integer suitableForSelfStudy;
        /** updated_desc / review_desc */
        private String sort;
        private Integer page = 1;
        private Integer size = 10;
    }

    /** 教室搜索结果行 */
    @Data
    public static class ClassroomVO {
        private Long id;
        private Long buildingId;
        private String buildingCode;
        private String buildingName;
        private Integer floorNo;
        private String roomNo;
        private String name;
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
        private Integer envScoreQuiet;
        private String envDesc;
        private String status;
        private LocalDateTime updatedContentAt;
        private LocalDateTime createdAt;
        private Long reviewCount;
        private Double reviewAvg;
    }

    /** 楼宇列表行 */
    @Data
    public static class BuildingVO {
        private Long id;
        private String code;
        private String name;
        private Integer floorsCount;
        private String description;
        private String campus;
        private String locationDesc;
        private Integer classroomCount;
    }

    /** 楼宇详情（含按楼层分组教室） */
    @Data
    public static class BuildingDetailVO {
        private Long id;
        private String code;
        private String name;
        private Integer floorsCount;
        private String description;
        private String campus;
        private String locationDesc;
        private List<FloorGroup> floors;
    }

    @Data
    public static class FloorGroup {
        private Long floorId;
        private Integer floorNo;
        private String description;
        private List<ClassroomVO> classrooms;
    }

    /** 教室详情 */
    @Data
    public static class ClassroomDetailVO {
        private ClassroomVO base;
        private List<String> images;
        private List<SocketMarkVO> socketMarks;
        private List<ReviewVO> reviews;
        private RoomStatusVO latestStatus;
        private LocalDateTime updatedContentAt;
    }

    @Data
    public static class SocketMarkVO {
        private Long id;
        private Long imageId;
        private Double posX;
        private Double posY;
        private String wallPos;
        private Integer accuracyUp;
        private Integer accuracyDown;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ReviewVO {
        private Long id;
        private Long userId;
        private String nickname;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RoomStatusVO {
        private String occupancy;
        private Integer quietLevel;
        private Integer acWorking;
        private Integer socketWorking;
        private String occupiedReason;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
    }
}
