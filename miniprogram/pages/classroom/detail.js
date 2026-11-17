const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

const IMAGE_CATEGORIES = [
  { value: 'OVERALL', label: '整体' },
  { value: 'FRONT', label: '前方' },
  { value: 'BACK', label: '后方' },
  { value: 'SOCKET', label: '插座' },
  { value: 'AC', label: '空调' },
  { value: 'PROJECTOR', label: '投影' },
  { value: 'OTHER', label: '其他' }
];

// 可修正字段（教室资料贡献）
const REPORT_FIELDS = [
  { key: 'hasAc', label: '空调', type: 'bool' },
  { key: 'hasProjector', label: '投影仪', type: 'bool' },
  { key: 'hasPc', label: '电脑', type: 'bool' },
  { key: 'hasMic', label: '麦克风', type: 'bool' },
  { key: 'hasWhiteboard', label: '白板', type: 'bool' },
  { key: 'hasWifi', label: 'WiFi', type: 'bool' },
  { key: 'socket_count', label: '插座数量', type: 'number' },
  { key: 'socket_desc', label: '插座位置描述', type: 'text' },
  { key: 'other_devices', label: '其他设备', type: 'text' },
  { key: 'env_desc', label: '环境描述', type: 'text' }
];

const WALL_POS = [
  { value: 'FRONT', label: '前墙' },
  { value: 'BACK', label: '后墙' },
  { value: 'LEFT', label: '左墙' },
  { value: 'RIGHT', label: '右墙' },
  { value: 'NEAR_PODIUM', label: '讲台附近' },
  { value: 'WINDOW', label: '窗边' }
];

Page({
  data: {
    id: null,
    loading: true,
    error: false,
    notFound: false,
    c: null,

    /* 照片九宫格 */
    categories: [{ value: '', label: '全部' }].concat(IMAGE_CATEGORIES),
    activeCat: '',
    allImages: [],
    shownImages: [],
    previewUrls: [],

    /* 插座标记 */
    socketMarks: [],
    marksOnImages: [],   // 有照片的标记
    marksNoImage: [],    // 无照片的标记
    wallPosText: util.WALL_POS_TEXT,

    /* 临时状态 */
    latestStatus: null,
    statusExpireText: '',
    occupancyText: util.OCCUPANCY_TEXT,

    /* 评价 */
    reviews: [],
    reviewRating: 5,
    reviewContent: '',
    submittingReview: false,

    /* 文字导航 */
    guideText: '',
    showGuide: false,

    /* 收藏 */
    isFavorite: false,

    /* 我要更新资料 */
    showReport: false,
    reportFields: REPORT_FIELDS,
    reportIndex: 0,
    reportBoolValue: 'true',
    reportValue: '',
    reportRemark: '',

    /* 上传照片 */
    uploading: false,
    uploadCatIndex: 0,
    uploadDesc: '',
    uploadPreview: '',

    /* 标记插座 */
    showMark: false,
    markImageIndex: 0,
    markImages: [],
    markRect: null,
    markPoints: [],      // 用户在弹层图片上点击的临时坐标
    markWallIndex: 0,
    markNote: '',

    /* 临时状态上报 */
    showStatus: false,
    occupancyOptions: [
      { value: 'FREE', label: '空闲' },
      { value: 'PARTLY', label: '部分占用' },
      { value: 'FULL', label: '占用' },
      { value: 'UNKNOWN', label: '未知' }
    ],
    statusOccIndex: 0,
    statusQuiet: 3,
    statusAcIndex: 2,   // 0 正常 / 1 损坏 / 2 未知
    statusSocketIndex: 2,
    statusReason: '',
    statusHours: 4,

    /* 申请新增教室（未收录） */
    showApply: false,
    applyBuildings: [],
    applyBuildingIndex: -1,
    applyFloorNo: '',
    applyRoomNo: '',
    applyRemark: ''
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadAll();
  },

  onShow() {
    if (req.getToken()) this.checkFavorite();
  },

  loadAll() {
    const id = this.data.id;
    this.setData({ loading: true, error: false, notFound: false });
    api.classrooms.detail(id)
      .then((c) => {
        c.typeText = util.ROOM_TYPE_TEXT[c.type] || c.type || '教室';
        c.updatedText = util.formatTime(c.updatedContentAt || c.updatedAt);
        this.setData({ c, loading: false });
        wx.setNavigationBarTitle({ title: (c.buildingName || '') + (c.roomNo || c.name || '教室') });
        this.loadImages();
        this.loadSocketMarks();
        this.loadReviews();
        this.loadLatestStatus();
        this.loadGuide();
      })
      .catch((err) => {
        if (err && err.code === 404) {
          this.setData({ loading: false, notFound: true, showApply: true });
          this.loadApplyBuildings();
        } else {
          this.setData({ loading: false, error: true });
        }
      });
  },

  /* ---------- 照片 ---------- */
  loadImages() {
    api.classrooms.images(this.data.id).then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []).map(im => {
        return Object.assign({}, im, { url: util.imgUrl(im) });
      }).filter(im => im.url);
      this.setData({ allImages: list });
      this.applyImageFilter();
    }).catch(() => this.setData({ allImages: [] }));
  },

  applyImageFilter() {
    const cat = this.data.activeCat;
    let shown = this.data.allImages;
    if (cat) {
      shown = this.data.allImages.filter(im => {
        if (cat === 'OTHER') return ['OTHER', 'PC', 'WHITEBOARD'].indexOf(im.category) !== -1;
        return im.category === cat;
      });
    }
    const urls = shown.map(im => im.url);
    this.setData({ shownImages: shown, previewUrls: urls });
  },

  onCatTap(e) {
    this.setData({ activeCat: e.currentTarget.dataset.cat });
    this.applyImageFilter();
  },

  previewImage(e) {
    const idx = e.currentTarget.dataset.index;
    if (!this.data.previewUrls.length) return;
    wx.previewImage({
      current: this.data.previewUrls[idx] || this.data.previewUrls[0],
      urls: this.data.previewUrls
    });
  },

  /* ---------- 插座标记 ---------- */
  loadSocketMarks() {
    api.classrooms.socketMarks(this.data.id).then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []);
      const imgMap = {};
      this.data.allImages.forEach(im => { imgMap[im.id] = im.url; });
      const onImages = [], noImage = [];
      list.forEach(m => {
        m.wallText = util.WALL_POS_TEXT[m.wallPos] || m.wallPos;
        m.accuracyRate = this.markRate(m);
        if (m.imageId && imgMap[m.imageId]) {
          m.imageUrl = imgMap[m.imageId];
          onImages.push(m);
        } else {
          noImage.push(m);
        }
      });
      this.setData({ socketMarks: list, marksOnImages: onImages, marksNoImage: noImage });
    }).catch(() => this.setData({ socketMarks: [] }));
  },

  markRate(m) {
    const up = m.accuracyUp || 0, down = m.accuracyDown || 0;
    if (up + down === 0) return '待验证';
    return Math.round((up / (up + down)) * 100) + '%';
  },

  // 点按圆点反馈准确/不准
  onMarkTap(e) {
    const mark = e.currentTarget.dataset.mark;
    if (!mark || !req.getToken()) {
      if (!req.getToken()) wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.showActionSheet({
      itemList: ['👍 位置准确', '👎 位置不准'],
      success: (res) => {
        api.classrooms.socketMarkFeedback(mark.id, res.tapIndex === 0)
          .then(() => {
            wx.showToast({ title: '感谢反馈', icon: 'success' });
            this.loadSocketMarks();
          })
          .catch(() => {});
      }
    });
  },

  /* ---------- 评价 ---------- */
  loadReviews() {
    api.classrooms.reviews(this.data.id, { page: 1, size: 10 }).then((data) => {
      const list = (data && data.list) || (Array.isArray(data) ? data : []);
      list.forEach(r => { r.timeText = util.formatTime(r.createdAt, false); });
      this.setData({ reviews: list });
    }).catch(() => this.setData({ reviews: [] }));
  },

  onRatingTap(e) { this.setData({ reviewRating: Number(e.currentTarget.dataset.v) }); },
  onReviewInput(e) { this.setData({ reviewContent: e.detail.value }); },

  submitReview() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    const content = this.data.reviewContent.trim();
    if (!content) return wx.showToast({ title: '请填写评价内容', icon: 'none' });
    this.setData({ submittingReview: true });
    api.classrooms.addReview(this.data.id, { rating: this.data.reviewRating, content })
      .then(() => {
        wx.showToast({ title: '评价已提交', icon: 'success' });
        this.setData({ reviewContent: '', reviewRating: 5, submittingReview: false });
        this.loadReviews();
      })
      .catch((err) => this.setData({ submittingReview: false }));
  },

  /* ---------- 临时状态 ---------- */
  loadLatestStatus() {
    api.classrooms.latestStatus(this.data.id).then((s) => {
      if (!s || !s.expiresAt) return this.setData({ latestStatus: null });
      s.timeText = util.formatTime(s.createdAt);
      s.occText = util.OCCUPANCY_TEXT[s.occupancy] || s.occupancy;
      s.expireText = util.expireDesc(s.expiresAt);
      this.setData({ latestStatus: s, statusExpireText: s.expireText });
    }).catch(() => this.setData({ latestStatus: null }));
  },

  /* ---------- 文字导航 ---------- */
  loadGuide() {
    const buildingId = this.data.c && this.data.c.buildingId;
    if (!buildingId) return;
    api.buildings.guide(buildingId).then((g) => {
      const text = typeof g === 'string' ? g : (g && (g.guide || g.text || g.content)) || '';
      this.setData({ guideText: text || '暂无导航信息' });
    }).catch(() => this.setData({ guideText: '暂无导航信息' }));
  },

  toggleGuide() { this.setData({ showGuide: !this.data.showGuide }); },

  /* ---------- 收藏 ---------- */
  checkFavorite() {
    api.favorites.list('CLASSROOM').then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []);
      const hit = list.some(f => f.targetType === 'CLASSROOM' && String(f.targetId) === String(this.data.id));
      this.setData({ isFavorite: hit });
    }).catch(() => {});
  },

  toggleFavorite() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    if (this.data.isFavorite) {
      api.favorites.remove('CLASSROOM', this.data.id).then(() => {
        this.setData({ isFavorite: false });
        wx.showToast({ title: '已取消收藏', icon: 'none' });
      }).catch(() => {});
    } else {
      api.favorites.add('CLASSROOM', this.data.id).then(() => {
        this.setData({ isFavorite: true });
        wx.showToast({ title: '已收藏', icon: 'success' });
      }).catch(() => {});
    }
  },

  /* ---------- 我要更新资料 ---------- */
  openReport() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    this.setData({ showReport: true, reportIndex: 0, reportBoolValue: 'true', reportValue: '', reportRemark: '' });
  },
  closeReport() { this.setData({ showReport: false }); },
  onReportField(e) { this.setData({ reportIndex: Number(e.detail.value), reportValue: '' }); },
  onReportBool(e) { this.setData({ reportBoolValue: e.detail.value }); },
  onReportValue(e) { this.setData({ reportValue: e.detail.value }); },
  onReportRemark(e) { this.setData({ reportRemark: e.detail.value }); },

  submitReport() {
    const f = this.data.reportFields[this.data.reportIndex];
    let newValue;
    if (f.type === 'bool') {
      newValue = this.data.reportBoolValue === 'true' ? '1' : '0';
    } else {
      newValue = this.data.reportValue.trim();
    }
    if (!newValue && newValue !== '0') {
      return wx.showToast({ title: '请填写修正后的内容', icon: 'none' });
    }
    api.classrooms.addEquipmentReport(this.data.id, {
      fieldName: f.key,
      newValue: String(newValue),
      remark: this.data.reportRemark.trim()
    }).then(() => {
      wx.showModal({
        title: '提交成功',
        content: '资料修正已进入审核，审核通过后展示给全校同学。',
        showCancel: false
      });
      this.setData({ showReport: false });
    }).catch(() => {});
  },

  /* ---------- 上传照片 ---------- */
  openUpload() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    this.setData({ showUpload: true, uploadCatIndex: 0, uploadDesc: '', uploadPreview: '' });
  },
  closeUpload() { this.setData({ showUpload: false }); },
  onUploadCat(e) { this.setData({ uploadCatIndex: Number(e.detail.value) }); },
  onUploadDesc(e) { this.setData({ uploadDesc: e.detail.value }); },

  chooseUploadImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const path = res.tempFiles[0].tempFilePath;
        this.setData({ uploadPreview: path });
        this.setData({ uploading: true });
        api.uploadImageBase64(path)
          .then((data) => {
            const ossKey = data && data.ossKey;
            if (!ossKey) throw new Error('上传返回缺少 ossKey');
            const cat = IMAGE_CATEGORIES[this.data.uploadCatIndex].value;
            return api.classrooms.addImage(this.data.id, {
              ossKey,
              category: cat,
              description: this.data.uploadDesc.trim()
            }).then(() => ossKey);
          })
          .then(() => {
            this.setData({ uploading: false });
            wx.showModal({
              title: '上传成功',
              content: '照片需审核通过后才会公开展示。',
              showCancel: false
            });
            this.loadImages();
          })
          .catch((err) => {
            this.setData({ uploading: false });
            util.toastError(err, '上传失败');
          });
      }
    });
  },

  /* ---------- 标记插座 ---------- */
  openMark() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    if (!this.data.allImages.length) {
      return wx.showToast({ title: '该教室还没有照片，请先上传照片', icon: 'none' });
    }
    this.setData({
      showMark: true,
      markImages: this.data.allImages,
      markImageIndex: 0,
      markPoints: [],
      markWallIndex: 0,
      markNote: ''
    });
    this.queryMarkRect();
  },
  closeMark() { this.setData({ showMark: false }); },
  onMarkImage(e) {
    this.setData({ markImageIndex: Number(e.detail.value), markPoints: [] });
    setTimeout(() => this.queryMarkRect(), 200);
  },
  onMarkNote(e) { this.setData({ markNote: e.detail.value }); },
  onMarkWall(e) { this.setData({ markWallIndex: Number(e.detail.value) }); },

  queryMarkRect() {
    wx.createSelectorQuery().in(this)
      .select('#mark-image')
      .boundingClientRect((rect) => {
        this.setData({ markRect: rect });
      })
      .exec();
  },

  onMarkImageTap(e) {
    const rect = this.data.markRect;
    if (!rect) return;
    const px = e.detail.x, py = e.detail.y;
    const posX = Math.max(0, Math.min(100, Math.round(((px - rect.left) / rect.width) * 100)));
    const posY = Math.max(0, Math.min(100, Math.round(((py - rect.top) / rect.height) * 100)));
    this.setData({ markPoints: [{ posX, posY }] });
  },

  submitMark() {
    const p = this.data.markPoints[0];
    if (!p) return wx.showToast({ title: '请先在照片上点击插座位置', icon: 'none' });
    const img = this.data.markImages[this.data.markImageIndex];
    const wall = WALL_POS[this.data.markWallIndex];
    api.classrooms.addSocketMark(this.data.id, {
      imageId: img && img.id,
      posX: p.posX,
      posY: p.posY,
      wallPos: wall.value,
      note: this.data.markNote.trim()
    }).then(() => {
      wx.showToast({ title: '标记已提交', icon: 'success' });
      this.setData({ showMark: false });
      this.loadSocketMarks();
    }).catch(() => {});
  },

  /* ---------- 临时状态上报 ---------- */
  openStatus() {
    if (!req.getToken()) return wx.navigateTo({ url: '/pages/login/login' });
    this.setData({ showStatus: true, statusOccIndex: 0, statusQuiet: 3, statusAcIndex: 2, statusSocketIndex: 2, statusReason: '', statusHours: 4 });
  },
  closeStatus() { this.setData({ showStatus: false }); },
  onStatusOcc(e) { this.setData({ statusOccIndex: Number(e.detail.value) }); },
  onStatusQuiet(e) { this.setData({ statusQuiet: Number(e.detail.value) }); },
  onStatusAc(e) { this.setData({ statusAcIndex: Number(e.detail.value) }); },
  onStatusSocket(e) { this.setData({ statusSocketIndex: Number(e.detail.value) }); },
  onStatusReason(e) { this.setData({ statusReason: e.detail.value }); },
  onStatusHours(e) { this.setData({ statusHours: Number(e.detail.value) || 4 }); },

  submitStatus() {
    const occ = this.data.occupancyOptions[this.data.statusOccIndex].value;
    const toBool = (idx) => idx === 0 ? 1 : (idx === 1 ? 0 : null);
    api.classrooms.addStatus(this.data.id, {
      occupancy: occ,
      quietLevel: this.data.statusQuiet,
      acWorking: toBool(this.data.statusAcIndex),
      socketWorking: toBool(this.data.statusSocketIndex),
      occupiedReason: this.data.statusReason.trim(),
      hoursValid: this.data.statusHours
    }).then(() => {
      wx.showToast({ title: '状态已上报', icon: 'success' });
      this.setData({ showStatus: false });
      this.loadLatestStatus();
    }).catch(() => {});
  },

  /* ---------- 申请新增教室 ---------- */
  loadApplyBuildings() {
    api.buildings.list().then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      this.setData({ applyBuildings: list });
    }).catch(() => {});
  },
  openApply() {
    this.setData({ showApply: true });
    if (!this.data.applyBuildings.length) this.loadApplyBuildings();
  },
  closeApply() { this.setData({ showApply: false }); },
  onApplyBuilding(e) { this.setData({ applyBuildingIndex: Number(e.detail.value) }); },
  onApplyFloor(e) { this.setData({ applyFloorNo: e.detail.value }); },
  onApplyRoom(e) { this.setData({ applyRoomNo: e.detail.value }); },
  onApplyRemark(e) { this.setData({ applyRemark: e.detail.value }); },

  submitApply() {
    const d = this.data;
    if (d.applyBuildingIndex < 0) return wx.showToast({ title: '请选择教学楼', icon: 'none' });
    if (!d.applyRoomNo.trim()) return wx.showToast({ title: '请填写教室编号', icon: 'none' });
    api.classrooms.applyNew({
      buildingId: d.applyBuildings[d.applyBuildingIndex].id,
      floorNo: d.applyFloorNo,
      roomNo: d.applyRoomNo.trim(),
      remark: d.applyRemark.trim()
    }).then(() => {
      wx.showModal({ title: '申请已提交', content: '感谢贡献！审核通过后该教室将收录进系统。', showCancel: false });
      this.setData({ showApply: false });
    }).catch(() => {});
  },

  noop() {},
  retry() { this.loadAll(); }
});
