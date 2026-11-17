const api = require('../../utils/api.js');
const util = require('../../utils/util.js');

const MAX_IMAGES = 6;

Page({
  data: {
    kind: 'LOST',
    title: '',
    categories: util.CATEGORY_LIST,
    catIndex: -1,
    description: '',
    date: '',          // yyyy-MM-dd
    time: '12:00',
    buildings: [],
    buildingIndex: -1,
    locationDesc: '',
    images: [],        // 本地预览
    uploading: false,
    contactVisible: true,
    contactValue: '',
    submitting: false
  },

  onLoad() {
    const now = new Date();
    const pad = n => (n < 10 ? '0' + n : '' + n);
    this.setData({
      date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    });
    this.loadBuildings();
  },

  loadBuildings() {
    api.buildings.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      this.setData({ buildings: list });
    }).catch(() => {});
  },

  switchKind(e) { this.setData({ kind: e.currentTarget.dataset.kind }); },

  onTitle(e) { this.setData({ title: e.detail.value }); },
  onCat(e) { this.setData({ catIndex: Number(e.detail.value) }); },
  onDesc(e) { this.setData({ description: e.detail.value }); },
  onDate(e) { this.setData({ date: e.detail.value }); },
  onTime(e) { this.setData({ time: e.detail.value }); },
  onLocation(e) { this.setData({ locationDesc: e.detail.value }); },
  onBuilding(e) { this.setData({ buildingIndex: Number(e.detail.value) }); },
  onContact(e) { this.setData({ contactValue: e.detail.value }); },
  onVisible(e) { this.setData({ contactVisible: e.detail.value }); },

  /* ---------- 多图上传 ---------- */
  chooseImages() {
    if (this.data.uploading) return;
    const remain = MAX_IMAGES - this.data.images.length;
    if (remain <= 0) {
      wx.showToast({ title: `最多上传 ${MAX_IMAGES} 张`, icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      success: (res) => {
        const paths = (res.tempFiles || []).map(f => f.tempFilePath);
        this.uploadImages(paths);
      },
      fail: () => {}
    });
  },

  uploadImages(paths) {
    if (!paths.length) return;
    this.setData({ uploading: true });
    let done = 0;
    const uploaded = [];
    const next = () => {
      if (done >= paths.length) {
        this.setData({
          images: this.data.images.concat(uploaded),
          uploading: false
        });
        return;
      }
      api.uploadImageBase64(paths[done]).then((data) => {
        uploaded.push({
          path: paths[done],
          ossKey: data && data.ossKey,
          url: (data && data.url) || paths[done]
        });
        done++;
        next();
      }).catch(() => {
        wx.showToast({ title: `第 ${done + 1} 张上传失败，已跳过`, icon: 'none' });
        done++;
        next();
      });
    };
    next();
  },

  removeImage(e) {
    const idx = Number(e.currentTarget.dataset.index);
    const images = this.data.images.slice();
    images.splice(idx, 1);
    this.setData({ images });
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    wx.previewImage({
      current: url,
      urls: this.data.images.map(i => i.url)
    });
  },

  /* ---------- 提交 ---------- */
  submit() {
    if (this.data.submitting) return;
    const d = this.data;
    if (!d.title.trim()) return wx.showToast({ title: '请填写物品名称', icon: 'none' });
    if (d.catIndex < 0) return wx.showToast({ title: '请选择类别', icon: 'none' });
    if (!d.date) return wx.showToast({ title: '请选择发生时间', icon: 'none' });
    if (d.contactVisible && !d.contactValue.trim()) {
      return wx.showToast({ title: '联系方式公开时必须填写联系方式', icon: 'none' });
    }

    const payload = {
      kind: d.kind,
      title: d.title.trim(),
      category: d.categories[d.catIndex],
      description: d.description.trim(),
      lostTime: d.date + ' ' + (d.time || '12:00') + ':00',
      buildingId: d.buildingIndex >= 0 ? d.buildings[d.buildingIndex].id : null,
      locationDesc: d.locationDesc.trim(),
      ossKeys: d.images.map(i => i.ossKey).filter(Boolean),
      contactVisible: d.contactVisible ? 1 : 0,
      contactValue: d.contactValue.trim()
    };

    this.setData({ submitting: true });
    api.lostItems.create(payload).then(() => {
      this.setData({ submitting: false });
      wx.showModal({
        title: '提交成功',
        content: '信息已提交，需审核通过后才会公开展示，可在「我的-我的失物招领」中查看进度。',
        showCancel: false,
        success: () => wx.navigateBack()
      });
    }).catch((err) => {
      this.setData({ submitting: false });
      util.toastError(err, '提交失败，请重试');
    });
  }
});
