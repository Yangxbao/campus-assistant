const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const env = require('../../config/env.js');
const util = require('../../utils/util.js');

Page({
  data: {
    devMode: false,
    devHint: env.DEV_MODE_HINT,
    logging: false,
    loggedIn: false,
    userInfo: null
  },

  onLoad() {
    if (req.getToken()) {
      const cached = wx.getStorageSync('campus_userInfo');
      this.setData({ loggedIn: true, userInfo: cached || null });
    }
  },

  // 微信一键登录：wx.login → POST /mp/auth/login
  onLogin() {
    if (this.data.logging) return;
    this.setData({ logging: true });
    wx.login({
      success: (res) => {
        if (!res.code) {
          this.setData({ logging: false });
          return util.toastError(null, '获取微信 code 失败');
        }
        api.auth.login(res.code)
          .then((data) => {
            req.setToken(data.token);
            const app = getApp();
            app.globalData.userInfo = data.userInfo || null;
            app.globalData.devMode = !!data.devMode;
            wx.setStorageSync('campus_userInfo', data.userInfo || null);
            this.setData({
              logging: false,
              loggedIn: true,
              userInfo: data.userInfo || null,
              devMode: !!data.devMode
            });
            if (data.devMode) {
              wx.showToast({ title: '开发模式登录成功', icon: 'none' });
            } else {
              wx.showToast({ title: '登录成功', icon: 'success' });
            }
            setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 800);
          })
          .catch((err) => {
            this.setData({ logging: false });
            util.toastError(err, '登录失败，请重试');
          });
      },
      fail: () => {
        this.setData({ logging: false });
        util.toastError(null, '微信登录调用失败');
      }
    });
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  }
});
