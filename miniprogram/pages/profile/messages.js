const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

Page({
  data: {
    hasToken: false,
    list: [],
    page: 1,
    hasMore: false,
    loading: false,
    loadingMore: false,
    error: false
  },

  onShow() {
    const hasToken = !!req.getToken();
    this.setData({ hasToken });
    if (hasToken) {
      this.reload();
      this.markReadOnEnter();
    }
  },

  onPullDownRefresh() {
    if (this.data.hasToken) this.reload();
    wx.stopPullDownRefresh();
  },

  reload() {
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.messages.list(1).then((data) => {
      const info = util.pageData(data, 1, PAGE_SIZE);
      this.setData({
        list: this.decorate(info.list),
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
    api.messages.list(nextPage).then((data) => {
      const info = util.pageData(data, nextPage, PAGE_SIZE);
      this.setData({
        list: this.data.list.concat(this.decorate(info.list)),
        page: nextPage,
        hasMore: info.hasMore,
        loadingMore: false
      });
    }).catch(() => {
      this.setData({ loadingMore: false });
      wx.showToast({ title: '加载更多失败', icon: 'none' });
    });
  },

  decorate(list) {
    return (list || []).map(m => Object.assign({}, m, {
      icon: util.MSG_TYPE_ICON[m.type] || '📬',
      timeText: util.formatTime(m.createdAt)
    }));
  },

  /** 进入页面时静默拉取一次，用于刷新角标（不弹错） */
  markReadOnEnter() {},

  onTapMessage(e) {
    const m = this.data.list[Number(e.currentTarget.dataset.index)];
    if (!m) return;
    if (m.isRead === 0 || m.isRead === false) {
      api.messages.read(m.id).then(() => {
        const key = `list[${e.currentTarget.dataset.index}].isRead`;
        this.setData({ [key]: 1 });
      }).catch(() => {});
    }
    // 跳转关联内容
    if (m.relatedType === 'LOST_ITEM' && m.relatedId) {
      wx.navigateTo({ url: '/pages/lostfound/detail?id=' + m.relatedId });
    } else if (m.relatedType === 'CLASSROOM' && m.relatedId) {
      wx.navigateTo({ url: '/pages/classroom/detail?id=' + m.relatedId });
    }
  },

  readAll() {
    api.messages.readAll().then(() => {
      const list = this.data.list.map(m => Object.assign({}, m, { isRead: 1 }));
      this.setData({ list });
      wx.showToast({ title: '已全部标记为已读', icon: 'success' });
    }).catch((err) => util.toastError(err, '操作失败'));
  },

  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.reload(); }
});
