const env = require('./config/env.js');
const api = require('./utils/api.js');

App({
  globalData: {
    userInfo: null,
    devMode: false,
    appName: env.APP_NAME
  },

  onLaunch() {
    this.initSession();
  },

  // 启动时若有 token 则拉取用户信息，失效则清除
  initSession() {
    const token = wx.getStorageSync('campus_token');
    if (!token) return;
    api.getMe({ silent: true, loading: false })
      .then((data) => {
        this.globalData.userInfo = data || null;
        if (data && data.avatarUrl) wx.setStorageSync('campus_userInfo', data);
      })
      .catch(() => {
        // token 失效静默清除，等下一次需要登录的页面引导
        wx.removeStorageSync('campus_token');
        this.globalData.userInfo = null;
      });
  },

  isLoggedIn() {
    return !!wx.getStorageSync('campus_token');
  }
});
