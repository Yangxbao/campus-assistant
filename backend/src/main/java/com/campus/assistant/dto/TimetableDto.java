package com.campus.assistant.dto;

import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 课程表 DTO。
 */
public class TimetableDto {

    @Data
    public static class CourseReq {
        private Long semesterId;
        private String courseName;
        private String teacher;
        private Integer weekday;
        private Integer startPeriod;
        private Integer endPeriod;
        /** ALL/ODD/EVEN */
        private String weekType;
        private List<Integer> weeks;
        private Long buildingId;
        private Long classroomId;
        private String roomRawText;
        private String remark;
    }

    @Data
    public static class CourseVO {
        private Long id;
        private Long semesterId;
        private String courseName;
        private String teacher;
        private Integer weekday;
        private Integer startPeriod;
        private Integer endPeriod;
        private String weekType;
        private List<Integer> weeks;
        private Long buildingId;
        private Long classroomId;
        private String roomRawText;
        private String remark;
    }

    @Data
    public static class SemesterVO {
        private Long id;
        private String name;
        private String startDate;
        private Integer totalWeeks;
        private Integer isCurrent;
        /** 当前教学周（由 start_date 计算，越界返回 0） */
        private Integer currentWeek;
    }

    @Data
    public static class PeriodVO {
        private Integer periodNo;
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Data
    public static class TodayVO {
        private Integer week;
        private Integer weekday;
        private List<CourseVO> todayCourses;
        private CourseVO nextCourse;
        private Long minutesUntilNext;
        private String nextCourseStartTime;
    }

    /** OCR 识别出的单条课程（结构化结果，confidence 为置信度 0-1） */
    @Data
    public static class OcrCourse {
        private String courseName;
        private Integer weekday;
        private Integer startPeriod;
        private Integer endPeriod;
        private String weekType;
        private String weeksText;
        private String roomRawText;
        private String teacher;
        private Double confidence;
    }

    @Data
    public static class OcrConfirmReq {
        private List<OcrCourse> courses;
    }

    @Data
    public static class RemindConfigReq {
        private Integer enabled;
        private Integer minutesBefore;
    }
}
