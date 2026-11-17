/**
 * 接口层：与 docs/api-contract.md 第 2 节 /api/mp/** 一一对应
 * 所有路径以 /mp 开头（Base URL 已含 /api 前缀）
 */
const { get, post, put, del, request } = require('./request.js');

/* ============ 2.1 认证与用户 ============ */
const auth = {
  // POST /mp/auth/login  body { code } → { token, userInfo, devMode }
  login(code) {
    return post('/mp/auth/login', { code }, { loading: false });
  }
};

const user = {
  // GET /mp/user/me
  getMe(extra) { return get('/mp/user/me', null, extra); },
  // PUT /mp/user/me（昵称、头像、隐私设置）
  updateMe(data) { return put('/mp/user/me', data); },
  // GET /mp/user/me/contributions
  getContributions(params) { return get('/mp/user/me/contributions', params); },
  // DELETE /mp/user/me（注销）
  deleteMe() { return del('/mp/user/me'); }
};

/* ============ 2.2 楼宇与教室 ============ */
const buildings = {
  list() { return get('/mp/buildings'); },
  detail(id) { return get(`/mp/buildings/${id}`); },
  // 文字导航：从楼入口 → 楼层 → 教室
  guide(id) { return get(`/mp/buildings/${id}/guide`); }
};

const classrooms = {
  // 多条件组合筛选
  search(params) { return get('/mp/classrooms/search', params); },
  detail(id) { return get(`/mp/classrooms/${id}`); },
  images(id) { return get(`/mp/classrooms/${id}/images`); },
  addImage(id, data) { return post(`/mp/classrooms/${id}/images`, data); },
  socketMarks(id) { return get(`/mp/classrooms/${id}/socket-marks`); },
  addSocketMark(id, data) { return post(`/mp/classrooms/${id}/socket-marks`, data); },
  // { accurate: true/false }
  socketMarkFeedback(markId, accurate) {
    return post(`/mp/socket-marks/${markId}/feedback`, { accurate });
  },
  addEquipmentReport(id, data) { return post(`/mp/classrooms/${id}/equipment-reports`, data); },
  addReview(id, data) { return post(`/mp/classrooms/${id}/reviews`, data); },
  reviews(id, params) { return get(`/mp/classrooms/${id}/reviews`, params); },
  addStatus(id, data) { return post(`/mp/classrooms/${id}/status`, data); },
  latestStatus(id) { return get(`/mp/classrooms/${id}/status/latest`); },
  recent() { return get('/mp/classrooms/recent'); },
  latestUpdated() { return get('/mp/classrooms/latest-updated'); },
  // 教室未收录时申请新增
  applyNew(data) { return post('/mp/classrooms/apply-new', data); },
  qrResolve(token) { return get(`/mp/qr/${token}`); }
};

/* ============ 2.3 课程表 ============ */
const timetable = {
  semesters() { return get('/mp/timetable/semesters'); },
  periods(semesterId) { return get('/mp/timetable/periods', { semesterId }); },
  courses(params) { return get('/mp/timetable/courses', params); },
  addCourse(data) { return post('/mp/timetable/courses', data); },
  updateCourse(id, data) { return put(`/mp/timetable/courses/${id}`, data); },
  deleteCourse(id) { return del(`/mp/timetable/courses/${id}`); },
  today(params) { return get('/mp/timetable/today', params); },
  weeksCurrent() { return get('/mp/timetable/weeks-current'); },
  // 接收已上传图片的 ossKey，创建 OCR 任务；OCR 未配置返回 code=50301
  createOcrTask(ossKey) { return post('/mp/timetable/ocr', { ossKey }, { silent: true }); },
  getOcrTask(taskId) { return get(`/mp/timetable/ocr/${taskId}`); },
  // 用户在校正页核对修改后才正式保存
  confirmOcr(taskId, courses) {
    return post(`/mp/timetable/ocr/${taskId}/confirm`, { courses });
  },
  // 2.7 提醒
  saveRemindConfig(data) { return post('/mp/timetable/remind-config', data); },
  saveSubscribeRecord(data) { return post('/mp/timetable/remind/subscribe-record', data, { silent: true }); }
};

/* ============ 2.4 失物招领 ============ */
const lostItems = {
  create(data) { return post('/mp/lost-items', data); },
  list(params) { return get('/mp/lost-items', params); },
  detail(id) { return get(`/mp/lost-items/${id}`); },
  update(id, data) { return put(`/mp/lost-items/${id}`, data); },
  remove(id) { return del(`/mp/lost-items/${id}`); },
  updateStatus(id, status) { return put(`/mp/lost-items/${id}/status`, { status }); },
  createClaim(id, data) { return post(`/mp/lost-items/${id}/claims`, data); },
  claims(id) { return get(`/mp/lost-items/${id}/claims`); },
  // 发布者接受/拒绝认领
  handleClaim(claimId, data) { return put(`/mp/claims/${claimId}/handle`, data); },
  // 疑似匹配（含匹配理由）
  matches(id) { return get(`/mp/lost-items/${id}/matches`); },
  matchFeeds() { return get('/mp/lost-items/match-feeds'); }
};

/* ============ 2.5 收藏、消息、举报、公告 ============ */
const favorites = {
  // { targetType: CLASSROOM|LOST_ITEM, targetId }
  add(targetType, targetId) { return post('/mp/favorites', { targetType, targetId }); },
  remove(targetType, targetId) { return del('/mp/favorites', { targetType, targetId }); },
  list(type) { return get('/mp/favorites', type ? { type } : null); }
};

const messages = {
  list(page) { return get('/mp/messages', { page: page || 1 }); },
  read(id) { return put(`/mp/messages/${id}/read`); },
  readAll() { return put('/mp/messages/read-all'); },
  unreadCount() { return get('/mp/messages/unread-count', null, { silent: true, loading: false }); }
};

const reports = {
  // { targetType, targetId, reasonType, reasonDesc }
  create(data) { return post('/mp/reports', data); }
};

const announcements = {
  list() { return get('/mp/announcements', null, { silent: true, loading: false }); }
};

/* ============ 2.6 上传 ============ */
/**
 * 小程序端用 base64 上传图片：POST /mp/upload/image-base64
 * 后端校验格式(jpg/png/webp)/大小(≤8MB)，返回 { ossKey, url }
 */
function uploadImageBase64(filePath) {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath,
      encoding: 'base64',
      success(res) {
        const fileName = (filePath.split('/').pop() || 'image').split('?')[0];
        post('/mp/upload/image-base64', {
          fileName,
          base64: res.data
        }).then(resolve).catch(reject);
      },
      fail(err) {
        wx.showToast({ title: '读取图片失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = {
  auth, user, buildings, classrooms, timetable,
  lostItems, favorites, messages, reports, announcements,
  uploadImageBase64,
  request
};
