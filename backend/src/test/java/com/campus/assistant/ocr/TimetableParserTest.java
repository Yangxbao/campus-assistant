package com.campus.assistant.ocr;

import com.campus.assistant.dto.TimetableDto.OcrCourse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课表解析器单元测试（契约第 5 节）：解析成功行 confidence=1，失败行 confidence=0 并保留原始文本。
 */
class TimetableParserTest {

    private final TimetableParser parser = new TimetableParser();

    @Test
    void 标准行解析成功() {
        List<OcrCourse> list = parser.parse("高等数学 一 1-2节 1-16周 教一楼 王老师");
        assertEquals(1, list.size());
        OcrCourse c = list.get(0);
        assertEquals(1.0, c.getConfidence());
        assertEquals("高等数学", c.getCourseName());
        assertEquals(1, c.getWeekday());
        assertEquals(1, c.getStartPeriod());
        assertEquals(2, c.getEndPeriod());
        assertEquals("1-16", c.getWeeksText());
        assertEquals("ALL", c.getWeekType());
        assertEquals("王老师", c.getTeacher());
    }

    @Test
    void 单双周解析() {
        List<OcrCourse> list = parser.parse(
                "大学英语 三 3-4节 单周 文A楼 李老师\n" +
                "数据结构 五 5-6节 双周 实验楼 张老师");
        assertEquals(2, list.size());
        assertEquals("ODD", list.get(0).getWeekType());
        assertEquals(3, list.get(0).getWeekday());
        assertEquals("EVEN", list.get(1).getWeekType());
    }

    @Test
    void 解析失败行置信度为0并保留原文() {
        List<OcrCourse> list = parser.parse("这是一段无法识别的乱码文本");
        assertEquals(1, list.size());
        OcrCourse c = list.get(0);
        assertEquals(0.0, c.getConfidence());
        assertEquals("这是一段无法识别的乱码文本", c.getCourseName());
        assertNull(c.getWeekday());
    }

    @Test
    void 混合多行部分成功() {
        List<OcrCourse> list = parser.parse(
                "大学物理 二 3-4节 1-8周 物理楼 刘老师\n" +
                "（乱码行");
        assertEquals(2, list.size());
        assertEquals(1.0, list.get(0).getConfidence());
        assertEquals(0.0, list.get(1).getConfidence());
    }

    @Test
    void 空文本返回空列表() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("  \n ").isEmpty());
    }

    @Test
    void 单行解析失败返回null() {
        assertNull(parser.parseLine("完全没有可匹配内容!!!"));
        assertNotNull(parser.parseLine("线性代数 四 1-2节 3-5周 教三楼 陈老师"));
    }
}
