const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

Page({
  data: {
    hasToken: false,
    tabs: [
      { type: 'CLASSROOM', label: '收藏的教室' },
      { type: 'LOST_ITEM', label: '收藏的失物招领' }
    ],
    tabIndex: 0,
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

  switchTab(e) {
    this.setData({ tabIndex: Number(e.currentTarget.dataset.index) });
    this.reload();
  },

  decorate(type, list) {
    return (list || []).map(item => {
      const it = item.classroom || item.lostItem || item.target || item;
      const o = Object.assign({}, it, { favoriteId: item.id });
      if (type === 'CLASSROOM') {
        o.title = (o.buildingName || '') + ' ' + (o.roomNo || o.name || '');
        o.sub = (o.floorNo != null ? o.floorNo + ' 层 · ' : '') + (util.ROOM_TYPE_TEXT[o.type] || '教室');
      } else {
        o.title = o.title || '未命名物品';
        o.sub = o.category || '';
        o.statusText = util.LOST_STATUS_TEXT[o.status] || o.status;
        o.timeText = util.formatTime(o.lostTime || o.createdAt, false);
      }
      return o;
    });
  },

  reload() {
    const type = this.data.tabs[this.data.tabIndex].type;
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.favorites.list(type).then((data) => {
      // 收藏列表可能直接返回数组
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      const info = util.pageData(Array.isArray(data) ? { list, total: list.length } : data, 1, Math.max(list.length, PAGE_SIZE));
      this.setData({
        list: this.decorate(type, info.list),
        hasMore: info.hasMore,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loadingMore || this.data.loading) return;
    const type = this.data.tabs[this.data.tabIndex].type;
    const nextPage = this.data.page + 1;
    this.setData({ loadingMore: true });
    api.favorites.list(type).then((data) => {
      const info = util.pageData(data, nextPage, PAGE_SIZE);
      this.setData({
        list: this.data.list.concat(this.decorate(type, info.list)),
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
    const type = this.data.tabs[this.data.tabIndex].type;
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    if (type === 'CLASSROOM') {
      wx.navigateTo({ url: '/pages/classroom/detail?id=' + id });
    } else {
      wx.navigateTo({ url: '/pages/lostfound/detail?id=' + id });
    }
  },

  removeFavorite(e) {
    const { id, target } = e.currentTarget.dataset;
    const type = this.data.tabs[this.data.tabIndex].type;
    wx.showModal({
      title: '取消收藏',
      content: '确定取消收藏吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.favorites.remove(type, target).then(() => {
          wx.showToast({ title: '已取消收藏', icon: 'success' });
          this.reload();
        }).catch((err) => util.toastError(err, '操作失败'));
      }
    });
  },

  goSearch() { wx.switchTab({ url: '/pages/classroom/search' }); },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  retry() { this.reload(); }
});
