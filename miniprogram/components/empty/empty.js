/**
 * 空态组件：空数据 / 加载失败 / 未登录等占位
 */
Component({
  properties: {
    // 展示模式：empty 空数据 / error 加载失败 / offline 网络异常
    mode: {
      type: String,
      value: 'empty'
    },
    text: { type: String, value: '' },
    sub: { type: String, value: '' },
    btnText: { type: String, value: '' }
  },
  data: {
    icon: '',
    defaultText: ''
  },
  observers: {
    mode(m) {
      const map = {
        empty: { icon: '📭', text: '暂无数据' },
        error: { icon: '😵', text: '加载失败' },
        offline: { icon: '📡', text: '网络异常' },
        search: { icon: '🔍', text: '没有找到相关内容' }
      };
      const conf = map[m] || map.empty;
      this.setData({ icon: conf.icon, defaultText: conf.text });
    }
  },
  methods: {
    onBtn() {
      this.triggerEvent('action');
    }
  }
});
