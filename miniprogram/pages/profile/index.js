const api = require('../../utils/api.js');
const req = require('../../utils/request.js');

const PRIVACY_TEXT = '1. 本应用为学生社区项目，非学校官方应用。\n\n' +
  '2. 联系方式默认脱敏展示，公开与否由你自行控制；认领申请仅在双方之间可见。\n\n' +
  '3. 教室照片、失物信息等内容需审核通过后才会公开展示。\n\n' +
  '4. 你可以随时在「账号设置」中退出登录或注销账号，注销后数据将被逻辑删除。';

Page({
  data: {
    hasToken: false,
    userInfo: null,
    unreadCount: 0,
    showPrivacy: false,
    privacyText: PRIVACY_TEXT,
    menus: [
      { key: 'timetable', icon: '📅', label: '我的课表', url: 'switch:/pages/timetable/index' },
      { key: 'favorites', icon: '⭐', label: '我的收藏', url: '/pages/profile/favorites' },
      { key: 'myItems', icon: '🧳', label: '我的失物招领', url: '/pages/profile/my-items' },
      { key: 'contributions', icon: '✋', label: '我的贡献', url: '/pages/profile/contributions' },
      { key: 'photos', icon: '📷', label: '我的照片', url: '/pages/profile/contributions?type=photo' },
      { key: 'feedback', icon: '💬', label: '我的反馈', url: '/pages/profile/contributions?type=feedback' },
      { key: 'messages', icon: '🔔', label: '我的消息', url: '/pages/profile/messages', badge: true },
      { key: 'settings', icon: '⚙️', label: '账号设置', url: '/pages/profile/settings' }
    ]
  },

  onShow() {
    const hasToken = !!req.getToken();
    this.setData({ hasToken });
    if (hasToken) {
      this.loadMe();
      this.loadUnread();
    } else {
      this.setData({ userInfo: null, unreadCount: 0 });
    }
  },

  loadMe() {
    api.user.getMe({ silent: true, loading: false }).then((data) => {
      this.setData({ userInfo: data || null });
      if (data) wx.setStorageSync('campus_userInfo', data);
    }).catch(() => {});
  },

  loadUnread() {
    api.messages.unreadCount().then((data) => {
      const count = typeof data === 'number' ? data : (data && data.count) || 0;
      this.setData({ unreadCount: count });
    }).catch(() => {});
  },

  onMenu(e) {
    const key = e.currentTarget.dataset.key;
    if (!this.data.hasToken) return this.goLogin();
    const menu = this.data.menus.find(m => m.key === key);
    if (!menu) return;
    if (menu.url.indexOf('switch:') === 0) {
      wx.switchTab({ url: menu.url.slice(7) });
    } else {
      wx.navigateTo({ url: menu.url });
    }
  },

  openPrivacy() { this.setData({ showPrivacy: true }); },
  closePrivacy() { this.setData({ showPrivacy: false }); },
  noop() {},

  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },

  /** 更换头像：选图 → base64 上传 → 更新 avatarUrl */
  onChangeAvatar() {
    if (!this.data.hasToken) return this.goLogin();
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath;
        if (!filePath) return;
        wx.showLoading({ title: '上传中', mask: true });
        api.uploadImageBase64(filePath)
          .then((data) => {
            const avatarUrl = data && data.url;
            if (!avatarUrl) throw new Error('上传结果缺少 url');
            return api.user.updateMe({ avatarUrl }).then(() => avatarUrl);
          })
          .then((avatarUrl) => {
            wx.hideLoading();
            const info = Object.assign({}, this.data.userInfo || {}, { avatarUrl });
            this.setData({ userInfo: info });
            wx.setStorageSync('campus_userInfo', info);
            wx.showToast({ title: '头像已更新', icon: 'success' });
          })
          .catch((err) => {
            wx.hideLoading();
            wx.showToast({ title: '头像更新失败', icon: 'none' });
            console.error('onChangeAvatar failed', err);
          });
      }
    });
  },

  logout() {
    if (!this.data.hasToken) return;
    wx.showModal({
      title: '退出登录',
      content: '退出后需要重新登录才能使用完整功能，确定退出吗？',
      success: (res) => {
        if (!res.confirm) return;
        req.clearToken();
        wx.removeStorageSync('campus_userInfo');
        this.setData({ hasToken: false, userInfo: null, unreadCount: 0 });
        wx.showToast({ title: '已退出登录', icon: 'success' });
      }
    });
  }
});
