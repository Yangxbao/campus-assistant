/**
 * 环境配置：集中管理 Base URL、订阅消息模板 ID 等
 *
 * 说明：
 * 1. 真实 AppID 请在 project.config.json 的 "appid" 字段配置
 *    （当前占位为 touristappid，可在微信开发者工具中导入后修改，或使用"游客模式"）。
 * 2. 本文件只存放前端公开配置，绝不硬编码任何密钥。
 *    微信 AppSecret 等敏感信息仅保存在后端服务的 .env 中，小程序端永远接触不到。
 * 3. 开发调试时请保持手机与电脑同一局域网，或将 BASE_URL 指向已部署的后端地址。
 */
module.exports = {
  // 后端接口基地址（见 docs/api-contract.md 第 0 节）
  // 注意：本机 8080 被其他课程项目的 nginx 占用，campus-assistant 后端跑在 18080；
  // 若 8080 空闲可改回 'http://localhost:8080/api'（真机调试改成局域网 IP）
  BASE_URL: 'http://localhost:18080/api',

  // 项目名称
  APP_NAME: '湖工商校园助手',

  // 课程提醒订阅消息模板 ID。
  // 需在微信公众平台 → 订阅消息 中申请模板后填入；
  // 为空数组时，订阅流程会明确提示"订阅消息未配置"，不会假装订阅成功。
  SUBSCRIBE_TEMPLATE_IDS: [],

  // 开发模式提示文案（后端未配置微信 AppSecret 时，登录接口返回 devMode:true）
  DEV_MODE_HINT: '开发模式：后端未配置微信 AppSecret，当前使用虚拟 openid，功能可正常联调'
};
