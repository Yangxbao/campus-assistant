/**
 * 统一请求封装
 * - 自动携带 Authorization: Bearer <token>
 * - 统一处理响应包 { code, message, data }，code=0 成功
 * - 401 清除会话并跳转登录页
 * - loading 计数管理、错误 toast、网络异常提示
 * - silent: 页面自行处理错误时不弹 toast
 * - code=50301 等业务码抛给页面自行处理（如 OCR 未配置）
 */
const env = require('../config/env.js');

const TOKEN_KEY = 'campus_token';

let loadingCount = 0;
let redirectingLogin = false;

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token);
}

function clearToken() {
  wx.removeStorageSync(TOKEN_KEY);
  const app = getApp();
  if (app) app.globalData.userInfo = null;
}

function showError(msg) {
  wx.showToast({ title: msg || '请求失败', icon: 'none', duration: 2200 });
}

function gotoLogin() {
  if (redirectingLogin) return;
  const pages = getCurrentPages();
  const current = pages[pages.length - 1];
  if (current && current.route === 'pages/login/login') return;
  redirectingLogin = true;
  setTimeout(() => {
    redirectingLogin = false;
    wx.reLaunch({ url: '/pages/login/login' });
  }, 600);
}

/**
 * @param {Object} options
 * @param {string} options.url    以 /mp/... 开头的接口路径
 * @param {string} [options.method]
 * @param {Object} [options.data]
 * @param {boolean} [options.loading=true] 是否显示全局 loading
 * @param {boolean} [options.silent=false] 出错时不弹 toast，由页面处理
 */
function request(options) {
  const {
    url,
    method = 'GET',
    data,
    loading = true,
    silent = false
  } = options;

  if (loading) {
    loadingCount++;
    if (loadingCount === 1) wx.showLoading({ title: '加载中', mask: true });
  }

  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' };
    const token = getToken();
    if (token) header.Authorization = 'Bearer ' + token;

    wx.request({
      url: env.BASE_URL + url,
      method,
      data,
      header,
      success(res) {
        // 未授权：清除会话跳登录
        if (res.statusCode === 401) {
          clearToken();
          if (!silent) showError('登录已过期，请重新登录');
          gotoLogin();
          return reject(makeErr(401, '登录已过期'));
        }
        // 服务器异常
        if (res.statusCode >= 500) {
          if (!silent) showError('服务器开小差了，请稍后重试');
          return reject(makeErr(res.statusCode, '服务器异常'));
        }
        // 教室等资源不存在：交给页面处理（如"申请新增教室"）
        if (res.statusCode === 404) {
          const msg = (res.data && res.data.message) || '内容不存在';
          return reject(makeErr(404, msg));
        }

        const body = res.data;
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 0) return resolve(body.data);
          const err = makeErr(body.code, body.message || '请求失败');
          // 50301（OCR 未配置）等业务码由页面自行处理，不统一弹错
          if (body.code !== 50301 && !silent) showError(err.message);
          return reject(err);
        }

        // 非标准响应包
        if (res.statusCode >= 200 && res.statusCode < 300) {
          return resolve(body === '' ? null : body);
        }
        if (!silent) showError('响应格式异常');
        reject(makeErr(res.statusCode, '响应格式异常'));
      },
      fail(err) {
        if (!silent) showError('网络异常，请检查网络后重试');
        reject(makeErr(-1, '网络异常'));
      },
      complete() {
        if (loading) {
          loadingCount = Math.max(0, loadingCount - 1);
          if (loadingCount === 0) wx.hideLoading();
        }
      }
    });
  });
}

function makeErr(code, message) {
  const err = new Error(message || '请求失败');
  err.code = code;
  err.errMsg = message;
  return err;
}

const get = (url, data, extra) => request(Object.assign({ url, method: 'GET', data }, extra));
const post = (url, data, extra) => request(Object.assign({ url, method: 'POST', data }, extra));
const put = (url, data, extra) => request(Object.assign({ url, method: 'PUT', data }, extra));
const del = (url, data, extra) => request(Object.assign({ url, method: 'DELETE', data }, extra));

module.exports = { request, get, post, put, del, getToken, setToken, clearToken };
