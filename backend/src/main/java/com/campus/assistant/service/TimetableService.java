package com.campus.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.assistant.common.BizException;
import com.campus.assistant.common.XssUtil;
import com.campus.assistant.dto.TimetableDto.CourseReq;
import com.campus.assistant.dto.TimetableDto.CourseVO;
import com.campus.assistant.dto.TimetableDto.OcrCourse;
import com.campus.assistant.dto.TimetableDto.PeriodVO;
import com.campus.assistant.dto.TimetableDto.SemesterVO;
import com.campus.assistant.dto.TimetableDto.TodayVO;
import com.campus.assistant.entity.TimetableDomain.Course;
import com.campus.assistant.entity.TimetableDomain.OcrTask;
import com.campus.assistant.entity.TimetableDomain.Period;
import com.campus.assistant.entity.TimetableDomain.Semester;
import com.campus.assistant.mapper.CourseMapper;
import com.campus.assistant.mapper.OcrTaskMapper;
import com.campus.assistant.mapper.PeriodMapper;
import com.campus.assistant.mapper.SemesterMapper;
import com.campus.assistant.ocr.OcrGateway;
import com.campus.assistant.ocr.TimetableParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 课程表服务：学期/节次/课程 CRUD、周次与单双周过滤、今日课程、OCR 链路（未配置返回 50301）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final SemesterMapper semesterMapper;
    private final PeriodMapper periodMapper;
    private final CourseMapper courseMapper;
    private final OcrTaskMapper ocrTaskMapper;
    private final OcrGateway ocrGateway;
    private final TimetableParser timetableParser;
    private final ObjectMapper objectMapper;
    private final com.campus.assistant.storage.StorageService storageService;
    private final com.campus.assistant.mapper.SysConfigMapper sysConfigMapper;

    // ---------- 学期 / 节次 ----------

    public List<SemesterVO> listSemesters() {
        List<SemesterVO> list = new ArrayList<>();
        for (Semester s : semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                .orderByDesc(Semester::getIsCurrent).orderByAsc(Semester::getStartDate))) {
            list.add(toSemesterVO(s));
        }
        return list;
    }

    public Long createSemester(String name, String startDate, Integer totalWeeks, Integer isCurrent) {
        Semester s = new Semester();
        s.setName(name);
        s.setStartDate(LocalDate.parse(startDate));
        s.setTotalWeeks(totalWeeks);
        s.setIsCurrent(isCurrent == null ? 0 : isCurrent);
        if (s.getIsCurrent() == 1) {
            semesterMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Semester>()
                    .eq(Semester::getIsCurrent, 1).set(Semester::getIsCurrent, 0));
        }
        semesterMapper.insert(s);
        return s.getId();
    }

    public List<PeriodVO> listPeriods(Long semesterId) {
        Long sid = semesterId == null ? currentSemesterId() : semesterId;
        List<PeriodVO> list = new ArrayList<>();
        for (Period p : periodMapper.selectList(new LambdaQueryWrapper<Period>()
                .eq(sid != null, Period::getSemesterId, sid)
                .orderByAsc(Period::getPeriodNo))) {
            PeriodVO vo = new PeriodVO();
            vo.setPeriodNo(p.getPeriodNo());
            vo.setStartTime(p.getStartTime());
            vo.setEndTime(p.getEndTime());
            list.add(vo);
        }
        return list;
    }

    // ---------- 课程 CRUD ----------

    public List<CourseVO> listCourses(long uid, Long semesterId, Integer week) {
        Long sid = semesterId == null ? currentSemesterId() : semesterId;
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getUserId, uid)
                .eq(sid != null, Course::getSemesterId, sid)
                .orderByAsc(Course::getWeekday).orderByAsc(Course::getStartPeriod));
        List<CourseVO> result = new ArrayList<>();
        for (Course c : courses) {
            if (week == null || appliesToWeek(c, week, sid)) {
                result.add(toCourseVO(c));
            }
        }
        return result;
    }

    public Long addCourse(long uid, CourseReq req) {
        validateCourseReq(req);
        Course c = new Course();
        c.setUserId(uid);
        c.setSemesterId(req.getSemesterId() == null ? currentSemesterId() : req.getSemesterId());
        applyReq(c, req);
        courseMapper.insert(c);
        return c.getId();
    }

    public void updateCourse(long uid, long id, CourseReq req) {
        Course c = courseMapper.selectById(id);
        if (c == null || c.getDeleted() == 1 || c.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "课程不存在或无权修改");
        }
        validateCourseReq(req);
        applyReq(c, req);
        courseMapper.updateById(c);
    }

    public void deleteCourse(long uid, long id) {
        Course c = courseMapper.selectById(id);
        if (c == null || c.getDeleted() == 1 || c.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "课程不存在或无权删除");
        }
        courseMapper.deleteById(id);
    }

    // ---------- 今日课程 ----------

    public TodayVO today(long uid, Long semesterId, Integer week) {
        Semester sem = semesterId == null ? currentSemester() : semesterMapper.selectById(semesterId);
        LocalDate today = LocalDate.now();
        int weekday = today.getDayOfWeek().getValue();
        Integer w = week;
        if (w == null && sem != null) {
            w = weekOf(sem, today);
        }
        List<CourseVO> todayCourses = new ArrayList<>();
        for (CourseVO c : listCourses(uid, semesterId, w)) {
            if (c.getWeekday() == weekday) {
                todayCourses.add(c);
            }
        }
        // 下一节课：按开始节次时间排序
        List<Period> periods = periodMapper.selectList(new LambdaQueryWrapper<Period>()
                .eq(sem != null, Period::getSemesterId, sem == null ? null : sem.getId())
                .orderByAsc(Period::getPeriodNo));
        TodayVO vo = new TodayVO();
        vo.setWeek(w);
        vo.setWeekday(weekday);
        vo.setTodayCourses(todayCourses);
        LocalTime now = LocalTime.now();
        CourseVO next = null;
        LocalTime nextStart = null;
        for (CourseVO c : todayCourses) {
            LocalTime st = periodTime(periods, c.getStartPeriod(), true);
            if (st != null && st.isAfter(now)) {
                if (next == null || st.isBefore(nextStart)) {
                    next = c;
                    nextStart = st;
                }
            }
        }
        if (next != null) {
            vo.setNextCourse(next);
            vo.setMinutesUntilNext(ChronoUnit.MINUTES.between(LocalDateTime.of(today, now),
                    LocalDateTime.of(today, nextStart)));
            vo.setNextCourseStartTime(nextStart.toString());
        }
        return vo;
    }

    // ---------- OCR 链路 ----------

    /** 创建 OCR 任务并识别；OCR 未配置时任务置 FAILED 并抛 50301，绝不伪造结果 */
    public Long createOcrTask(long uid, String ossKey) {
        if (ossKey == null || ossKey.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "ossKey 不能为空");
        }
        OcrTask task = new OcrTask();
        task.setUserId(uid);
        task.setOssKey(ossKey);
        task.setStatus("UPLOADED");
        ocrTaskMapper.insert(task);

        byte[] image;
        try {
            image = storageService.read(ossKey);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMsg("图片读取失败");
            ocrTaskMapper.updateById(task);
            throw BizException.of(BizException.BAD_REQUEST, "图片读取失败，请重新上传");
        }
        task.setStatus("PROCESSING");
        ocrTaskMapper.updateById(task);

        var result = ocrGateway.recognize(image);
        if (!result.isAvailable()) {
            task.setStatus("FAILED");
            task.setErrorMsg(result.getMessage());
            ocrTaskMapper.updateById(task);
            throw BizException.of(BizException.OCR_NOT_CONFIGURED,
                    result.getMessage() == null ? "OCR 服务未配置，请手动录入" : result.getMessage());
        }
        List<OcrCourse> courses = timetableParser.parse(result.getRawText());
        try {
            task.setResultJson(objectMapper.writeValueAsString(courses));
        } catch (Exception e) {
            log.warn("OCR 结果序列化失败", e);
        }
        task.setStatus("DONE");
        ocrTaskMapper.updateById(task);
        return task.getId();
    }

    public Map<String, Object> ocrResult(long uid, long taskId) {
        OcrTask task = ocrTaskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1 || task.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "任务不存在");
        }
        List<OcrCourse> courses = List.of();
        if (task.getResultJson() != null) {
            try {
                courses = objectMapper.readValue(task.getResultJson(), new TypeReference<>() {
                });
            } catch (Exception ignored) {
                // 结果损坏时按空列表返回，由用户手动录入
            }
        }
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("taskId", task.getId());
        m.put("status", task.getStatus());
        m.put("courses", courses);
        if (task.getErrorMsg() != null) {
            m.put("errorMsg", task.getErrorMsg());
        }
        return m;
    }

    /** 用户在校正页确认后正式保存课程 */
    public int confirmOcrCourses(long uid, long taskId, List<OcrCourse> courses) {
        OcrTask task = ocrTaskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1 || task.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "任务不存在");
        }
        int saved = 0;
        Long sid = currentSemesterId();
        for (OcrCourse c : courses) {
            if (c == null || c.getWeekday() == null || c.getStartPeriod() == null) {
                continue;
            }
            Course course = new Course();
            course.setUserId(uid);
            course.setSemesterId(sid);
            course.setCourseName(XssUtil.escape(c.getCourseName()));
            course.setTeacher(XssUtil.escape(c.getTeacher()));
            course.setWeekday(c.getWeekday());
            course.setStartPeriod(c.getStartPeriod());
            course.setEndPeriod(c.getEndPeriod() == null ? c.getStartPeriod() : c.getEndPeriod());
            course.setWeekType(c.getWeekType() == null ? "ALL" : c.getWeekType());
            List<Integer> ws = parseWeeksJson(c.getWeeksText());
            course.setWeeks(ws.isEmpty() ? null
                    : ws.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "[", "]")));
            course.setRoomRawText(XssUtil.escape(c.getRoomRawText()));
            courseMapper.insert(course);
            saved++;
        }
        return saved;
    }

    // ---------- 提醒配置 ----------

    public void saveRemindConfig(long uid, Integer enabled, Integer minutesBefore) {
        // 用户级提醒配置存 sys_config（key 带用户维度），微信模板未配置时推送标记 notConfigured，不假装已推送
        String key = "remind:" + uid;
        String value = (enabled == null ? 0 : enabled) + "," + (minutesBefore == null ? 15 : minutesBefore);
        com.campus.assistant.entity.UserDomain.SysConfig exist = sysConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.assistant.entity.UserDomain.SysConfig>()
                        .eq(com.campus.assistant.entity.UserDomain.SysConfig::getConfigKey, key).last("LIMIT 1"));
        if (exist == null) {
            com.campus.assistant.entity.UserDomain.SysConfig cfg = new com.campus.assistant.entity.UserDomain.SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            cfg.setRemark("课表提醒配置（微信模板未配置时仅记录，不真实推送）");
            sysConfigMapper.insert(cfg);
        } else {
            exist.setConfigValue(value);
            sysConfigMapper.updateById(exist);
        }
    }

    // ---------- 内部工具 ----------

    public Semester currentSemester() {
        Semester s = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1).last("LIMIT 1"));
        if (s == null) {
            s = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                    .orderByDesc(Semester::getId).last("LIMIT 1"));
        }
        return s;
    }

    public Long currentSemesterId() {
        Semester s = currentSemester();
        return s == null ? null : s.getId();
    }

    /** 当前教学周：由 semester.start_date 计算，越界返回 0 */
    public int weekOf(Semester sem, LocalDate date) {
        if (sem == null || sem.getStartDate() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(sem.getStartDate(), date);
        if (days < 0) {
            return 0;
        }
        int week = (int) (days / 7) + 1;
        if (sem.getTotalWeeks() != null && week > sem.getTotalWeeks()) {
            return 0;
        }
        return week;
    }

    /** 周次/单双周过滤：ALL 每周；ODD 单周；EVEN 双周；weeks 为 json 数组，空则视为全部周 */
    boolean appliesToWeek(Course c, int week, Long semesterId) {
        if ("ODD".equals(c.getWeekType()) && week % 2 == 0) {
            return false;
        }
        if ("EVEN".equals(c.getWeekType()) && week % 2 == 1) {
            return false;
        }
        List<Integer> weeks = parseWeeks(c.getWeeks());
        if (weeks == null || weeks.isEmpty()) {
            return true;
        }
        return weeks.contains(week);
    }

    private void validateCourseReq(CourseReq req) {
        if (req.getWeekday() == null || req.getWeekday() < 1 || req.getWeekday() > 7) {
            throw BizException.of(BizException.BAD_REQUEST, "weekday 取值 1-7");
        }
        if (req.getStartPeriod() == null || req.getStartPeriod() < 1
                || (req.getEndPeriod() != null && req.getEndPeriod() < req.getStartPeriod())) {
            throw BizException.of(BizException.BAD_REQUEST, "节次不合法");
        }
        if (req.getWeekType() != null && !List.of("ALL", "ODD", "EVEN").contains(req.getWeekType())) {
            throw BizException.of(BizException.BAD_REQUEST, "weekType 取值 ALL/ODD/EVEN");
        }
    }

    private void applyReq(Course c, CourseReq req) {
        c.setCourseName(XssUtil.escape(req.getCourseName()));
        c.setTeacher(XssUtil.escape(req.getTeacher()));
        c.setWeekday(req.getWeekday());
        c.setStartPeriod(req.getStartPeriod());
        c.setEndPeriod(req.getEndPeriod() == null ? req.getStartPeriod() : req.getEndPeriod());
        c.setWeekType(req.getWeekType() == null ? "ALL" : req.getWeekType());
        c.setWeeks(weeksToJson(req.getWeeks()));
        c.setBuildingId(req.getBuildingId());
        c.setClassroomId(req.getClassroomId());
        c.setRoomRawText(XssUtil.escape(req.getRoomRawText()));
        c.setRemark(XssUtil.escape(req.getRemark()));
    }

    private CourseVO toCourseVO(Course c) {
        CourseVO vo = new CourseVO();
        vo.setId(c.getId());
        vo.setSemesterId(c.getSemesterId());
        vo.setCourseName(c.getCourseName());
        vo.setTeacher(c.getTeacher());
        vo.setWeekday(c.getWeekday());
        vo.setStartPeriod(c.getStartPeriod());
        vo.setEndPeriod(c.getEndPeriod());
        vo.setWeekType(c.getWeekType());
        vo.setWeeks(parseWeeks(c.getWeeks()));
        vo.setBuildingId(c.getBuildingId());
        vo.setClassroomId(c.getClassroomId());
        vo.setRoomRawText(c.getRoomRawText());
        vo.setRemark(c.getRemark());
        return vo;
    }

    private SemesterVO toSemesterVO(Semester s) {
        SemesterVO vo = new SemesterVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setStartDate(s.getStartDate() == null ? null : s.getStartDate().toString());
        vo.setTotalWeeks(s.getTotalWeeks());
        vo.setIsCurrent(s.getIsCurrent());
        vo.setCurrentWeek(weekOf(s, LocalDate.now()));
        return vo;
    }

    private String weeksToJson(List<Integer> weeks) {
        if (weeks == null || weeks.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(weeks);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Integer> parseWeeks(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 周次文本（如 "1-16周" 或 "1,3,5"）解析为周列表，失败返回空列表 */
    private List<Integer> parseWeeksJson(String weeksText) {
        if (weeksText == null || weeksText.isBlank()) {
            return List.of();
        }
        List<Integer> weeks = new ArrayList<>();
        try {
            String t = weeksText.replace("周", "").replace("第", "").trim();
            for (String part : t.split("[,，、]")) {
                if (part.contains("-")) {
                    String[] se = part.split("-");
                    int a = Integer.parseInt(se[0].trim());
                    int b = Integer.parseInt(se[1].trim());
                    for (int i = a; i <= b; i++) {
                        weeks.add(i);
                    }
                } else if (!part.isBlank()) {
                    weeks.add(Integer.parseInt(part.trim()));
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return weeks;
    }

    private LocalTime periodTime(List<Period> periods, Integer periodNo, boolean start) {
        if (periodNo == null) {
            return null;
        }
        return periods.stream().filter(p -> p.getPeriodNo().equals(periodNo)).findFirst()
                .map(p -> start ? p.getStartTime() : p.getEndTime()).orElse(null);
    }
}
