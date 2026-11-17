package com.campus.assistant.ocr;

import com.campus.assistant.dto.TimetableDto.OcrCourse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 课表解析器：将 OCR 结构化识别结果（文本行）解析为课程数组。
 * 解析策略：逐行匹配“课程名 星期 节次 周次 单双周 教室 教师”模式；
 * 解析失败的行 confidence=0 并保留原始文本，由用户在校正页修正。
 */
@Component
public class TimetableParser {

    /** 常见课表行模式：课程名 + 星期 + 节次 + （周次/单双周）+ 教室 + 教师 */
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(.+?)[\\s,，]+"                          // 课程名
            + "(?:周?([一二三四五六日天]))[\\s,，]*"    // 星期
            + "(?:第?(\\d+)-(\\d+)节?)[\\s,，]*"       // 节次
            + "(?:([\\d\\-,，、]+)周?)?"               // 周次
            + "(?:\\s*(单周|双周))?"                   // 单双周
            + "[\\s,，]*(?:([\\w\\u4e00-\\u9fa5（）()\\-]+(?:楼|教室|实验室|机房|室)))?"  // 教室
            + "[\\s,，]*(\\S+)?$"                     // 教师
    );

    public List<OcrCourse> parse(String rawText) {
        List<OcrCourse> result = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return result;
        }
        for (String line : rawText.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            OcrCourse c = parseLine(t);
            if (c == null) {
                // 解析失败：confidence=0，保留原始文本供用户修正
                OcrCourse failed = new OcrCourse();
                failed.setCourseName(t);
                failed.setConfidence(0.0);
                result.add(failed);
            } else {
                result.add(c);
            }
        }
        return result;
    }

    /** 解析单行，失败返回 null */
    OcrCourse parseLine(String line) {
        Matcher m = LINE_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        try {
            OcrCourse c = new OcrCourse();
            c.setCourseName(m.group(1).trim());
            c.setWeekday(weekdayNum(m.group(2)));
            c.setStartPeriod(Integer.parseInt(m.group(3)));
            c.setEndPeriod(Integer.parseInt(m.group(4)));
            String weeksText = m.group(5);
            c.setWeeksText(weeksText);
            c.setWeekType(parseWeekType(weeksText, m.group(6)));
            c.setRoomRawText(m.group(7));
            c.setTeacher(m.group(8));
            c.setConfidence(1.0);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private int weekdayNum(String zh) {
        return switch (zh) {
            case "一" -> 1;
            case "二" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            default -> 7;
        };
    }

    /** 周次文本解析：单双周优先；含逗号/顿号/连字符则视为 ALL 由用户确认，置信度降低 */
    private String parseWeekType(String weeksText, String zhType) {
        if (zhType != null) {
            return zhType.contains("单") ? "ODD" : "EVEN";
        }
        return "ALL";
    }
}
