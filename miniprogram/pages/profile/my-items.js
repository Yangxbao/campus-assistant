const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

Page({
  data: {
    hasToken: false,
    statusTabs: [
      { value: '', label: '全部' },
      { value: 'OPEN', label: '进行中' },
      { value: 'MATCHED', label: '待确认' },
      { value: 'DONE', label: '已完成' },
      { value: 'CLOSED', label: '已撤销' }
    ],
    statusIndex: 0,
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
    if (hasToken) this.reload();
  },

  onPullDownRefresh() {
    if (this.data.hasToken) this.reload();
    wx.stopPullDownRefresh();
  },

  onStatus(e) {
    this.setData({ statusIndex: Number(e.currentTarget.dataset.index) });
    this.reload();
  },

  buildQuery(page) {
    const d = this.data;
    const q = { page, size: PAGE_SIZE, sort: 'time_desc', mine: true };
    if (d.statusTabs[d.statusIndex].value) q.status = d.statusTabs[d.statusIndex].value;
    return q;
  },

  decorate(list) {
    return (list || []).map(i => Object.assign({}, i, {
      kindText: util.KIND_TEXT[i.kind] || i.kind,
      statusText: util.LOST_STATUS_TEXT[i.status] || i.status,
      auditText: util.AUDIT_TEXT[i.auditStatus] || '',
      timeText: util.formatTime(i.lostTime || i.createdAt, false),
      cover: (i.images && i.images[0]) || i.coverUrl || ''
    }));
  },

  reload() {
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.lostItems.list(this.buildQuery(1)).then((data) => {
      const info = util.pageData(data, 1, PAGE_SIZE);
      // 兜底：后端不支持 mine 参数时按发布人过滤
      const me = getApp().globalData.userInfo || {};
      const list = info.list.filter(i => i.isMine === true || (i.userId && i.userId === me.id) || !me.id);
      this.setData({
        list: this.decorate(list),
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
    api.lostItems.list(this.buildQuery(nextPage)).then((data) => {
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

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/lostfound/detail?id=' + id });
  },

  goPublish() { wx.navigateTo({ url: '/pages/lostfound/publish' }); },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.reload(); }
});
