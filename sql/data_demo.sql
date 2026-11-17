-- =====================================================================
-- 湖南工商大学校园助手 —— 演示数据（data_demo.sql）
-- ★ 演示数据，非真实校园资料 ★
-- 楼宇、楼层、教室、设备、照片均为编造的演示数据，仅用于本地开发联调。
-- 管理员账号：admin / admin123（bcrypt）
-- =====================================================================

SET NAMES utf8mb4;
USE campus_assistant;

-- 管理员（密码 admin123，bcrypt 演示账号）
INSERT INTO `t_admin_user` (`username`,`password`,`real_name`,`role`,`status`)
SELECT 'admin','$2a$10$gtz2mhYIe2eRACrPfbgikOvJlBpB5Mwqo94Yo7i47HwvPRi0m/IdK','演示超级管理员','SUPER_ADMIN','ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `t_admin_user` WHERE `username`='admin');

-- 演示学生用户（openid 为演示虚拟账号）
INSERT INTO `t_user` (`openid`,`nickname`,`role`,`status`)
SELECT 'demo-user-0001','演示同学','user','ACTIVE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_user` WHERE `openid`='demo-user-0001');

-- ==================== 教学楼（演示数据，非真实校园资料） ====================
INSERT INTO `t_building` (`id`,`code`,`name`,`floors_count`,`description`,`campus`,`location_desc`,`status`) VALUES
 (1,'J1','演示一教',5,'演示用教学楼，主要用于公共基础课','主校区','演示校区北门东侧200米（演示数据）','APPROVED'),
 (2,'J2','演示二教',4,'演示用教学楼，多媒体教室为主','主校区','图书馆正对面（演示数据）','APPROVED'),
 (3,'L1','演示实验楼',3,'演示用实验楼，机房与实验室','主校区','演示校区西侧（演示数据）','APPROVED');

INSERT INTO `t_floor` (`id`,`building_id`,`floor_no`,`description`) VALUES
 (1,1,1,'一楼：大厅右侧楼梯上二楼（演示数据）'),
 (2,1,2,'二楼：走中间连廊，教室在连廊两侧（演示数据）'),
 (3,1,3,'三楼：中厅左转直达（演示数据）'),
 (4,2,1,'一楼：进门左手边教室区（演示数据）'),
 (5,2,2,'二楼：电梯右侧走廊（演示数据）'),
 (6,2,3,'三楼：走廊尽头为大教室区（演示数据）'),
 (7,3,1,'一楼：机房入口在楼梯旁（演示数据）'),
 (8,3,2,'二楼：实验室走廊（演示数据）');

-- ==================== 教室（演示数据，非真实校园资料） ====================
INSERT INTO `t_classroom` (`id`,`building_id`,`floor_id`,`room_no`,`name`,`type`,`seat_count`,`has_ac`,`socket_desc`,`socket_count`,`has_projector`,`has_pc`,`has_mic`,`has_whiteboard`,`has_wifi`,`other_devices`,`env_score_quiet`,`env_desc`,`desk_desc`,`light_desc`,`ventilation_desc`,`clean_desc`,`status`,`updated_content_at`) VALUES
 (1,1,1,'101','演示教室101','MULTIMEDIA',120,1,'讲台两侧各一组，教室后墙中部（演示数据）',6,1,0,1,1,1,'演示投影仪',3,'课间人流较大，上课时段安静','固定联排桌椅','灯光充足','两扇窗，通风一般','每日保洁（演示数据）','APPROVED',NOW()),
 (2,1,2,'201','演示教室201','SMALL',45,1,'靠窗一侧每两个座位间一个插座（演示数据）',22,1,0,0,1,1,NULL,5,'小教室，靠窗采光好，适合自习','独立课桌','自然光+顶灯','南北对流，通风好','整洁（演示数据）','APPROVED',NOW()),
 (3,1,2,'202','演示教室202','SMALL',45,1,'讲台附近一组，后排墙面三个（演示数据）',9,0,0,0,1,0,NULL,4,'安静，适合自习（演示数据）','独立课桌','灯光正常','通风一般','较整洁','APPROVED',NOW()),
 (4,1,3,'301','演示教室301','LINEAR',200,1,'墙柱四周均有插座，数量多（演示数据）',30,1,0,1,0,1,'演示音响',3,'大教室，课间嘈杂','联排长桌','灯光充足','通风良好','一般','APPROVED',NOW()),
 (5,1,3,'302','演示教室302','SMALL',40,0,'仅讲台一组插座（演示数据）',2,1,0,0,1,1,NULL,5,'无空调，但安静凉爽，适合自习','独立课桌','灯光正常','窗多通风好','整洁','APPROVED',NOW()),
 (6,2,1,'101','演示多媒体教室','MULTIMEDIA',80,1,'讲台下方成排插座（演示数据）',10,1,1,1,1,1,'演示录播设备',3,'多媒体教室，投影常开','固定桌椅','灯光充足','通风一般','较整洁','APPROVED',NOW()),
 (7,2,2,'201','演示考研自习教室','SMALL',60,1,'每排课桌末端一个插座，后墙一排（演示数据）',24,0,0,0,1,1,NULL,5,'考研自习常用，非常安静','独立带挡板课桌','灯光充足','中央空调通风','每日保洁','APPROVED',NOW()),
 (8,2,2,'202','演示教室202','SMALL',50,1,'窗边立柱处插座（演示数据）',8,1,0,0,0,1,NULL,4,'安静，采光好（演示数据）','独立课桌','自然光充足','通风良好','整洁','APPROVED',NOW()),
 (9,2,3,'301','演示大教室','LINEAR',180,1,'四周立柱插座，讲台旁两排（演示数据）',26,1,0,1,0,1,'演示扩音设备',3,'大教室，自习人多时较吵','联排长桌','灯光充足','通风良好','一般','APPROVED',NOW()),
 (10,3,1,'101','演示机房一','LAB',60,1,'机位桌面下每个座位一个插座（演示数据）',60,1,1,0,0,1,'演示台式机60台',4,'机房环境，键盘声（演示数据）','机房专用桌','灯光充足','空调恒温','整洁','APPROVED',NOW()),
 (11,3,2,'201','演示物理实验室','LAB',40,1,'实验台侧面插座（演示数据）',40,0,0,0,0,0,'演示实验仪器',4,'实验室，需预约使用（演示数据）','实验台','灯光正常','通风柜排风','整洁','APPROVED',NOW()),
 (12,3,2,'202','演示画室','OTHER',35,1,'墙边走线槽插座（演示数据）',12,0,0,0,0,1,'画架若干',4,'画室，氛围安静（演示数据）','画凳+画板','北向自然光','通风良好','颜料易脏','APPROVED',NOW());

-- 教室照片（占位 ossKey，演示数据，非真实图片）
INSERT INTO `t_classroom_image` (`classroom_id`,`user_id`,`oss_key`,`category`,`description`,`audit_status`) VALUES
 (1,1,'demo/classroom-101-overall.jpg','OVERALL','演示占位图：教室101全景（演示数据）','APPROVED'),
 (2,1,'demo/classroom-201-socket.jpg','SOCKET','演示占位图：教室201靠窗插座（演示数据）','APPROVED'),
 (7,1,'demo/classroom-j2-201.jpg','OVERALL','演示占位图：考研自习教室（演示数据）','APPROVED');

-- 插座标记（演示数据）
INSERT INTO `t_socket_mark` (`classroom_id`,`user_id`,`image_id`,`pos_x`,`pos_y`,`wall_pos`,`audit_status`) VALUES
 (2,1,2,85.00,60.00,'WINDOW','APPROVED'),
 (2,1,NULL,10.00,20.00,'NEAR_PODIUM','APPROVED'),
 (7,1,NULL,50.00,90.00,'BACK','APPROVED');

-- 教室评价（演示数据）
INSERT INTO `t_classroom_review` (`classroom_id`,`user_id`,`rating`,`content`,`audit_status`) VALUES
 (2,1,5,'演示评价：插座多光线好，很适合自习（演示数据）','APPROVED'),
 (7,1,5,'演示评价：非常安静，考研党推荐（演示数据）','APPROVED'),
 (1,1,3,'演示评价：课间人多，略吵（演示数据）','APPROVED');

-- 教室临时状态（演示数据，默认4小时有效）
INSERT INTO `t_room_status` (`classroom_id`,`user_id`,`occupancy`,`quiet_level`,`ac_working`,`socket_working`,`occupied_reason`,`expires_at`,`audit_status`) VALUES
 (2,1,'PARTLY',4,1,1,'演示：零散自习约10人（演示数据）',DATE_ADD(NOW(),INTERVAL 4 HOUR),'APPROVED');

-- ==================== 学期与节次（演示数据） ====================
INSERT INTO `t_semester` (`id`,`name`,`start_date`,`total_weeks`,`is_current`) VALUES
 (1,'2026-2027学年第一学期（演示数据）','2026-08-31',20,1);

INSERT INTO `t_period` (`semester_id`,`period_no`,`start_time`,`end_time`) VALUES
 (1,1,'08:00','08:45'),(1,2,'08:55','09:40'),
 (1,3,'10:00','10:45'),(1,4,'10:55','11:40'),
 (1,5,'14:00','14:45'),(1,6,'14:55','15:40'),
 (1,7,'16:00','16:45'),(1,8,'16:55','17:40');

-- ==================== 系统配置（演示数据） ====================
INSERT INTO `t_sys_config` (`config_key`,`config_value`,`remark`) VALUES
 ('announcement','欢迎使用校园助手（演示环境）。本平台为学生社区服务项目，全部资料为演示数据，非学校官方信息。','首页公告'),
 ('semester.current','1','当前学期id'),
 ('upload.maxSizeMb','8','上传大小限制'),
 ('upload.ratePerMinute','30','每分钟上传次数上限'),
 ('lost.expireDays','30','失物默认有效期天数'),
 ('classroom.cacheVersion','1','教室详情缓存版本号'),
 ('remind.defaultMinutesBefore','15','课前提醒默认提前分钟数');

-- 完
