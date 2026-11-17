const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    loading: true,
    error: false,
    list: []
  },

  onLoad() {
    this.reload();
  },

  onPullDownRefresh() {
    this.reload();
    wx.stopPullDownRefresh();
  },

  reload() {
    this.setData({ loading: true, error: false });
    api.buildings.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      list.forEach(b => {
        // 演示数据标注：楼名后缀提示（若后端未标注）
        b.classroomText = b.classroomCount != null ? b.classroomCount + ' 间教室' : '教室详情见内页';
        b.floorText = b.floorsCount != null ? b.floorsCount + ' 层' : '';
      });
      this.setData({ list, loading: false });
    }).catch(() => {
      this.setData({ loading: false, error: true, list: [] });
    });
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/building/detail?id=' + id });
  },

  goSearch() {
    wx.switchTab({ url: '/pages/classroom/search' });
  },

  retry() { this.reload(); }
});
