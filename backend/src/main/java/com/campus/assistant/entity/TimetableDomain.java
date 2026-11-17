package com.campus.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 课程表相关实体（t_semester / t_period / t_course / t_ocr_task）。
 */
public class TimetableDomain {

    @Data
    @TableName("t_semester")
    public static class Semester {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String name;
        /** 第一周周一 */
        private LocalDate startDate;
        private Integer totalWeeks;
        private Integer isCurrent;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_period")
    public static class Period {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long semesterId;
        private Integer periodNo;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_course")
    public static class Course {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private Long semesterId;
        private String courseName;
        private String teacher;
        /** 1-7，周一到周日 */
        private Integer weekday;
        private Integer startPeriod;
        private Integer endPeriod;
        /** ALL/ODD/EVEN */
        private String weekType;
        /** json 数组如 [1,2,3,15,16] */
        private String weeks;
        private Long buildingId;
        private Long classroomId;
        private String roomRawText;
        private String remark;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_ocr_task")
    public static class OcrTask {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private String ossKey;
        /** UPLOADED/PROCESSING/DONE/FAILED */
        private String status;
        private String resultJson;
        private String errorMsg;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }
}
