const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

Page({
  data: {
    id: null,
    loading: true,
    error: false,
    b: null,
    guideText: '',
    showGuide: false,
    loadingGuide: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.reload();
  },

  onPullDownRefresh() {
    this.reload();
    wx.stopPullDownRefresh();
  },

  reload() {
    this.setData({ loading: true, error: false });
    api.buildings.detail(this.data.id).then((data) => {
      const floors = (data && data.floors) || [];
      floors.forEach(f => {
        f.floorLabel = (f.floorNo != null ? f.floorNo + ' 层' : '楼层');
        (f.classrooms || []).forEach(c => {
          c.typeText = util.ROOM_TYPE_TEXT[c.type] || c.type || '教室';
        });
      });
      this.setData({ b: data, floors, loading: false });
      wx.setNavigationBarTitle({ title: data.name || '楼宇详情' });
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  /* 文字导航：按需加载 */
  toggleGuide() {
    if (this.data.showGuide) {
      this.setData({ showGuide: false });
      return;
    }
    if (this.data.guideText) {
      this.setData({ showGuide: true });
      return;
    }
    this.setData({ loadingGuide: true });
    api.buildings.guide(this.data.id).then((data) => {
      // data 可能为字符串或 { guide / content / steps[] }
      let text = '';
      if (typeof data === 'string') text = data;
      else if (Array.isArray(data)) text = data.join('\n');
      else if (data && typeof data === 'object') {
        text = data.guide || data.content || data.text ||
          (Array.isArray(data.steps) ? data.steps.join('\n') : JSON.stringify(data));
      }
      this.setData({ guideText: text || '暂无导航信息', showGuide: true, loadingGuide: false });
    }).catch((err) => {
      this.setData({ loadingGuide: false });
      util.toastError(err, '导航信息获取失败');
    });
  },

  goClassroom(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/classroom/detail?id=' + id });
  },

  retry() { this.reload(); }
});
