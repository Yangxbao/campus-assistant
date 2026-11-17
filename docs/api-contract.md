# 湖南工商大学校园助手 —— 接口契约与数据模型（对接基准文档）

> 本文档是小程序端、Java 后端、管理后台三端开发的统一对接基准。
> 所有模块必须严格遵循本文档的路径、字段命名（后端 JSON 一律小驼峰）与语义。
> 项目为「学生社区服务项目」，非学校官方应用，全部演示数据必须标注"演示数据"。

## 0. 全局约定

- Base URL: `http://localhost:8080/api`
- 认证：`Authorization: Bearer <JWT>`。JWT payload 至少含 `uid, openid, role`。
- 角色体系（用户表 role 字段 + admin_users 表独立于小程序用户）：
  - 小程序用户角色：`user`（学生）、也可被赋予 `auditor`（内容审核）、`maintainer`（教室资料维护）。
  - 管理后台角色：`SUPER_ADMIN` / `AUDITOR` / `MAINTAINER`。
- 统一响应包：
  ```json
  { "code": 0, "message": "ok", "data": {} }
  ```
  `code=0` 成功；非 0 失败；`data` 可为 null。分页统一返回：
  ```json
  { "list": [], "total": 100, "page": 1, "size": 10 }
  ```
- 枚举值统一用字符串。审核状态：`PENDING / APPROVED / REJECTED / OFFLINE(下架)`。
- 失物状态：`OPEN(进行中) / MATCHED(待确认) / DONE(已完成) / CLOSED(已撤销) / EXPIRED`。
- 教室资料冲突提交进入 `PENDING` 审核，不直接覆盖已发布数据。
- 所有删除均为逻辑删除（`deleted` 字段 0/1）。
- 时间统一返回 ISO 字符串 `yyyy-MM-dd HH:mm:ss`。

## 1. 数据库表清单（后端负责产出 `sql/schema.sql` + `sql/data_demo.sql`）

1. `t_user`：id, openid(唯一), nickname, avatar_url, phone(可空,加密存), role, status(ACTIVE/BANNED), contribute_count, last_login_time, deleted, created_at, updated_at
2. `t_building`：id, code(楼编号如 J1), name, floors_count, description, campus(校区), location_desc(文字位置说明), cover_image_id, status, deleted, created_at, updated_at
3. `t_floor`：id, building_id, floor_no(1,2,-1), description, deleted
4. `t_classroom`：id, building_id, floor_id, room_no(教室编号如 301), name, type(LINEAR大教室/SMALL小教室/LAB实验室/MULTIMEDIA多媒体/OTHER), seat_count(可空), has_ac, socket_desc(文字描述插座位置), socket_count(可空), has_projector, has_pc, has_mic, has_whiteboard, has_wifi, other_devices, env_score_quiet(1-5可空), env_desc(环境描述), desk_desc, light_desc, ventilation_desc, clean_desc, status(APPROVED...), latest_contributor_id, updated_content_at, deleted, created_at, updated_at；索引 (building_id, floor_id, room_no)
5. `t_classroom_equipment_report`（教室设备反馈/修正提交）：id, classroom_id, user_id, field_name, new_value, remark, audit_status, auditor_id, audit_remark, audit_time, deleted, created_at
6. `t_classroom_image`：id, classroom_id, user_id, oss_key, category(OVERALL整体/FRONT前方/BACK后方/SOCKET插座/AC空调/PROJECTOR投影/PC电脑/WHITEBOARD白板/OTHER), description, audit_status, deleted, created_at；索引 classroom_id
7. `t_socket_mark`（插座位置标记）：id, classroom_id, user_id, image_id(可空,标记在某照片上), pos_x(0-100百分比), pos_y(0-100), wall_pos(FRONT前/BACK后/LEFT/RIGHT/NEAR_PODIUM讲台/WINDOW窗边), accuracy_up(点赞数), accuracy_down(不准数), audit_status, deleted, created_at
8. `t_classroom_review`（教室评价/准确性反馈）：id, classroom_id, user_id, rating(1-5), content, audit_status, deleted, created_at
9. `t_room_status`（临时教室状态，社区反馈非官方）：id, classroom_id, user_id, occupancy(FREE空闲/PARTLY部分占用/FULL占用/UNKNOWN), quiet_level(1-5), ac_working(0/1/null), socket_working(0/1/null), occupied_reason(活动占用原因), expires_at(有效期), audit_status, deleted, created_at
10. `t_semester`：id, name, start_date(周一), total_weeks, is_current(0/1), deleted
11. `t_period`（节次配置）：id, semester_id, period_no, start_time, end_time, deleted
12. `t_course`（课程记录）：id, user_id, semester_id, course_name, teacher, weekday(1-7), start_period, end_period, week_type(ALL每周/ODD单周/EVEN双周), weeks(json数组如"[1,2,3,15,16]"), building_id(可空), classroom_id(可空), room_raw_text(原始教室文本,教室未收录时用), remark, deleted, created_at, updated_at；索引 (user_id, semester_id)
13. `t_lost_item`：id, user_id, kind(LOST我丢了东西/FOUND我捡到了东西), title, category(证件/卡类/电子产品/书籍/钥匙/雨伞/衣物/首饰/现金/其他), description, lost_time(发生时间), building_id(可空), location_desc, contact_visible(0/1 联系方式是否公开), contact_value(联系方式,默认不公开), status(OPEN/MATCHED/DONE/CLOSED/EXPIRED), expire_at, view_count, deleted, created_at, updated_at；索引 (kind, category, status)
14. `t_item_image`：id, lost_item_id, oss_key, deleted
15. `t_claim`（认领记录）：id, lost_item_id, claimant_id, proof_desc(物品特征证明), contact, status(PENDING/ACCEPTED/REJECTED/CANCELLED), handler_id, handle_time, handle_remark, deleted, created_at
16. `t_message`（站内消息）：id, user_id(接收者), type(SYSTEM/LOST_CONTACT/CLAIM/MATCH/AUDIT/REMIND), title, content, related_type, related_id, is_read(0/1), deleted, created_at；索引 (user_id, is_read)
17. `t_favorite`：id, user_id, target_type(CLASSROOM/LOST_ITEM), target_id, deleted, created_at；唯一索引 (user_id,target_type,target_id)
18. `t_report`（举报）：id, reporter_id, target_type(CLASSROOM_IMAGE/LOST_ITEM/REVIEW/ROOM_STATUS/USER), target_id, reason_type(PORN/AD/PRIVACY/FALSE/OTHER), reason_desc, status(PENDING/HANDLED/REJECTED), handler_id, handle_result, deleted, created_at
19. `t_audit_record`（内容审核记录）：id, target_type, target_id, auditor_id, action(APPROVE/REJECT/OFFLINE/DELETE), remark, created_at
20. `t_op_log`（操作日志）：id, actor_type(USER/ADMIN), actor_id, module, action, detail(json文本), ip, created_at
21. `t_sys_config`：id, config_key(唯一), config_value, remark, updated_at
22. `t_ocr_task`：id, user_id, oss_key, status(UPLOADED/PROCESSING/DONE/FAILED), result_json(OCR结构化识别结果), error_msg, deleted, created_at
23. `t_wx_user_session`：id, user_id, session_key(可空), last_active, deleted（可选，若使用 JWT 无状态可省，但保留结构）
24. `t_admin_user`：id, username(唯一), password(bcrypt), real_name, role(SUPER_ADMIN/AUDITOR/MAINTAINER), status, deleted, created_at, last_login_time
25. `t_building_qr`：id, target_type(BUILDING/CLASSROOM), target_id, qr_token(唯一随机码,用于小程序码扫码跳转), created_at

## 2. 小程序端接口（/api/mp/**，需 JWT）

### 2.1 认证与用户
- `POST /mp/auth/login` body `{ code }` —— 微信 code 登录。后端若未配置微信 AppSecret，走**开发模式**：按 code 确定性生成虚拟 openid（如 `dev-{md5(code)前12位}`）并明示 `devMode:true`；若已配置则真实调用 `https://api.weixin.qq.com/sns/jscode2session`。返回 `{ token, userInfo, devMode }`。
- `GET /mp/user/me` / `PUT /mp/user/me`（昵称、头像、隐私设置 phone 可见性）
- `GET /mp/user/me/contributions`（我的教室资料贡献、照片、反馈记录，分页）
- `DELETE /mp/user/me`（注销，逻辑删除）

### 2.2 楼宇与教室
- `GET /mp/buildings`（列表，含楼层数、教室数）
- `GET /mp/buildings/{id}`（详情 + 按楼层分组的教室列表）
- `GET /mp/classrooms/search` query：`keyword, buildingId, floorNo, roomNo, type, hasAc, hasSocket(minCount), hasProjector, hasPc, hasMic, hasWhiteboard, hasWifi, minQuiet, suitableForSelfStudy(有插座+安静>=4), page, size, sort(updated_desc/review_desc)` —— 多条件组合筛选
- `GET /mp/classrooms/{id}`（详情：基础信息、设备、审核通过的照片、插座标记汇总、最新状态反馈(未过期)、最近评价、更新时间）
- `GET /mp/classrooms/{id}/images` / `POST /mp/classrooms/{id}/images`（body: `{ ossKey, category, description }`，提交后进审核）
- `POST /mp/classrooms/{id}/socket-marks`（`{ imageId?, posX, posY, wallPos, note }`）
- `GET /mp/classrooms/{id}/socket-marks`
- `POST /mp/socket-marks/{id}/feedback`（`{ accurate: true/false }` 汇总准确率）
- `POST /mp/classrooms/{id}/equipment-reports`（设备修正/损坏反馈，进审核；同字段多人冲突时并排存两条 PENDING，由审核员裁定）
- `POST /mp/classrooms/{id}/reviews`
- `GET /mp/classrooms/{id}/reviews`
- `POST /mp/classrooms/{id}/status`（临时现场状态，`{ occupancy, quietLevel, acWorking, socketWorking, occupiedReason, hoursValid }`，默认 4 小时有效）
- `GET /mp/classrooms/{id}/status/latest`
- `GET /mp/classrooms/recent`（我最近浏览的教室，基于 Redis zset 记录）
- `GET /mp/classrooms/latest-updated`（首页：最新更新的教室）
- `POST /mp/classrooms/apply-new`（教室未收录时申请新增：`{ buildingId, floorNo, roomNo, remark }`，生成 PENDING 记录）
- `GET /mp/buildings/{id}/guide`（文字导航：从楼入口→楼层→教室的文字指引，由楼层 description + 教室信息拼装）
- `GET /mp/qr/{token}`（扫码进入：qr_token → 返回目标教室/楼宇 id）

### 2.3 课程表
- `GET /mp/timetable/semesters` / `POST /mp/timetable/semesters`(仅管理员或系统配置)
- `GET /mp/timetable/periods?semesterId=`
- `GET /mp/timetable/courses?semesterId=&week=`（返回该用户课表；week 可选，用于计算单双周/周次过滤）
- `POST /mp/timetable/courses` body: `{ semesterId, courseName, teacher, weekday, startPeriod, endPeriod, weekType, weeks[], buildingId?, classroomId?, roomRawText, remark }` —— 允许同时间段不同周并存（不查冲突拒绝，前端提示）
- `PUT /mp/timetable/courses/{id}` / `DELETE /mp/timetable/courses/{id}`（仅本人）
- `GET /mp/timetable/today?semesterId=&week=`（今日课程 + 下一节课 + 剩余时间）
- `POST /mp/timetable/ocr` —— 接收已上传图片的 ossKey，创建 OCR 任务。OCR 服务不可用时返回 `code=50301, message:"OCR 服务未配置，请手动录入"`，**不得伪造识别结果**。
- `GET /mp/timetable/ocr/{taskId}`（轮询结果，result_json 为结构化课程数组：`[{courseName, weekday, startPeriod, endPeriod, weekType, weeksText, roomRawText, teacher, confidence}]`）
- `POST /mp/timetable/ocr/{taskId}/confirm` body: `{ courses:[...校正后的课程数组] }` —— 用户在校正页核对修改后才正式保存
- `GET /mp/timetable/weeks-current`（当前教学周，由 semester.start_date 计算）

### 2.4 失物招领
- `POST /mp/lost-items`（kind/title/category/description/lostTime/buildingId/locationDesc/ossKeys[]/contactVisible/contactValue）—— 首图必须审核通过后公开展示（简化：先 PENDING，管理员审核通过后可被检索；本人可在"我的"里看到）
- `GET /mp/lost-items` query: `kind, category, buildingId, keyword, sort(time_desc), status, page, size`
- `GET /mp/lost-items/{id}`（浏览量+1；联系方式仅发布者/管理员可见，公开时也做脱敏提示）
- `PUT /mp/lost-items/{id}`（仅本人）/ `DELETE /mp/lost-items/{id}`（逻辑删除）
- `PUT /mp/lost-items/{id}/status`（本人标记 DONE/CLOSED）
- `POST /mp/lost-items/{id}/claims`（认领申请：proof_desc + contact）
- `GET /mp/lost-items/{id}/claims`（仅发布者可见）
- `PUT /mp/claims/{id}/handle`（发布者接受/拒绝；接受后可联动消息）
- `GET /mp/lost-items/{id}/matches`（简单自动匹配：对 LOST↔FOUND 同 category、标题/描述关键词重合、地点相近、时间窗 30 天内的对向信息返回提示列表 + 匹配理由；纯规则实现，不冒充 AI）
- `GET /mp/lost-items/match-feeds`（首页/消息：与我发布的方向相反的疑似匹配）

### 2.5 收藏、消息、举报
- `POST /mp/favorites` `{ targetType, targetId }` / `DELETE /mp/favorites` / `GET /mp/favorites?type=`
- `GET /mp/messages?page=` / `PUT /mp/messages/{id}/read` / `PUT /mp/messages/read-all` / `GET /mp/messages/unread-count`
- `POST /mp/reports` `{ targetType, targetId, reasonType, reasonDesc }`
- `GET /mp/announcements`（公告，来自 sys_config 或独立公告接口，首页展示）

### 2.6 上传
- `POST /mp/upload/image` multipart 或 `POST /mp/upload/image-base64`（小程序开发工具可用 base64）→ 后端校验格式(jpg/png/webp)/大小(≤8MB)/EXIF 清洗，本地存储到 `upload-dir`，返回 `{ ossKey, url }`。生产切换 MinIO/OSS 只需实现 `StorageService` 接口。

### 2.7 提醒
- `POST /mp/timetable/remind-config` `{ enabled, minutesBefore }`（存用户配置）
- 订阅消息：后端提供 `POST /mp/timetable/remind/subscribe-record`（记录用户同意订阅的模板 ID），未配置微信模板时接口可用但推送标记 `notConfigured`，**不假装已推送**。

## 3. 管理后台接口（/api/admin/**，JWT + 角色校验）

- `POST /admin/auth/login` `{ username, password }` → `{ token, role, realName }`
- 教室管理（SUPER_ADMIN/MAINTAINER）：
  - `GET/POST/PUT/DELETE /admin/buildings`（含 floors）
  - `GET/POST/PUT/DELETE /admin/classrooms`（支持按 building/floor/roomNo/类型筛选分页）
  - `POST /admin/classrooms/import`（上传 xlsx/csv，表头模板见 docs/教室批量导入模板.csv）→ 返回成功/失败行明细
  - `GET /admin/classrooms/export`（导出 xlsx）
  - `GET /admin/classrooms/{id}/qrcode`（返回教室二维码 PNG base64，内容为 `https://<domain>/qr/{token}` 跳转 token；后端用 zxing 生成）
  - 照片管理（SUPER_ADMIN/MAINTAINER，2026-11-16 新增）：
    - `GET /admin/classrooms/{id}/images` → 全部审核状态照片列表 `[{ id, ossKey, url, category, description, auditStatus, createdAt }]`
    - `POST /admin/classrooms/{id}/images`（multipart：file + 可选 category=OVERALL/FRONT/BACK/SOCKET/AC/PROJECTOR/PC/WHITEBOARD/OTHER、description）→ 管理员上传直接发布（audit_status=APPROVED，不走审核流），返回 `{ id, ossKey, url }`
    - `DELETE /admin/classrooms/images/{imageId}` → 逻辑删除
  - 小程序用户 `GET /mp/user/me` 返回新增 `avatarUrl` 字段（用户可在「我的」页点击头像上传更换）
- 审核（AUDITOR/SUPER_ADMIN）：
  - `GET /admin/audits?targetType=&status=PENDING&page=`（待审：照片/资料修正/失物/举报/教室状态）
  - `PUT /admin/audits/handle` `{ targetType, targetId, action: APPROVE/REJECT/OFFLINE/DELETE, remark }`（写 t_audit_record + 发站内消息给提交者）
- 用户管理：`GET /admin/users?page=&keyword=&status=`、`PUT /admin/users/{id}/status`（封禁/解封）、`GET /admin/users/{id}`（详情+违规记录）
- 统计：`GET /admin/stats/overview`（用户数/楼宇数/教室数/照片数/失物数/找回数）、`GET /admin/stats/daily?days=30`（每日新增教室资料、失物发布、审核量）、`GET /admin/stats/contributors`（贡献排行）
- 系统配置：`GET/PUT /admin/configs`（学期、节次、上传限制、过期时间、OCR、存储、微信配置的 key-value）
- 举报处理：`GET /admin/reports?status=`、`PUT /admin/reports/{id}/handle`

## 4. Redis 使用约定（可降级）

- `room:recent:{uid}` zset：最近浏览教室
- `rate:upload:{uid}`、`rate:publish:{uid}`：限流（每分钟/每日上限）
- `captcha:*`、后台登录失败计数
- 缓存教室详情 5 分钟（key 带版本号，资料更新时失效）
- Redis 不可用时后端自动降级为本地缓存/直接落库，不阻断主流程。

## 5. OCR 服务约定

- 后端定义 `OcrService` 接口：`OcrResult recognize(byte[] image)`。
- 实现 1：`TesseractOcrService`（可选拓扑，需 tesseract 环境）；实现 2：`HttpOcrService`（调用配置的第三方 OCR HTTP 接口）；未配置时 `NoopOcrService` 返回 50301。
- 课表图片解析：OCR 输出文本后，由后端 `TimetableParser` 做表格结构解析（按行列坐标或分隔重建：课程名/星期/节次/周数/单双周/教室/教师），解析失败的单元格置 confidence=0 并返回原始文本，由用户在校正页修正。OCR 服务未部署时，整条链路返回明确的"OCR 未配置"提示。

## 6. 端口与启动

- 后端：8080；管理后台 dev：5173（proxy /api → 8080）；MySQL：3306，库名 `campus_assistant`，账号 root/见 .env.example；Redis：6379。
- 小程序：导入 `miniprogram/` 目录，AppID 在 `miniprogram/config/env.js` 配置，未配置时提示"游客/开发模式"。
