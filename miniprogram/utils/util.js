/**
 * 通用工具函数
 */
const env = require('../config/env.js');

/** 格式化时间 yyyy-MM-dd HH:mm:ss（兼容 ISO / 时间戳） */
function formatTime(input, withTime = true) {
  if (!input) return '';
  const d = input instanceof Date ? input : new Date(String(input).replace(/-/g, '/'));
  if (isNaN(d.getTime())) return String(input);
  const pad = (n) => (n < 10 ? '0' + n : '' + n);
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  if (!withTime) return date;
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/** 距离有效期剩余描述，如 "2小时后过期" */
function expireDesc(expiresAt) {
  if (!expiresAt) return '';
  const t = new Date(String(expiresAt).replace(/-/g, '/')).getTime();
  const diff = t - Date.now();
  if (diff <= 0) return '已过期';
  const h = Math.floor(diff / 3600000);
  if (h >= 1) return `${h} 小时后过期`;
  const m = Math.ceil(diff / 60000);
  return `${m} 分钟后过期`;
}

/** 联系方式脱敏：手机号保留前3后4 */
function maskContact(v) {
  if (!v) return '';
  const s = String(v);
  if (/^1\d{10}$/.test(s)) return s.slice(0, 3) + '****' + s.slice(7);
  if (s.length > 6) return s.slice(0, 3) + '****' + s.slice(-3);
  return s;
}

/** 枚举文案映射 */
const OCCUPANCY_TEXT = { FREE: '空闲', PARTLY: '部分占用', FULL: '占用', UNKNOWN: '未知' };
const ROOM_TYPE_TEXT = { LINEAR: '大教室', SMALL: '小教室', LAB: '实验室', MULTIMEDIA: '多媒体', OTHER: '其他' };
const WEEK_TYPE_TEXT = { ALL: '每周', ODD: '单周', EVEN: '双周' };
const LOST_STATUS_TEXT = { OPEN: '进行中', MATCHED: '待确认', DONE: '已完成', CLOSED: '已撤销', EXPIRED: '已过期' };
const AUDIT_TEXT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过', OFFLINE: '已下架' };
const CLAIM_STATUS_TEXT = { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELLED: '已取消' };
const WALL_POS_TEXT = { FRONT: '前墙', BACK: '后墙', LEFT: '左墙', RIGHT: '右墙', NEAR_PODIUM: '讲台附近', WINDOW: '窗边' };
const CATEGORY_LIST = ['证件', '卡类', '电子产品', '书籍', '钥匙', '雨伞', '衣物', '首饰', '现金', '其他'];
const KIND_TEXT = { LOST: '我丢了东西', FOUND: '我捡到了东西' };
const MSG_TYPE_ICON = { SYSTEM: '📢', LOST_CONTACT: '☎️', CLAIM: '🙋', MATCH: '🔎', AUDIT: '📋', REMIND: '⏰' };

/** 课程块颜色（按课程名稳定哈希取色，色系统一柔和） */
const COURSE_COLORS = [
  ['#EAF1FF', '#3D7EFF'],
  ['#FFF3E6', '#FF9F43'],
  ['#E8F8EF', '#2FA968'],
  ['#F3EDFF', '#8C6FE8'],
  ['#FDECEC', '#F25555'],
  ['#E6F7F8', '#2BB3B3']
];
function courseColor(name) {
  let h = 0;
  const s = String(name || '');
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return COURSE_COLORS[h % COURSE_COLORS.length];
}

/** 判断某课程在某周是否上课 */
function courseOnWeek(course, week) {
  if (!week) return true;
  const wt = course.weekType || 'ALL';
  if (wt === 'ODD' && week % 2 === 0) return false;
  if (wt === 'EVEN' && week % 2 === 1) return false;
  let weeks = course.weeks;
  if (typeof weeks === 'string') {
    try { weeks = JSON.parse(weeks); } catch (e) { weeks = null; }
  }
  if (Array.isArray(weeks) && weeks.length) return weeks.indexOf(week) !== -1;
  return true; // 未配置周数视为每周
}

/** 节次时间显示：periods 数组中 startPeriod~endPeriod 的开始/结束时间 */
function periodTime(periods, startNo, endNo) {
  const s = periods.find(p => p.periodNo === startNo);
  const e = periods.find(p => p.periodNo === endNo);
  return `${s ? s.startTime : ''} ~ ${e ? e.endTime : ''}`;
}

/** 订阅课程提醒：未配置模板 ID 时明确提示，绝不假装订阅成功 */
function subscribeCourseRemind() {
  return new Promise((resolve) => {
    const ids = env.SUBSCRIBE_TEMPLATE_IDS || [];
    if (!ids.length) {
      wx.showToast({ title: '订阅消息未配置，请联系管理员在 config/env.js 填写模板 ID', icon: 'none', duration: 2500 });
      return resolve({ configured: false });
    }
    wx.requestSubscribeMessage({
      tmplIds: ids,
      success(res) {
        const accepted = ids.filter(id => res[id] === 'accept');
        if (accepted.length) {
          // 记录用户同意订阅的模板 ID
          apiTimetableSaveSubscribe(accepted).finally(() => resolve({ configured: true, accepted }));
        } else {
          wx.showToast({ title: '您拒绝了订阅提醒', icon: 'none' });
          resolve({ configured: true, accepted: [] });
        }
      },
      fail(err) {
        wx.showToast({ title: '订阅失败：' + (err.errMsg || '未知原因'), icon: 'none' });
        resolve({ configured: true, accepted: [] });
      }
    });
  });
}

function apiTimetableSaveSubscribe(tmplIds) {
  const api = require('./api.js');
  return api.timetable.saveSubscribeRecord({ templateIds: tmplIds });
}

/** 后端图片地址规整：优先 url 字段，否则用 uploads 目录 + ossKey */
function imgUrl(item) {
  if (!item) return '';
  const u = typeof item === 'string' ? item : (item.url || '');
  if (u && /^https?:/.test(u)) return u;
  const key = typeof item === 'string' ? '' : item.ossKey;
  if (!key) return u || '';
  if (/^https?:/.test(key)) return key;
  return 'http://localhost:8080/uploads/' + key.replace(/^\//, '');
}

/** 通用错误提示（页面 catch 中使用） */
function toastError(err, fallback) {
  const msg = (err && (err.errMsg || err.message)) || fallback || '操作失败';
  wx.showToast({ title: msg, icon: 'none' });
}

/** 安全返回分页对象 */
function pageData(data, page, size) {
  const list = (data && data.list) || [];
  const total = (data && data.total) || 0;
  return { list, total, hasMore: page * size < total };
}

module.exports = {
  formatTime, expireDesc, maskContact,
  OCCUPANCY_TEXT, ROOM_TYPE_TEXT, WEEK_TYPE_TEXT, LOST_STATUS_TEXT,
  AUDIT_TEXT, CLAIM_STATUS_TEXT, WALL_POS_TEXT,
  CATEGORY_LIST, KIND_TEXT, MSG_TYPE_ICON,
  courseColor, courseOnWeek, periodTime,
  subscribeCourseRemind, toastError, pageData, imgUrl
};
