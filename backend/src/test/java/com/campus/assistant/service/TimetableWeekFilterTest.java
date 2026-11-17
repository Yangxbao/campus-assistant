package com.campus.assistant.service;

import com.campus.assistant.entity.TimetableDomain.Course;
import com.campus.assistant.mapper.CourseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程周次过滤测试（契约 2.3）：ALL 每周；ODD 单周；EVEN 双周；weeks json 数组按周过滤，空视为全周。
 */
@SpringBootTest
@ActiveProfiles("test")
class TimetableWeekFilterTest {

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private CourseMapper courseMapper;

    private Course insert(long uid, String weekType, String weeksJson) {
        Course c = new Course();
        c.setUserId(uid);
        c.setSemesterId(1L);
        c.setCourseName("测试课程-" + weekType);
        c.setWeekday(1);
        c.setStartPeriod(1);
        c.setEndPeriod(2);
        c.setWeekType(weekType);
        c.setWeeks(weeksJson);
        courseMapper.insert(c);
        return c;
    }

    @Test
    void ALL每周都上() {
        Course c = insert(910001L, "ALL", null);
        assertTrue(timetableService.appliesToWeek(c, 1, 1L));
        assertTrue(timetableService.appliesToWeek(c, 8, 1L));
        assertTrue(timetableService.appliesToWeek(c, 16, 1L));
    }

    @Test
    void 单周课只在奇数周() {
        Course c = insert(910001L, "ODD", null);
        assertTrue(timetableService.appliesToWeek(c, 1, 1L));
        assertFalse(timetableService.appliesToWeek(c, 2, 1L));
        assertTrue(timetableService.appliesToWeek(c, 15, 1L));
    }

    @Test
    void 双周课只在偶数周() {
        Course c = insert(910001L, "EVEN", null);
        assertFalse(timetableService.appliesToWeek(c, 1, 1L));
        assertTrue(timetableService.appliesToWeek(c, 2, 1L));
        assertTrue(timetableService.appliesToWeek(c, 16, 1L));
    }

    @Test
    void 指定周次数组过滤() {
        Course c = insert(910001L, "ALL", "[1,3,5,15]");
        assertTrue(timetableService.appliesToWeek(c, 3, 1L));
        assertFalse(timetableService.appliesToWeek(c, 4, 1L));
        assertTrue(timetableService.appliesToWeek(c, 15, 1L));
        assertFalse(timetableService.appliesToWeek(c, 16, 1L));
    }

    @Test
    void 列表查询按周过滤() {
        long uid = 910002L;
        insert(uid, "ODD", null);   // 奇数周
        insert(uid, "ALL", "[2]");  // 仅第2周

        List<?> oddWeek = timetableService.listCourses(uid, 1L, 1);
        assertEquals(1, oddWeek.size(), "第1周应只有单周课");

        List<?> evenWeek = timetableService.listCourses(uid, 1L, 2);
        assertEquals(1, evenWeek.size(), "第2周应只有指定[2]的课");

        List<?> all = timetableService.listCourses(uid, 1L, null);
        assertEquals(2, all.size(), "不传周次应返回全部课程");
    }
}
