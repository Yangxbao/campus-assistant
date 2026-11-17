const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const ANNOUNCE_READ_KEY = 'announce_read_ids';

Page({
  data: {
    keyword: '',
    announcements: [],
    showAnnounce: null,
    recentClassrooms: [],
    latestClassrooms: [],
    lostItems: [],
    loadingRecent: true,
    loadingLatest: true,
    loadingLost: true,
    failRecent: false,
    failLatest: false,
    failLost: false,
    unreadCount: 0,
    hasToken: false
  },

  onLoad() {
    this.loadAnnouncements();
  },

  onShow() {
    // 登录后的个性化数据
    const hasToken = !!req.getToken();
    this.setData({ hasToken });
    if (hasToken) {
      this.loadRecent();
      this.loadLatest();
      this.loadLost();
      this.loadUnread();
    } else {
      this.setData({
        loadingRecent: false, loadingLatest: false, loadingLost: false,
        failRecent: false, failLatest: false, failLost: false,
        recentClassrooms: [], latestClassrooms: [], lostItems: []
      });
    }
  },

  onPullDownRefresh() {
    this.loadAnnouncements();
    if (req.getToken()) {
      this.loadRecent();
      this.loadLatest();
      this.loadLost();
    }
    wx.stopPullDownRefresh();
  },

  /* ---------- 公告 ---------- */
  loadAnnouncements() {
    api.announcements.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      const readIds = wx.getStorageSync(ANNOUNCE_READ_KEY) || [];
      const firstUnread = list.find(a => readIds.indexOf(a.id) === -1);
      this.setData({
        announcements: list,
        showAnnounce: firstUnread || null
      });
    }).catch(() => {
      // 公告加载失败不打扰首页主流程
      this.setData({ announcements: [] });
    });
  },

  onShowAnnounce(e) {
    const item = e.currentTarget.dataset.item;
    if (item) this.setData({ showAnnounce: item });
  },

  closeAnnounce() {
    const a = this.data.showAnnounce;
    if (a && a.id) {
      const readIds = wx.getStorageSync(ANNOUNCE_READ_KEY) || [];
      if (readIds.indexOf(a.id) === -1) {
        readIds.push(a.id);
        wx.setStorageSync(ANNOUNCE_READ_KEY, readIds);
      }
    }
    this.setData({ showAnnounce: null });
  },

  /* ---------- 最近使用教室 ---------- */
  loadRecent() {
    this.setData({ loadingRecent: true, failRecent: false });
    api.classrooms.recent().then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []).slice(0, 6);
      this.setData({ recentClassrooms: list, loadingRecent: false });
    }).catch(() => {
      this.setData({ loadingRecent: false, failRecent: true, recentClassrooms: [] });
    });
  },

  /* ---------- 最新更新教室 ---------- */
  loadLatest() {
    this.setData({ loadingLatest: true, failLatest: false });
    api.classrooms.latestUpdated().then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []).slice(0, 6);
      list.forEach(c => {
        c.updatedText = util.formatTime(c.updatedContentAt || c.updatedAt, false);
      });
      this.setData({ latestClassrooms: list, loadingLatest: false });
    }).catch(() => {
      this.setData({ loadingLatest: false, failLatest: true, latestClassrooms: [] });
    });
  },

  /* ---------- 最新失物 ---------- */
  loadLost() {
    this.setData({ loadingLost: true, failLost: false });
    api.lostItems.list({ sort: 'time_desc', size: 5, page: 1 }).then((data) => {
      const list = (data && data.list) || [];
      list.forEach(i => {
        i.timeText = util.formatTime(i.lostTime || i.createdAt, false);
        i.kindText = util.KIND_TEXT[i.kind] || i.kind;
        i.statusText = util.LOST_STATUS_TEXT[i.status] || i.status;
        i.cover = (i.images && i.images[0]) || i.coverUrl || '';
        if (i.cover && !/^https?:/.test(i.cover)) i.cover = this.absUrl(i.cover);
      });
      this.setData({ lostItems: list, loadingLost: false });
    }).catch(() => {
      this.setData({ loadingLost: false, failLost: true, lostItems: [] });
    });
  },

  absUrl(u) {
    if (!u) return '';
    if (/^https?:/.test(u)) return u;
    return 'http://localhost:8080' + (u.charAt(0) === '/' ? u : '/' + u);
  },

  loadUnread() {
    api.messages.unreadCount().then((data) => {
      const count = typeof data === 'number' ? data : (data && data.count) || 0;
      this.setData({ unreadCount: count });
    }).catch(() => {});
  },

  /* ---------- 跳转 ---------- */
  onSearchInput(e) { this.setData({ keyword: e.detail.value }); },

  goSearch() {
    const kw = this.data.keyword || '';
    wx.navigateTo({
      url: '/pages/classroom/search' + (kw ? '?keyword=' + encodeURIComponent(kw) : '')
    });
  },

  goClassroomSearch() { wx.switchTab({ url: '/pages/classroom/search' }); },
  goTimetable() { wx.switchTab({ url: '/pages/timetable/index' }); },
  goLostfound() { wx.switchTab({ url: '/pages/lostfound/list' }); },
  goBuildings() { wx.navigateTo({ url: '/pages/building/list' }); },
  goFavorites() { wx.navigateTo({ url: '/pages/profile/favorites' }); },
  goContribute() {
    // 信息贡献说明入口：引导到贡献记录 + 贡献玩法
    wx.showActionSheet({
      itemList: ['查看我的贡献', '了解如何贡献教室资料'],
      success: (res) => {
        if (res.tapIndex === 0) {
          wx.navigateTo({ url: '/pages/profile/contributions' });
        } else {
          wx.showModal({
            title: '如何贡献教室资料',
            content: '进入任意教室详情页，即可：上传教室照片、修正设备信息、标记插座位置、上报临时状态。提交后经审核通过就会展示给全校同学。',
            showCancel: false
          });
        }
      }
    });
  },
  goMessages() { wx.navigateTo({ url: '/pages/profile/messages' }); },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },

  goClassroom(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/classroom/detail?id=' + id });
  },
  goLostDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/lostfound/detail?id=' + id });
  },
  retryRecent() { this.loadRecent(); },
  retryLatest() { this.loadLatest(); },
  retryLost() { this.loadLost(); },

  noop() {}
});
