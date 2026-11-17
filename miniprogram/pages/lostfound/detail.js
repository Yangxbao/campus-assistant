const api = require('../../utils/api.js');
const req = require('../../utils/request.js');
const util = require('../../utils/util.js');

Page({
  data: {
    id: null,
    loading: true,
    error: false,
    item: null,
    isMine: false,
    images: [],

    /* 疑似匹配 */
    matches: [],
    loadingMatches: false,

    /* 认领申请 */
    showClaim: false,
    proofDesc: '',
    claimContact: '',
    submittingClaim: false,

    /* 发布者：认领列表 */
    showClaims: false,
    claims: [],
    loadingClaims: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    this.reload();
  },

  onPullDownRefresh() {
    this.reload();
    wx.stopPullDownRefresh();
  },

  reload() {
    this.setData({ loading: true, error: false });
    api.lostItems.detail(this.data.id).then((data) => {
      const me = getApp().globalData.userInfo || {};
      const isMine = data.isMine === true || (data.userId && data.userId === me.id);
      const images = (data.images || []).map(util.imgUrl).filter(Boolean);
      const matches = (data.matches && data.matches.list) || data.matches || [];
      this.decorateMatches(matches);
      this.setData({
        item: this.decorate(data),
        isMine,
        images,
        loading: false
      });
      if (isMine) this.loadClaims();
      else if (matches.length === 0) this.loadMatches();
    }).catch(() => {
      this.setData({ loading: false, error: true });
    });
  },

  decorate(item) {
    item.kindText = util.KIND_TEXT[item.kind] || item.kind;
    item.statusText = util.LOST_STATUS_TEXT[item.status] || item.status;
    item.timeText = util.formatTime(item.lostTime || item.createdAt);
    // 联系方式：后端按可见性返回，公开时做脱敏展示
    item.contactShown = !!(item.contactValue && (item.contactVisible === 1 || item.contactVisible === true));
    if (item.contactShown) {
      item.contactMasked = util.maskContact(item.contactValue);
    }
    return item;
  },

  decorateMatches(list) {
    (list || []).forEach(m => {
      m.kindText = util.KIND_TEXT[m.kind] || m.kind;
      m.statusText = util.LOST_STATUS_TEXT[m.status] || m.status;
      m.timeText = util.formatTime(m.lostTime || m.createdAt, false);
    });
  },

  /* ---------- 图片预览 ---------- */
  previewImage(e) {
    const current = e.currentTarget.dataset.url;
    wx.previewImage({ current, urls: this.data.images });
  },

  /* ---------- 疑似匹配 ---------- */
  loadMatches() {
    this.setData({ loadingMatches: true });
    api.lostItems.matches(this.data.id).then((data) => {
      const list = (Array.isArray(data) ? data : (data && data.list) || []);
      this.decorateMatches(list);
      this.setData({ matches: list, loadingMatches: false });
    }).catch(() => {
      this.setData({ loadingMatches: false, matches: [] });
    });
  },

  goMatch(e) {
    const id = e.currentTarget.dataset.id;
    if (id) wx.navigateTo({ url: '/pages/lostfound/detail?id=' + id });
  },

  /* ---------- 认领申请 ---------- */
  openClaim() {
    this.setData({ showClaim: true, proofDesc: '', claimContact: '' });
  },
  closeClaim() { this.setData({ showClaim: false, submittingClaim: false }); },
  onProof(e) { this.setData({ proofDesc: e.detail.value }); },
  onClaimContact(e) { this.setData({ claimContact: e.detail.value }); },

  submitClaim() {
    if (this.data.submittingClaim) return;
    if (!this.data.proofDesc.trim()) {
      return wx.showToast({ title: '请描述物品特征作为凭证', icon: 'none' });
    }
    if (!this.data.claimContact.trim()) {
      return wx.showToast({ title: '请留下你的联系方式', icon: 'none' });
    }
    this.setData({ submittingClaim: true });
    api.lostItems.createClaim(this.data.id, {
      proofDesc: this.data.proofDesc.trim(),
      contact: this.data.claimContact.trim()
    }).then(() => {
      this.setData({ showClaim: false, submittingClaim: false });
      wx.showToast({ title: '认领申请已提交', icon: 'success' });
    }).catch((err) => {
      this.setData({ submittingClaim: false });
      util.toastError(err, '提交失败，请重试');
    });
  },

  /* ---------- 发布者：认领列表 ---------- */
  loadClaims() {
    this.setData({ loadingClaims: true });
    api.lostItems.claims(this.data.id).then((data) => {
      const list = Array.isArray(data) ? data : (data && data.list) || [];
      list.forEach(c => {
        c.statusText = util.CLAIM_STATUS_TEXT[c.status] || c.status;
        c.timeText = util.formatTime(c.createdAt);
      });
      this.setData({ claims: list, loadingClaims: false });
    }).catch(() => {
      this.setData({ loadingClaims: false, claims: [] });
    });
  },

  openClaims() {
    this.loadClaims();
    this.setData({ showClaims: true });
  },
  closeClaims() { this.setData({ showClaims: false }); },

  handleClaim(e) {
    const { id, action } = e.currentTarget.dataset; // ACCEPTED / REJECTED
    wx.showModal({
      title: action === 'ACCEPTED' ? '接受认领' : '拒绝认领',
      content: action === 'ACCEPTED'
        ? '接受后将通知认领人与你线下交接，确认接受吗？'
        : '确定拒绝这条认领申请吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.lostItems.handleClaim(id, { action }).then(() => {
          wx.showToast({ title: '已处理', icon: 'success' });
          this.loadClaims();
        }).catch((err) => util.toastError(err, '操作失败'));
      }
    });
  },

  /* ---------- 发布者：状态操作 ---------- */
  markStatus(e) {
    const status = e.currentTarget.dataset.status; // DONE / CLOSED
    const text = status === 'DONE' ? '标记为已完成' : '撤销这条信息';
    wx.showModal({
      title: text,
      content: '确认后状态将更新，其他同学将看到最新状态。',
      success: (res) => {
        if (!res.confirm) return;
        api.lostItems.updateStatus(this.data.id, status).then(() => {
          wx.showToast({ title: '已更新', icon: 'success' });
          this.reload();
        }).catch((err) => util.toastError(err, '操作失败'));
      }
    });
  },

  removeItem() {
    wx.showModal({
      title: '删除信息',
      content: '删除后其他人将看不到这条信息，确定删除吗？',
      confirmColor: '#F25555',
      success: (res) => {
        if (!res.confirm) return;
        api.lostItems.remove(this.data.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' });
          setTimeout(() => wx.navigateBack(), 600);
        }).catch((err) => util.toastError(err, '删除失败'));
      }
    });
  },

  copyContact() {
    const v = this.data.item && this.data.item.contactValue;
    if (!v) return;
    wx.setClipboardData({ data: v });
  },

  retry() { this.reload(); }
});
