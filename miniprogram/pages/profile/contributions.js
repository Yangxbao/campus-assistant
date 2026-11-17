const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

// 贡献类型文案（按后端返回的 type / recordType 归类展示）
const TYPE_TEXT = {
  EQUIPMENT_REPORT: '资料修正',
  CLASSROOM_INFO: '教室资料',
  IMAGE: '教室照片',
  REVIEW: '教室评价',
  ROOM_STATUS: '状态上报',
  SOCKET_MARK: '插座标记',
  REPORT: '举报',
  PHOTO: '教室照片',
  FEEDBACK: '反馈'
};

Page({
  data: {
    hasToken: false,
    typeFilter: '',     // photo / feedback / 空=全部
    typeTabs: [
      { value: '', label: '全部' },
      { value: 'photo', label: '照片' },
      { value: 'feedback', label: '反馈' }
    ],
    list: [],
    page: 1,
    hasMore: false,
    loading: false,
    loadingMore: false,
    error: false
  },

  onLoad(options) {
    if (options.type) this.setData({ typeFilter: options.type });
  },

  onShow() {
    const hasToken = !!req.getToken();
    this.setData({ hasToken });
    if (hasToken) this.reload();
  },

  onPullDownRefresh() {
    if (this.data.hasToken) this.reload();
    wx.stopPullDownRefresh();
  },

  onType(e) {
    this.setData({ typeFilter: e.currentTarget.dataset.value });
    this.reload();
  },

  decorate(list) {
    return (list || []).map(item => {
      const o = Object.assign({}, item);
      const t = o.type || o.recordType || '';
      o.typeText = TYPE_TEXT[t] || t || '贡献';
      o.statusText = util.AUDIT_TEXT[o.auditStatus] || o.auditStatus || '';
      o.timeText = util.formatTime(o.createdAt || o.updatedAt);
      o.targetText = o.targetDesc || o.classroomName ||
        (o.classroom ? (o.classroom.buildingName || '') + ' ' + (o.classroom.roomNo || '') : '') ||
        o.title || o.content || '';
      return o;
    });
  },

  filterByType(list) {
    const f = this.data.typeFilter;
    if (!f) return list;
    if (f === 'photo') return list.filter(i => i.type === 'IMAGE' || i.type === 'PHOTO' || i.type === 'SOCKET_MARK');
    if (f === 'feedback') return list.filter(i => ['REVIEW', 'REPORT', 'EQUIPMENT_REPORT', 'ROOM_STATUS'].indexOf(i.type) !== -1);
    return list;
  },

  reload() {
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.user.getContributions({ page: 1, size: PAGE_SIZE }).then((data) => {
      const info = util.pageData(data, 1, PAGE_SIZE);
      this.setData({
        list: this.decorate(this.filterByType(info.list)),
        hasMore: info.hasMore,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loadingMore || this.data.loading) return;
    const nextPage = this.data.page + 1;
    this.setData({ loadingMore: true });
    api.user.getContributions({ page: nextPage, size: PAGE_SIZE }).then((data) => {
      const info = util.pageData(data, nextPage, PAGE_SIZE);
      this.setData({
        list: this.data.list.concat(this.decorate(this.filterByType(info.list))),
        page: nextPage,
        hasMore: info.hasMore,
        loadingMore: false
      });
    }).catch(() => {
      this.setData({ loadingMore: false });
      wx.showToast({ title: '加载更多失败', icon: 'none' });
    });
  },

  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.reload(); }
});
