const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

Page({
  data: {
    hasToken: false,
    kindTabs: [
      { value: '', label: '全部' },
      { value: 'LOST', label: '失物' },
      { value: 'FOUND', label: '招领' }
    ],
    kindIndex: 0,
    categories: [{ value: '', label: '全部类别' }].concat(
      util.CATEGORY_LIST.map(c => ({ value: c, label: c }))
    ),
    catIndex: 0,
    buildings: [],
    buildingIndex: -1,
    keyword: '',
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
      if (!this.data.buildings.length) this.loadBuildings();
      this.reload();
    }
  },

  onPullDownRefresh() {
    if (this.data.hasToken) this.reload();
    wx.stopPullDownRefresh();
  },

  loadBuildings() {
    api.buildings.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      this.setData({ buildings: list });
    }).catch(() => {});
  },

  buildQuery(page) {
    const d = this.data;
    const q = { page, size: PAGE_SIZE, sort: 'time_desc' };
    if (d.kindTabs[d.kindIndex].value) q.kind = d.kindTabs[d.kindIndex].value;
    if (d.catIndex > 0) q.category = d.categories[d.catIndex].value;
    if (d.buildingIndex >= 0) q.buildingId = d.buildings[d.buildingIndex].id;
    if (d.keyword) q.keyword = d.keyword;
    return q;
  },

  decorate(list) {
    return (list || []).map(i => Object.assign({}, i, {
      kindText: util.KIND_TEXT[i.kind] || i.kind,
      statusText: util.LOST_STATUS_TEXT[i.status] || i.status,
      timeText: util.formatTime(i.lostTime || i.createdAt, false),
      cover: (i.images && i.images[0]) || i.coverUrl || ''
    }));
  },

  reload() {
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.lostItems.list(this.buildQuery(1)).then((data) => {
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

  /* ---------- 筛选 ---------- */
  onKind(e) {
    this.setData({ kindIndex: Number(e.currentTarget.dataset.index) });
    this.reload();
  },
  onCat(e) {
    this.setData({ catIndex: Number(e.detail.value) });
    this.reload();
  },
  onBuilding(e) {
    this.setData({ buildingIndex: Number(e.detail.value) });
    this.reload();
  },
  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  onKeywordConfirm() { this.reload(); },
  clearKeyword() { this.setData({ keyword: '' }); this.reload(); },

  goPublish() {
    if (!this.data.hasToken) return wx.navigateTo({ url: '/pages/login/login' });
    wx.navigateTo({ url: '/pages/lostfound/publish' });
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/lostfound/detail?id=' + id });
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.reload(); }
});
