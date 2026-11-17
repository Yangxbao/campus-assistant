const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

const DAY_LABELS = ['一', '二', '三', '四', '五', '六', '日'];
const WEEK_TYPE_OPTIONS = [
  { value: 'ALL', label: '每周' },
  { value: 'ODD', label: '单周' },
  { value: 'EVEN', label: '双周' }
];
const CELL_H = 120;

Page({
  data: {
    taskId: null,
    loading: true,
    error: false,
    courses: [],
    view: 'grid',        // grid 周视图 / list 列表
    maxPeriod: 10,
    weekNoOptions: [],
    columns: [],

    /* 编辑弹层 */
    showEdit: false,
    editIndex: -1,
    editIsNew: false,
    form: {
      courseName: '', teacher: '', roomRawText: '',
      weekday: 1, startPeriod: 1, endPeriod: 1,
      weekType: 'ALL', weeksText: ''
    },
    weekdayOptions: DAY_LABELS.map(d => '周' + d),
    weekTypeOptions: WEEK_TYPE_OPTIONS,
    weekTypeIndex: 0,
    saving: false
  },

  onLoad(options) {
    this.setData({ taskId: options.taskId });
    this.load();
  },

  load() {
    this.setData({ loading: true, error: false });
    api.timetable.getOcrTask(this.data.taskId).then((task) => {
      const courses = (task && (task.courses || task.result)) || [];
      if (!courses.length) {
        this.setData({ loading: false, courses: [], columns: [] });
        return;
      }
      this.decorate(courses);
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  decorate(courses) {
    let maxPeriod = 8;
    courses.forEach((c, i) => {
      c.index = i;
      c.lowConfidence = c.confidence != null && Number(c.confidence) < 0.5;
      c.confidenceText = c.confidence != null
        ? '置信度 ' + Math.round(Number(c.confidence) * 100) + '%'
        : '';
      const [bg, color] = util.courseColor(c.courseName);
      c.bg = bg;
      c.color = color;
      c.weekdayText = '周' + DAY_LABELS[c.weekday - 1];
      c.timeText = `第${c.startPeriod}-${c.endPeriod}节`;
      if (Number(c.endPeriod) > maxPeriod) maxPeriod = Number(c.endPeriod);
    });
    this.setData({ courses, maxPeriod });
    const weekNoOptions = [];
    for (let i = 1; i <= maxPeriod; i++) weekNoOptions.push(i);
    this.setData({ courses, maxPeriod, weekNoOptions });
    this.buildGrid();
  },

  /* 周视图（只读预览） */
  buildGrid() {
    const columns = [];
    for (let wd = 1; wd <= 7; wd++) {
      const cells = [];
      for (let p = 1; p <= this.data.maxPeriod; p++) {
        cells.push({ p, course: null, covered: false });
      }
      columns.push({ weekday: wd, label: DAY_LABELS[wd - 1], cells });
    }
    this.data.courses.forEach((c) => {
      const wd = Number(c.weekday);
      if (wd < 1 || wd > 7) return;
      const col = columns[wd - 1];
      const sp = Math.min(Math.max(Number(c.startPeriod) || 1, 1), this.data.maxPeriod);
      const ep = Math.min(Math.max(Number(c.endPeriod) || sp, sp), this.data.maxPeriod);
      if (col.cells[sp - 1].course) return;
      col.cells[sp - 1].course = Object.assign({}, c, {
        blockH: (ep - sp + 1) * CELL_H - 8
      });
      for (let p = sp + 1; p <= ep; p++) col.cells[p - 1].covered = true;
    });
    this.setData({ columns });
  },

  switchView(e) {
    this.setData({ view: e.currentTarget.dataset.view });
  },

  /* ---------- 编辑弹层 ---------- */
  openEdit(e) {
    const idx = Number(e.currentTarget.dataset.index);
    const c = this.data.courses[idx];
    this.setData({
      showEdit: true,
      editIndex: idx,
      editIsNew: false,
      weekTypeIndex: Math.max(0, WEEK_TYPE_OPTIONS.findIndex(o => o.value === (c.weekType || 'ALL'))),
      form: {
        courseName: c.courseName || '',
        teacher: c.teacher || '',
        roomRawText: c.roomRawText || '',
        weekday: Number(c.weekday) || 1,
        startPeriod: Number(c.startPeriod) || 1,
        endPeriod: Number(c.endPeriod) || 1,
        weekType: c.weekType || 'ALL',
        weeksText: c.weeksText || ''
      }
    });
  },

  openAdd() {
    this.setData({
      showEdit: true,
      editIndex: -1,
      editIsNew: true,
      weekTypeIndex: 0,
      form: {
        courseName: '', teacher: '', roomRawText: '',
        weekday: 1, startPeriod: 1, endPeriod: 1,
        weekType: 'ALL', weeksText: ''
      }
    });
  },

  closeEdit() { this.setData({ showEdit: false, saving: false }); },

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

  /** 解析周次文本："1-16周" / "1,3,5" / "1-8,11-16" → 数组；解析失败返回 []（视为每周） */
  parseWeeks(text) {
    const s = String(text || '');
    const out = new Set();
    const rangeRe = /(\d+)\s*[-~—]\s*(\d+)/g;
    let m;
    while ((m = rangeRe.exec(s)) !== null) {
      const a = Number(m[1]);
      const b = Number(m[2]);
      for (let i = Math.min(a, b); i <= Math.max(a, b); i++) out.add(i);
    }
    const cleaned = s.replace(rangeRe, ' ');
    const singles = cleaned.match(/\d+/g) || [];
    singles.forEach(n => out.add(Number(n)));
    return Array.from(out).sort((a, b) => a - b);
  },

  saveEdit() {
    const f = this.data.form;
    if (!String(f.courseName).trim()) {
      wx.showToast({ title: '请填写课程名称', icon: 'none' });
      return;
    }
    if (Number(f.endPeriod) < Number(f.startPeriod)) {
      wx.showToast({ title: '结束节次不能早于开始节次', icon: 'none' });
      return;
    }
    const item = {
      courseName: String(f.courseName).trim(),
      teacher: String(f.teacher).trim(),
      roomRawText: String(f.roomRawText).trim(),
      weekday: f.weekday,
      startPeriod: f.startPeriod,
      endPeriod: f.endPeriod,
      weekType: f.weekType,
      weeksText: String(f.weeksText || '').trim(),
      confidence: this.data.editIsNew ? 1 : (this.data.courses[this.data.editIndex] || {}).confidence
    };
    const courses = this.data.courses.slice();
    if (this.data.editIsNew) courses.push(item);
    else courses[this.data.editIndex] = item;
    this.setData({ showEdit: false, courses });
    this.decorate(courses);
  },

  removeCourse(e) {
    const idx = Number(e.currentTarget.dataset.index);
    const c = this.data.courses[idx];
    wx.showModal({
      title: '删除该条',
      content: `确定在校正结果中移除「${c.courseName}」吗？（仅影响本次保存）`,
      confirmColor: '#F25555',
      success: (res) => {
        if (!res.confirm) return;
        const courses = this.data.courses.slice();
        courses.splice(idx, 1);
        this.setData({ courses });
        this.decorate(courses);
      }
    });
  },

  /* ---------- 确认保存（绝不静默保存） ---------- */
  confirmAll() {
    if (!this.data.courses.length) {
      wx.showToast({ title: '没有可保存的课程', icon: 'none' });
      return;
    }
    const lowCount = this.data.courses.filter(c => c.lowConfidence).length;
    wx.showModal({
      title: '确认保存到课表',
      content: `共 ${this.data.courses.length} 条课程将写入我的课表` +
        (lowCount ? `，其中 ${lowCount} 条识别置信度较低，请确认已核对` : '') +
        '。保存后请到课程表页查看，确定继续吗？',
      success: (res) => {
        if (!res.confirm) return;
        this.doConfirm();
      }
    });
  },

  doConfirm() {
    if (this.data.saving) return;
    const payload = this.data.courses.map(c => ({
      courseName: c.courseName,
      teacher: c.teacher || '',
      weekday: Number(c.weekday),
      startPeriod: Number(c.startPeriod),
      endPeriod: Number(c.endPeriod),
      weekType: c.weekType || 'ALL',
      weeks: this.parseWeeks(c.weeksText),
      roomRawText: c.roomRawText || ''
    }));
    this.setData({ saving: true });
    api.timetable.confirmOcr(this.data.taskId, payload).then(() => {
      this.setData({ saving: false });
      wx.showToast({ title: '已保存到课表', icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/timetable/index' });
      }, 800);
    }).catch((err) => {
      this.setData({ saving: false });
      util.toastError(err, '保存失败，请重试');
    });
  },

  retry() { this.load(); },
  goManual() { wx.switchTab({ url: '/pages/timetable/index' }); },
  noop() {}
});
