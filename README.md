<div align="center">

# 🎓 湖工商校园助手 · Campus Assistant

**面向在校学生的校园社区服务小程序 · 三端全栈项目**

微信小程序（原生 JS）+ Spring Boot 3 后端 + Vue3 管理后台

`教室资料共建` `插座位置地图` `课表 OCR` `失物招领` `自习状态分享`

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-blue)](https://baomidou.com/)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](./LICENSE)

> ⚠️ 本项目为**学生社区项目**，非学校官方应用，未获得学校授权，不使用任何学校官方标识。数据库中的楼宇/教室信息均为**演示数据**。

</div>

---

## ✨ 为什么做这个项目

大学教室"去哪自习"全靠口口相传：哪间教室有插座？空调坏没坏？现在有没有人？这些信息散落在各个群里，第二天就过期了。

**Campus Assistant 把这些信息变成人人可查、人人可贡献的结构化数据**——学生上传照片、标记插座、反馈设备状况，经审核后沉淀为全校可用的教室档案；课表还能直接关联教室，"下一节课去哪"点一下就知道。

## 🧩 三端功能总览

### 📱 微信小程序（原生 JS · 18 页面 · 零 npm 依赖）

| 模块 | 能力 |
|---|---|
| 🏫 教室查询 | 楼宇/楼层/编号/**多条件设备筛选**（空调·插座数·投影·电脑·话筒·白板·WiFi·安静度·适合自习），分页 + 关键词 |
| 📷 教室详情 | 设备清单、照片分类展示、**照片上标记插座位置（坐标 0-100%）**、准确性投票、临时现场状态（含有效期，标注社区反馈非官方）、评价、文字导航 |
| 📅 个人课表 | 周视图网格、单双周/周数过滤、手动增删改、**点击课程一键跳转对应教室**、教室未收录时引导申请 |
| 🔍 课表 OCR | 拍照上传 → OCR 任务轮询 → **可视化校正页逐条修改** → 确认后才保存（未配置 OCR 时明确提示，绝不伪造识别结果） |
| 🧳 失物招领 | 丢失/捡到双视图、类别·楼宇筛选、多图发布、联系方式可见开关、**规则匹配引擎**（类别+关键词+地点+30 天窗口）、认领申请流转 |
| 👤 个人中心 | 收藏、贡献记录、消息（未读红点）、隐私设置（手机号 AES 加密存储）、账号注销（逻辑删除） |

### ☕ Java 后端（Spring Boot 3.2 · 17 个 Controller · 88 个源文件）

- **JWT 双轨认证**：小程序用户 token 与管理员 token 分离，`@RequireRole` 注解角色拦截（SUPER_ADMIN / AUDITOR / MAINTAINER）
- **内容审核流**：PENDING → 通过/驳回/下架/删除，写审计表并**自动发站内消息**通知提交者
- **微信登录**：配置 AppSecret 后走真实 `jscode2session`；未配置时 devMode 虚拟 openid（有明确提示）
- **文件上传**：限流（Redis 计数）+ 格式/大小校验 + **EXIF 元数据清洗** + 随机文件名，预留 `StorageService` 接口可切换 MinIO/OSS
- **Redis 多用途**：最近浏览 zset、限流、缓存 —— **Redis 不可用时自动降级，服务不中断**
- **更多**：Excel/CSV 批量导入（返回逐行成败明细）、zxing 教室二维码、OCR 接口族（可接第三方或自建 PaddleOCR）、操作审计 AOP 切面、XSS 转义、统一响应包 + 全局异常处理

### 🖥️ 管理后台（Vue3 + TypeScript · 8 页面）

登录（按角色出菜单）/ 数据看板（概览卡片 + 30 天趋势 + 贡献排行）/ 楼宇管理 / 教室管理（CRUD + 批量导入导出 + 二维码 + **照片管理**）/ 审核中心（5 类待审 + 图片预览）/ 举报处理 / 用户管理（封禁）/ 系统配置（学期节次·上传限制·OCR·微信）

## 🏗️ 架构

```
┌──────────────────┐   HTTPS   ┌────────────────────────────┐
│ 微信小程序(原生JS) │ ────────▶ │  Java 后端 (Spring Boot 3.2) │
└──────────────────┘           │  8080 端口, /api 前缀          │
┌──────────────────┐           │  ├─ MyBatis-Plus ──▶ MySQL 8  │
│ 管理后台(Vue3+TS) │ ────────▶ │  ├─ Redis (缓存/限流/最近浏览) │
└──────────────────┘  /api 代理 │  ├─ 本地文件存储(可切换OSS)     │
                                │  └─ OCR 服务接口(可配置接入)    │
┌──────────────────┐           └────────────────────────────┘
│ 微信开发者工具     │    三端通过《docs/api-contract.md》统一契约对接
│ 导入 miniprogram/ │    后端自带 Swagger 文档：/swagger-ui.html
└──────────────────┘
```

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 小程序 | 原生 JS + WXML/WXSS（**零 npm 依赖**），自带项目自检脚本 `scripts/check.js` |
| 后端 | Spring Boot 3.2 · MyBatis-Plus 3.5 · JJWT · springdoc(Swagger) · zxing · Apache POI · Spring AOP |
| 数据 | MySQL 8.0（25 张表，utf8mb4，逻辑删除）· Redis 7 |
| 管理后台 | Vue 3.5 · TypeScript 5.6 · Vite 6 · Element Plus · Pinia · echarts |
| 工程 | Maven 单模块 · `.env.example` 全环境变量模板 · init-db 一键建库（幂等可重跑） |

## 🚀 快速开始

```bash
# 0. 依赖：JDK17+ / Maven 3.8+ / MySQL 8 / Redis / Node 18+

# 1. 一键建库 + 导表 + 演示数据（幂等，可重复执行）
mysql -uroot -p < sql/schema.sql && mysql -uroot -p campus_assistant < sql/data_demo.sql
# 或使用脚本（支持 MYSQL_HOST/PORT/USER/PASSWORD 环境变量覆盖）
bash scripts/init-db.sh

# 2. 启动后端（默认 8080，全部配置可用环境变量覆盖，见 .env.example）
cd backend && mvn clean package -DskipTests
java -jar target/campus-assistant-backend.jar
# ✅ 验证：http://localhost:8080/api/mp/buildings 返回统一响应包
# 📖 Swagger：http://localhost:8080/swagger-ui.html

# 3. 启动管理后台（Vite 自动把 /api 代理到 8080，可用 VITE_API_TARGET 覆盖）
cd admin && npm install && npm run dev
# ✅ http://localhost:5173  账号 admin / admin123

# 4. 小程序：微信开发者工具导入 miniprogram/（AppID 用测试号或 touristappid）
#    详情 → 本地设置 → 勾选「不校验合法域名」即可联调本机后端
```

> 💡 未配置真实 AppSecret / OCR / 订阅消息时，相关功能走**开发模式并明确提示**，全流程可正常联调，详见《docs/交付报告.md》第六节。

## 🗄️ 数据库设计（25 张表）

- **教室域**：t_building / t_floor / t_classroom / t_classroom_image / t_socket_mark / t_classroom_review / t_room_status / t_classroom_equipment_report / t_building_qr
- **课表域**：t_semester / t_period / t_course / t_ocr_task
- **社区域**：t_lost_item / t_item_image / t_claim / t_message / t_favorite / t_report
- **支撑域**：t_user / t_admin_user / t_wx_user_session / t_audit_record / t_op_log / t_sys_config

设计要点：冲突资料多人并存 PENDING 由审核裁定（**不互相覆盖**）；课程与教室弱关联（classroomId 可空 + roomRawText 兜底）；手机号等敏感字段 AES 加密存储。

## 🔐 安全设计

JWT 认证 + 角色拦截 · 用户仅能改自己的数据 · 输入校验 + XSS 转义 + MyBatis 参数化防注入 · 上传限流 + EXIF 清洗 · 敏感联系方式默认不公开 · 举报/审核/下架/封禁机制齐全 · 重要操作写审计日志（AOP 切面）· 真实密钥一律走环境变量不入库

## 🧪 质量

- **27 个单元测试全绿**：JWT 拦截 7 / 课表解析 6 / 教室筛选 5 / 失物匹配 4 / 周次过滤 5（H2 内存库）
- 小程序自检脚本：18 页面四件套齐全、45 个接口路径全部命中契约
- 管理后台 `vue-tsc` 零类型错误；三端接口实测记录见《docs/交付报告.md》第四节

## 📂 项目结构

```
campus-assistant/
├── miniprogram/        微信小程序（pages/ components/ utils/ config/env.js）
├── backend/            Java 后端
│   └── src/main/java/com/campus/assistant/
│       ├── controller/  mp/ 小程序接口 · admin/ 管理接口
│       ├── service/     业务逻辑
│       ├── mapper/      MyBatis-Plus 数据访问
│       ├── entity/      25 张表实体（按域分组）
│       ├── storage/     存储抽象（本地实现，预留 OSS）
│       └── common/      JWT/拦截器/统一响应/异常/限流/AOP
├── admin/              管理后台（Vue3 + TS + Vite）
├── sql/                schema.sql（25 表）+ data_demo.sql（演示数据）
├── docs/               api-contract.md（三端契约）· 交付报告.md · 批量导入模板
├── scripts/            init-db.sh · start.sh
└── .env.example        环境变量模板（真实密钥绝不硬编码）
```

## 🗺️ Roadmap

- [ ] 接入真实 OCR 服务（PaddleOCR / 第三方 API，接口已就绪）
- [ ] 云对象存储实现（MinIO/OSS，接口已预留）
- [ ] 订阅消息课程提醒（模板配置位已留）
- [ ] 真实校园资料批量导入（模板见 `docs/教室批量导入模板.csv`）

## ⚠️ 免责声明

本项目为个人学习与社区实践用途：非学校官方应用；演示数据不代表真实校园状况；OCR 未配置时返回明确提示而非伪造结果；请勿用于商业用途。

## 🧑‍💻 作者的其他项目

| 项目 | 简介 |
|---|---|
| 🔍 [DevPilot](https://github.com/Yangxbao/DevPilot) | 自托管 AI 研发协作平台：代码库索引 + 语义检索 + 带证据引用的 AI 问答与 Bug 根因分析（Java） |
| 🤖 [AgentOS](https://github.com/Yangxbao/AgentOS) | 本地运行的 AI 数字员工协作系统：公司/岗位 → AI 员工 → 任务派工 → 多 Agent 受控执行（Python） |
| 📚 [docmind](https://github.com/Yangxbao/docmind) | 本地优先的 RAG 文档知识库（Java） |
| 🎯 [OfferDojo](https://github.com/Yangxbao/OfferDojo) | 大学生实战学习平台：220 道模拟任务 + 八股题库 + 学习路线（Java） |
| 🌟 [PetLife](https://github.com/Yangxbao/PetLife) | Android 自律养成电子宠物（Kotlin + Jetpack Compose） |
| 👵 [grandma-home](https://github.com/Yangxbao/grandma-home) | 适老化 Android 桌面：大字界面 · 家人一键通话（Kotlin） |
| 🧹 [windows-file-map](https://github.com/Yangxbao/windows-file-map) | 本机文件地图与一键清理助手：只读盘点 + 白名单安全清理（纯 Python 标准库） |
| 🗺️ [FMap](https://github.com/Yangxbao/FMap) | AI 文件资产地图（Rust · 只读 · 隐私优先） |

> 📫 更多项目见 [我的 GitHub 主页](https://github.com/Yangxbao)

## 📄 License

[MIT](./LICENSE) © 2026 Yangxbao
