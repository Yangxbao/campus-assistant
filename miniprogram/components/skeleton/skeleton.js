/**
 * 骨架屏组件：type=card 列表卡片骨架 / type=detail 详情页骨架 / type=grid 网格骨架
 */
Component({
  properties: {
    type: { type: String, value: 'card' },
    rows: { type: Number, value: 3 }
  }
});
