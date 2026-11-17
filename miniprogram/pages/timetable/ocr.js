const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

const POLL_INTERVAL = 2000;  // 轮询间隔 ms
const POLL_MAX = 40;         // 最多轮询 80 秒

Page({
  data: {
    imgPath: '',        // 本地预览图
    uploading: false,
    taskId: null,
    taskStatus: '',     // UPLOADED / PROCESSING / DONE / FAILED
    statusText: '',
    errorMsg: '',
    done: false
  },

  onLoad() {},

  onUnload() {
    this.stopPolling();
  },

  chooseImage() {
    if (this.data.uploading || this.data.taskId) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (file) this.setData({ imgPath: file.tempFilePath, errorMsg: '', done: false });
      },
      fail: () => {}
    });
  },

  clearImage() {
    this.stopPolling();
    this.setData({ imgPath: '', taskId: null, taskStatus: '', statusText: '', errorMsg: '', done: false });
  },

  /* 上传 → 创建 OCR 任务 → 轮询 */
  startOcr() {
    if (this.data.uploading || !this.data.imgPath) return;
    this.setData({ uploading: true, errorMsg: '' });
    api.uploadImageBase64(this.data.imgPath).then((data) => {
      const ossKey = data && (data.ossKey || data.key);
      if (!ossKey) throw new Error('上传返回异常');
      return api.timetable.createOcrTask(ossKey);
    }).then((data) => {
      const taskId = data && (data.taskId || data.id);
      if (!taskId) throw new Error('任务创建失败');
      this.setData({
        uploading: false,
        taskId,
        taskStatus: 'UPLOADED',
        statusText: '任务已创建，等待识别…'
      });
      this.startPolling();
    }).catch((err) => {
      this.setData({ uploading: false });
      // OCR 服务未配置：明确提示并引导手动录入
      if (err && err.code === 50301) {
        this.setData({ taskStatus: 'FAILED', errorMsg: 'OCR 服务未配置，请手动录入' });
        wx.showModal({
          title: 'OCR 服务未配置',
          content: '当前后端未部署 OCR 服务，无法识别课表图片。请点击"确定"返回课程表手动录入课程。',
          showCancel: false,
          confirmText: '去手动录入',
          success: () => {
            wx.switchTab({ url: '/pages/timetable/index' });
          }
        });
        return;
      }
      util.toastError(err, '上传失败，请重试');
    });
  },

  startPolling() {
    this.stopPolling();
    this.pollCount = 0;
    this.timer = setInterval(() => this.pollOnce(), POLL_INTERVAL);
    this.pollOnce();
  },

  stopPolling() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  },

  pollOnce() {
    if (!this.data.taskId) return this.stopPolling();
    this.pollCount++;
    if (this.pollCount > POLL_MAX) {
      this.stopPolling();
      this.setData({ taskStatus: 'FAILED', statusText: '', errorMsg: '识别超时，请稍后重试或手动录入' });
      return;
    }
    api.timetable.getOcrTask(this.data.taskId).then((task) => {
      const status = (task && task.status) || '';
      if (status === 'DONE') {
        this.stopPolling();
        this.setData({ taskStatus: 'DONE', statusText: '识别完成', done: true });
        // 进入校正页核对后再保存，绝不静默写入课表
        wx.redirectTo({ url: '/pages/timetable/correct?taskId=' + this.data.taskId });
      } else if (status === 'FAILED') {
        this.stopPolling();
        this.setData({
          taskStatus: 'FAILED',
          statusText: '',
          errorMsg: (task && task.errorMsg) || '识别失败，请换张清晰的照片重试'
        });
      } else {
        this.setData({ taskStatus: status, statusText: status === 'PROCESSING' ? '正在识别中…' : '排队中…' });
      }
    }).catch((err) => {
      // 单次轮询失败不中断，连续失败由超时兜底
      if (err && err.code === 404) {
        this.stopPolling();
        this.setData({ taskStatus: 'FAILED', statusText: '', errorMsg: '任务不存在或已过期' });
      }
    });
  },

  reChoose() { this.clearImage(); },

  goManual() {
    wx.switchTab({ url: '/pages/timetable/index' });
  }
});
