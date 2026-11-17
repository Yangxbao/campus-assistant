const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

const PAGE_SIZE = 10;

Page({
  data: {
    keyword: '',
    buildings: [],
    buildingIndex: -1,
    floorNo: '',
    roomNo: '',
    typeIndex: -1,
    types: [
      { value: '', label: '全部类型' },
      { value: 'LINEAR', label: '大教室' },
      { value: 'SMALL', label: '小教室' },
      { value: 'LAB', label: '实验室' },
      { value: 'MULTIMEDIA', label: '多媒体' },
      { value: 'OTHER', label: '其他' }
    ],
    deviceFilters: [
      { key: 'hasAc', label: '空调', on: false },
      { key: 'hasProjector', label: '投影', on: false },
      { key: 'hasPc', label: '电脑', on: false },
      { key: 'hasMic', label: '麦克风', on: false },
      { key: 'hasWhiteboard', label: '白板', on: false },
      { key: 'hasWifi', label: 'WiFi', on: false }
    ],
    hasSocketMin: '',
    minQuiet: 0,
    minQuietLabel: '不限',
    suitableForSelfStudy: false,
    sortIndex: 0,
    sorts: [
      { value: 'updated_desc', label: '最近更新' },
      { value: 'review_desc', label: '评价最高' }
    ],
    list: [],
    page: 1,
    total: 0,
    hasMore: false,
    loading: false,
    loadingMore: false,
    error: false,
    showFilter: false,
    activeCount: 0
  },

  onLoad(options) {
    if (options.keyword) this.setData({ keyword: decodeURIComponent(options.keyword) });
    this.loadBuildings();
    this.reload();
  },

  loadBuildings() {
    api.buildings.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      this.setData({ buildings: list });
    }).catch(() => {});
  },

  buildQuery(page) {
    const d = this.data;
    const q = { page, size: PAGE_SIZE, sort: d.sorts[d.sortIndex].value };
    if (d.keyword) q.keyword = d.keyword;
    if (d.buildingIndex >= 0) q.buildingId = d.buildings[d.buildingIndex].id;
    if (d.floorNo) q.floorNo = d.floorNo;
    if (d.roomNo) q.roomNo = d.roomNo;
    if (d.typeIndex > 0) q.type = d.types[d.typeIndex].value;
    d.deviceFilters.forEach(f => { if (f.on) q[f.key] = true; });
    if (d.hasSocketMin && Number(d.hasSocketMin) > 0) q.hasSocket = Number(d.hasSocketMin);
    if (d.minQuiet > 0) q.minQuiet = d.minQuiet;
    if (d.suitableForSelfStudy) q.suitableForSelfStudy = true;
    return q;
  },

  decorate(list) {
    (list || []).forEach(c => {
      c.typeText = util.ROOM_TYPE_TEXT[c.type] || c.type || '教室';
      c.updatedText = util.formatTime(c.updatedContentAt || c.updatedAt, false);
    });
    return list || [];
  },

  reload() {
    this.setData({ page: 1, loading: true, error: false, list: [] });
    api.classrooms.search(this.buildQuery(1)).then((data) => {
      const info = util.pageData(data, 1, PAGE_SIZE);
      this.setData({
        list: this.decorate(info.list),
        total: info.total,
        hasMore: info.hasMore,
        loading: false
      });
    }).catch((err) => {
      this.setData({ loading: false, error: true });
    });
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loadingMore || this.data.loading) return;
    const nextPage = this.data.page + 1;
    this.setData({ loadingMore: true });
    api.classrooms.search(this.buildQuery(nextPage)).then((data) => {
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

  /* ---------- 筛选交互 ---------- */
  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  onKeywordConfirm() { this.reload(); },
  clearKeyword() { this.setData({ keyword: '' }); this.reload(); },

  toggleFilter() { this.setData({ showFilter: !this.data.showFilter }); },

  onBuilding(e) { this.setData({ buildingIndex: Number(e.detail.value) }); },
  onFloorNo(e) { this.setData({ floorNo: e.detail.value }); },
  onRoomNo(e) { this.setData({ roomNo: e.detail.value }); },
  onType(e) { this.setData({ typeIndex: Number(e.detail.value) }); },
  onSort(e) {
    this.setData({ sortIndex: Number(e.detail.value) });
    this.reload();
  },

  onDevice(e) {
    const idx = e.currentTarget.dataset.index;
    const key = `deviceFilters[${idx}].on`;
    this.setData({ [key]: !this.data.deviceFilters[idx].on });
  },
  onSocketMin(e) { this.setData({ hasSocketMin: e.detail.value }); },
  onQuietChange(e) {
    const v = Number(e.detail.value);
    this.setData({ minQuiet: v, minQuietLabel: v === 0 ? '不限' : v + ' 星及以上' });
  },
  onSelfStudy(e) { this.setData({ suitableForSelfStudy: e.detail.value }); },

  applyFilter() {
    this.setData({ showFilter: false });
    this.countActive();
    this.reload();
  },

  resetFilter() {
    this.setData({
      buildingIndex: -1, floorNo: '', roomNo: '', typeIndex: -1,
      deviceFilters: this.data.deviceFilters.map(f => Object.assign({}, f, { on: false })),
      hasSocketMin: '', minQuiet: 0, minQuietLabel: '不限',
      suitableForSelfStudy: false
    });
    this.countActive();
    this.reload();
  },

  countActive() {
    const d = this.data;
    let n = 0;
    if (d.buildingIndex >= 0) n++;
    if (d.floorNo) n++;
    if (d.roomNo) n++;
    if (d.typeIndex > 0) n++;
    d.deviceFilters.forEach(f => { if (f.on) n++; });
    if (d.hasSocketMin) n++;
    if (d.minQuiet > 0) n++;
    if (d.suitableForSelfStudy) n++;
    this.setData({ activeCount: n });
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/classroom/detail?id=' + id });
  },

  retry() { this.reload(); }
});
