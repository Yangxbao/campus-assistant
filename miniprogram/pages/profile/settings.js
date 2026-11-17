const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

Page({
  data: {
    hasToken: false,
    userInfo: null,
    nickname: '',
    phoneVisible: false,
    savingProfile: false,
    uploadingAvatar: false,
    devMode: false
  },

  onShow() {
    const hasToken = !!req.getToken();
    this.setData({ hasToken });
    if (hasToken) this.loadMe();
  },

  loadMe() {
    const cached = wx.getStorageSync('campus_userInfo') || null;
    if (cached) {
      this.applyUser(cached);
    }
    api.user.getMe({ silent: true, loading: false }).then((data) => {
      if (data) this.applyUser(data);
    }).catch(() => {});
  },

  applyUser(u) {
    this.setData({
      userInfo: u,
      nickname: u.nickname || '',
      phoneVisible: u.phoneVisible === 1 || u.phoneVisible === true,
      devMode: !!getApp().globalData.devMode
    });
  },

  /* ---------- 昵称 ---------- */
  onNickname(e) { this.setData({ nickname: e.detail.value }); },

  saveNickname() {
    if (this.data.savingProfile) return;
    const name = this.data.nickname.trim();
    if (!name) return wx.showToast({ title: '昵称不能为空', icon: 'none' });
    this.setData({ savingProfile: true });
    api.user.updateMe({ nickname: name }).then((data) => {
      this.setData({ savingProfile: false });
      if (data) this.applyUser(Object.assign({}, this.data.userInfo, data));
      wx.setStorageSync('campus_userInfo', Object.assign({}, this.data.userInfo, { nickname: name }));
      wx.showToast({ title: '已保存', icon: 'success' });
    }).catch((err) => {
      this.setData({ savingProfile: false });
      util.toastError(err, '保存失败');
    });
  },

  /* ---------- 头像 ---------- */
  changeAvatar() {
    if (this.data.uploadingAvatar) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file) return;
        this.setData({ uploadingAvatar: true });
        api.uploadImageBase64(file.tempFilePath).then((data) => {
          const key = data && data.ossKey;
          const url = data && data.url;
          return api.user.updateMe({ avatarUrl: key || url }).then(() => {
            const newUrl = url || util.imgUrl({ ossKey: key });
            wx.setStorageSync('campus_userInfo', Object.assign({}, this.data.userInfo, { avatarUrl: newUrl }));
            this.setData({
              uploadingAvatar: false,
              'userInfo.avatarUrl': newUrl
            });
            wx.showToast({ title: '头像已更新', icon: 'success' });
          });
        }).catch((err) => {
          this.setData({ uploadingAvatar: false });
          util.toastError(err, '头像更新失败');
        });
      },
      fail: () => {}
    });
  },

  /* ---------- 手机号可见性 ---------- */
  onPhoneVisible(e) {
    const v = e.detail.value;
    this.setData({ phoneVisible: v });
    api.user.updateMe({ phoneVisible: v ? 1 : 0 }).then(() => {
      wx.showToast({ title: v ? '已公开手机号' : '已隐藏手机号', icon: 'none' });
    }).catch((err) => {
      this.setData({ phoneVisible: !v });
      util.toastError(err, '设置失败');
    });
  },

  /* ---------- 退出登录 ---------- */
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需要重新登录才能使用完整功能，确定退出吗？',
      success: (res) => {
        if (!res.confirm) return;
        req.clearToken();
        wx.removeStorageSync('campus_userInfo');
        this.setData({ hasToken: false, userInfo: null });
        wx.showToast({ title: '已退出登录', icon: 'success' });
        setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 700);
      }
    });
  },

  /* ---------- 注销 ---------- */
  deleteAccount() {
    wx.showModal({
      title: '注销账号',
      content: '注销后你的账号数据将被删除（逻辑删除），包括课程表、收藏、发布的失物信息等，且不可恢复。确定继续吗？',
      confirmColor: '#F25555',
      confirmText: '继续注销',
      success: (res) => {
        if (!res.confirm) return;
        wx.showModal({
          title: '再次确认',
          content: '这是最后一次确认，注销操作不可撤销！',
          confirmColor: '#F25555',
          success: (res2) => {
            if (!res2.confirm) return;
            api.user.deleteMe().then(() => {
              req.clearToken();
              wx.removeStorageSync('campus_userInfo');
              wx.showToast({ title: '账号已注销', icon: 'success' });
              setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 800);
            }).catch((err) => util.toastError(err, '注销失败'));
          }
        });
      }
    });
  }
});
