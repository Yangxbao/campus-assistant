const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const CELL_H = 120; // 每节课格高 rpx
const DAY_LABELS = ['一', '二', '三', '四', '五', '六', '日'];
const WEEK_TYPE_OPTIONS = [
  { value: 'ALL', label: '每周' },
  { value: 'ODD', label: '单周' },
  { value: 'EVEN', label: '双周' }
];

function todayWeekday() {
  return ((new Date().getDay() + 6) % 7) + 1; // 周一=1 … 周日=7
}

Page({
  data: {
    hasToken: false,
    loading: true,
    error: false,

    semesterId: null,
    semesterName: '',
    totalWeeks: 20,
    currentWeek: null,   // 服务端当前教学周
    week: 1,
    periods: [],         // [{periodNo, startTime, endTime}]
    maxPeriod: 8,
    columns: [],         // 7 列网格
    todayWd: 1,

    todayCourses: [],
    nextCourse: null,
    nextText: '',

    weekOptions: [],
    weekIndex: 0,

    /* 课程详情弹层 */
    showDetail: false,
    detailCourse: null,

    /* 手动添加/编辑表单 */
    showForm: false,
    editingId: null,
    form: {
      courseName: '', teacher: '',
      weekday: 1, startPeriod: 1, endPeriod: 1,
      weekType: 'ALL', weeks: [],
      roomRawText: '', remark: ''
    },
    weekdayOptions: DAY_LABELS.map(d => '周' + d),
    periodOptions: [],
    weekTypeOptions: WEEK_TYPE_OPTIONS,
    weekTypeIndex: 0,
    weekChips: [],
    saving: false
  },

  onShow() {
    const hasToken = !!req.getToken();
    this.setData({ hasToken, todayWd: todayWeekday() });
    if (!hasToken) {
      this.setData({ loading: false, error: false });
      return;
    }
    if (!this.data.semesterId || this.data.needReload) {
      this.loadAll();
    }
  },

  onPullDownRefresh() {
    if (this.data.hasToken) this.loadAll();
    wx.stopPullDownRefresh();
  },

  loadAll() {
    this.setData({ loading: true, error: false });
    api.timetable.semesters().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      const sem = list.find(s => s.isCurrent) || list[0] || null;
      if (!sem) {
        // 无学期配置：使用默认参数仍可手动录入
        this.setData({ semesterId: null, semesterName: '未配置学期', loading: false });
        return this.loadPeriodsAndCourses();
      }
      this.setData({
        semesterId: sem.id,
        semesterName: sem.name || '本学期',
        totalWeeks: sem.totalWeeks || 20
      });
      return this.loadPeriodsAndCourses();
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  loadPeriodsAndCourses() {
    const p1 = this.data.semesterId
      ? api.timetable.periods(this.data.semesterId)
      : Promise.resolve([]);
    return Promise.all([p1, api.timetable.weeksCurrent().catch(() => null)])
      .then(([periods, cur]) => {
        const plist = (Array.isArray(periods) ? periods : (periods && periods.list) || [])
          .slice().sort((a, b) => a.periodNo - b.periodNo);
        const finalPeriods = plist.length ? plist : this.defaultPeriods();
        const currentWeek = typeof cur === 'number' ? cur : (cur && cur.week) || null;
        const week = currentWeek || 1;
        this.setData({
          periods: finalPeriods,
          maxPeriod: finalPeriods.length,
          currentWeek,
          week,
          periodOptions: finalPeriods.map(p => `第${p.periodNo}节 ${p.startTime || ''}`),
          weekOptions: this.buildWeekOptions(this.data.totalWeeks)
        });
        return this.loadCourses();
      })
      .catch(() => {
        this.setData({ loading: false, error: true });
      });
  },

  defaultPeriods() {
    const arr = [];
    for (let i = 1; i <= 8; i++) {
      arr.push({ periodNo: i, startTime: '', endTime: '' });
    }
    return arr;
  },

  buildWeekOptions(totalWeeks) {
    const arr = [];
    for (let i = 1; i <= totalWeeks; i++) arr.push('第' + i + '周');
    return arr;
  },

  loadCourses() {
    const q = {};
    if (this.data.semesterId) q.semesterId = this.data.semesterId;
    q.week = this.data.week;
    return api.timetable.courses(q).then((data) => {
      const courses = Array.isArray(data) ? data : (data && data.list) || [];
      this.setData({ courses, loading: false, needReload: false });
      this.rebuild();
      this.loadToday();
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  /* ---------- 网格构建 ---------- */
  rebuild() {
    const week = this.data.week;
    const isThisWeek = this.data.currentWeek != null && week === this.data.currentWeek;
    const todayWd = todayWeekday();
    const maxPeriod = this.data.maxPeriod;

    const columns = [];
    for (let wd = 1; wd <= 7; wd++) {
      const cells = [];
      for (let p = 1; p <= maxPeriod; p++) cells.push({ p, course: null, covered: false });
      columns.push({
        weekday: wd,
        label: DAY_LABELS[wd - 1],
        isToday: isThisWeek && wd === todayWd,
        cells
      });
    }

    (this.data.courses || []).forEach((c) => {
      const wd = Number(c.weekday);
      if (wd < 1 || wd > 7) return;
      if (!util.courseOnWeek(c, week)) return;
      const col = columns[wd - 1];
      const sp = Math.min(Math.max(Number(c.startPeriod) || 1, 1), maxPeriod);
      const ep = Math.min(Math.max(Number(c.endPeriod) || sp, sp), maxPeriod);
      if (col.cells[sp - 1].course) return; // 同格冲突时保留先到者
      const [bg, color] = util.courseColor(c.courseName);
      col.cells[sp - 1].course = Object.assign({}, c, {
        span: ep - sp + 1,
        blockH: (ep - sp + 1) * CELL_H - 8,
        bg,
        color
      });
      for (let p = sp + 1; p <= ep; p++) col.cells[p - 1].covered = true;
    });

    this.setData({ columns, isThisWeek });
  },

  /* ---------- 今日课程 / 下一节 ---------- */
  loadToday() {
    const q = {};
    if (this.data.semesterId) q.semesterId = this.data.semesterId;
    q.week = this.data.week;
    api.timetable.today(q).then((data) => {
      const list = (data && (data.courses || data.list)) || [];
      const decorated = this.decorateToday(list);
      let next = data && data.next ? data.next : null;
      let nextText = '';
      if (next) {
        nextText = next.remainingText || next.remaining || '';
      } else {
        const calc = this.calcNext(decorated);
        next = calc.next;
        nextText = calc.text;
      }
      if (next) {
        next.bg = util.courseColor(next.courseName)[0];
        next.color = util.courseColor(next.courseName)[1];
      }
      this.setData({ todayCourses: decorated, nextCourse: next, nextText });
    }).catch(() => {
      // 接口异常时本地兜底计算
      const list = (this.data.courses || []).filter(c => Number(c.weekday) === todayWeekday());
      const decorated = this.decorateToday(list);
      const calc = this.calcNext(decorated);
      if (calc.next) {
        calc.next.bg = util.courseColor(calc.next.courseName)[0];
        calc.next.color = util.courseColor(calc.next.courseName)[1];
      }
      this.setData({ todayCourses: decorated, nextCourse: calc.next, nextText: calc.text });
    });
  },

  decorateToday(list) {
    return (list || []).map(c => {
      const [bg, color] = util.courseColor(c.courseName);
      return Object.assign({}, c, {
        bg, color,
        timeText: util.periodTime(this.data.periods, c.startPeriod, c.endPeriod)
      });
    }).sort((a, b) => a.startPeriod - b.startPeriod);
  },

  calcNext(list) {
    const now = new Date();
    const nowMin = now.getHours() * 60 + now.getMinutes();
    for (const c of list) {
      const s = this.data.periods.find(p => p.periodNo === c.startPeriod);
      if (!s || !s.startTime) continue;
      const [h, m] = s.startTime.split(':').map(Number);
      if (h * 60 + m >= nowMin) {
        const diff = h * 60 + m - nowMin;
        return { next: c, text: diff >= 60 ? `${Math.floor(diff / 60)}小时${diff % 60}分后上课` : `${diff}分钟后上课` };
      }
    }
    return { next: null, text: '' };
  },

  /* ---------- 周次切换 ---------- */
  switchWeek(week) {
    const w = Math.min(Math.max(week, 1), this.data.totalWeeks);
    this.setData({ week: w, weekIndex: w - 1 });
    this.rebuild();
    this.loadToday();
  },
  prevWeek() { this.switchWeek(this.data.week - 1); },
  nextWeek() { this.switchWeek(this.data.week + 1); },
  onWeekPick(e) { this.switchWeek(Number(e.detail.value) + 1); },
  goCurrentWeek() {
    if (this.data.currentWeek) this.switchWeek(this.data.currentWeek);
  },
  // 快速跳到最近的单周/双周
  jumpType(e) {
    const type = e.currentTarget.dataset.type; // 'ODD' | 'EVEN'
    const target = type === 'ODD' ? 1 : 2;
    const w = this.data.week % 2 === (target % 2) ? this.data.week : this.data.week + 1;
    this.switchWeek(Math.min(w, this.data.totalWeeks));
  },

  /* ---------- 课程详情弹层 ---------- */
  onCellCourse(e) {
    const { weekday, period } = e.currentTarget.dataset;
    const col = this.data.columns[weekday - 1];
    const course = col && col.cells[period - 1] && col.cells[period - 1].course;
    if (!course) return;
    const weeks = typeof course.weeks === 'string'
      ? (() => { try { return JSON.parse(course.weeks); } catch (err) { return []; } })()
      : (course.weeks || []);
    const wt = util.WEEK_TYPE_TEXT[course.weekType] || '每周';
    const detail = Object.assign({}, course, {
      weekdayText: '周' + DAY_LABELS[course.weekday - 1],
      timeText: util.periodTime(this.data.periods, course.startPeriod, course.endPeriod),
      weekText: wt + (weeks.length ? '（' + weeks.join(',') + '周）' : ''),
      roomText: course.roomRawText || course.classroomName || (course.classroomId ? '已关联教室' : '未填写教室'),
      weeksArr: weeks
    });
    this.setData({ showDetail: true, detailCourse: detail });
  },
  closeDetail() { this.setData({ showDetail: false, detailCourse: null }); },
  stopBubble() {},

  goClassroom() {
    const id = this.data.detailCourse && this.data.detailCourse.classroomId;
    if (!id) return;
    this.closeDetail();
    wx.navigateTo({ url: '/pages/classroom/detail?id=' + id });
  },

  /* ---------- 手动添加 / 编辑 ---------- */
  openAdd() {
    this.setData({
      showForm: true,
      editingId: null,
      weekTypeIndex: 0,
      form: {
        courseName: '', teacher: '',
        weekday: this.data.isThisWeek ? todayWeekday() : 1,
        startPeriod: 1, endPeriod: 1,
        weekType: 'ALL', weeks: [],
        roomRawText: '', remark: ''
      },
      weekChips: this.buildWeekChips([])
    });
  },

  openEdit() {
    const c = this.data.detailCourse;
    if (!c) return;
    this.closeDetail();
    const weeks = (c.weeksArr || []).slice();
    this.setData({
      showForm: true,
      editingId: c.id,
      form: {
        courseName: c.courseName || '',
        teacher: c.teacher || '',
        weekday: Number(c.weekday) || 1,
        startPeriod: Number(c.startPeriod) || 1,
        endPeriod: Number(c.endPeriod) || 1,
        weekType: c.weekType || 'ALL',
        weeks,
        roomRawText: c.roomRawText || '',
        remark: c.remark || ''
      },
      weekTypeIndex: Math.max(0, WEEK_TYPE_OPTIONS.findIndex(o => o.value === (c.weekType || 'ALL'))),
      weekChips: this.buildWeekChips(weeks)
    });
  },

  buildWeekChips(weeks) {
    const chips = [];
    for (let i = 1; i <= this.data.totalWeeks; i++) {
      chips.push({ no: i, on: weeks.indexOf(i) !== -1 });
    }
    return chips;
  },

  closeForm() { this.setData({ showForm: false, saving: false }); },

  onFormInput(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({ ['form.' + key]: e.detail.value });
  },
  onFormWeekday(e) { this.setData({ 'form.weekday': Number(e.detail.value) + 1 }); },
  onFormStart(e) {
    const v = Number(e.detail.value) + 1;
    this.setData({ 'form.startPeriod': v, 'form.endPeriod': Math.max(v, this.data.form.endPeriod) });
  },
  onFormEnd(e) { this.setData({ 'form.endPeriod': Number(e.detail.value) + 1 }); },
  onFormWeekType(e) {
    const i = Number(e.detail.value);
    this.setData({ weekTypeIndex: i, 'form.weekType': WEEK_TYPE_OPTIONS[i].value });
  },
  onWeekChip(e) {
    const no = Number(e.currentTarget.dataset.no);
    const weeks = this.data.form.weeks.slice();
    const idx = weeks.indexOf(no);
    if (idx === -1) weeks.push(no); else weeks.splice(idx, 1);
    weeks.sort((a, b) => a - b);
    this.setData({
      'form.weeks': weeks,
      weekChips: this.buildWeekChips(weeks)
    });
  },
  selectAllWeeks() {
    const weeks = [];
    for (let i = 1; i <= this.data.totalWeeks; i++) weeks.push(i);
    this.setData({ 'form.weeks': weeks, weekChips: this.buildWeekChips(weeks) });
  },

  submitForm() {
    if (this.data.saving) return;
    const f = this.data.form;
    if (!f.courseName.trim()) {
      wx.showToast({ title: '请填写课程名称', icon: 'none' });
      return;
    }
    if (f.endPeriod < f.startPeriod) {
      wx.showToast({ title: '结束节次不能早于开始节次', icon: 'none' });
      return;
    }
    const payload = {
      semesterId: this.data.semesterId,
      courseName: f.courseName.trim(),
      teacher: f.teacher.trim(),
      weekday: f.weekday,
      startPeriod: f.startPeriod,
      endPeriod: f.endPeriod,
      weekType: f.weekType,
      weeks: f.weeks,
      roomRawText: f.roomRawText.trim(),
      remark: f.remark.trim()
    };
    this.setData({ saving: true });
    const task = this.data.editingId
      ? api.timetable.updateCourse(this.data.editingId, payload)
      : api.timetable.addCourse(payload);
    task.then(() => {
      this.setData({ showForm: false, saving: false });
      wx.showToast({ title: this.data.editingId ? '已保存修改' : '已添加课程', icon: 'success' });
      this.loadCourses();
    }).catch((err) => {
      this.setData({ saving: false });
      util.toastError(err, '保存失败，请重试');
    });
  },

  deleteCourse() {
    const c = this.data.detailCourse;
    if (!c || !c.id) return;
    wx.showModal({
      title: '删除课程',
      content: `确定删除「${c.courseName}」吗？`,
      confirmColor: '#F25555',
      success: (res) => {
        if (!res.confirm) return;
        api.timetable.deleteCourse(c.id).then(() => {
          this.closeDetail();
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadCourses();
        }).catch((err) => util.toastError(err, '删除失败'));
      }
    });
  },

  goOcr() { wx.navigateTo({ url: '/pages/timetable/ocr' }); },

  onRemind() { util.subscribeCourseRemind(); },

  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.loadAll(); },
  noop() {}
});
