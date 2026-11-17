-- =====================================================================
-- 湖南工商大学校园助手 —— 数据库建表脚本（schema.sql）
-- 说明：本项目为学生社区服务项目，非学校官方应用。
-- 本文件仅包含表结构定义，不含任何真实校园资料。
-- 字符集：utf8mb4；所有删除均为逻辑删除（deleted 0/1）；
-- 时间字段统一默认 CURRENT_TIMESTAMP / 更新时刷新。
-- =====================================================================

SET NAMES utf8mb4;
USE campus_assistant;

-- 1. 小程序用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid(唯一)',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `phone` VARCHAR(255) DEFAULT NULL COMMENT '手机号(AES加密存储)',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: user/auditor/maintainer',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/BANNED',
  `contribute_count` INT NOT NULL DEFAULT 0 COMMENT '贡献次数',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除0/1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_role_status` (`role`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序用户';

-- 2. 教学楼表
CREATE TABLE IF NOT EXISTS `t_building` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(32) NOT NULL COMMENT '楼编号如J1',
  `name` VARCHAR(128) NOT NULL COMMENT '楼名',
  `floors_count` INT DEFAULT NULL COMMENT '楼层数',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '楼描述',
  `campus` VARCHAR(64) DEFAULT NULL COMMENT '校区',
  `location_desc` VARCHAR(512) DEFAULT NULL COMMENT '文字位置说明',
  `cover_image_id` BIGINT DEFAULT NULL COMMENT '封面图id(t_classroom_image)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学楼';

-- 3. 楼层表
CREATE TABLE IF NOT EXISTS `t_floor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `building_id` BIGINT NOT NULL COMMENT '所属楼',
  `floor_no` INT NOT NULL COMMENT '楼层号,1,2,-1',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '楼层描述(导航用)',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_building_floor` (`building_id`,`floor_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼层';

-- 4. 教室表
CREATE TABLE IF NOT EXISTS `t_classroom` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `building_id` BIGINT NOT NULL,
  `floor_id` BIGINT DEFAULT NULL,
  `room_no` VARCHAR(32) NOT NULL COMMENT '教室编号如301',
  `name` VARCHAR(128) DEFAULT NULL COMMENT '教室名称',
  `type` VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'LINEAR/SMALL/LAB/MULTIMEDIA/OTHER',
  `seat_count` INT DEFAULT NULL,
  `has_ac` TINYINT NOT NULL DEFAULT 0,
  `socket_desc` VARCHAR(512) DEFAULT NULL COMMENT '插座位置文字描述',
  `socket_count` INT DEFAULT NULL,
  `has_projector` TINYINT NOT NULL DEFAULT 0,
  `has_pc` TINYINT NOT NULL DEFAULT 0,
  `has_mic` TINYINT NOT NULL DEFAULT 0,
  `has_whiteboard` TINYINT NOT NULL DEFAULT 0,
  `has_wifi` TINYINT NOT NULL DEFAULT 0,
  `other_devices` VARCHAR(255) DEFAULT NULL,
  `env_score_quiet` INT DEFAULT NULL COMMENT '安静度1-5可空',
  `env_desc` VARCHAR(512) DEFAULT NULL,
  `desk_desc` VARCHAR(255) DEFAULT NULL,
  `light_desc` VARCHAR(255) DEFAULT NULL,
  `ventilation_desc` VARCHAR(255) DEFAULT NULL,
  `clean_desc` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED/OFFLINE',
  `latest_contributor_id` BIGINT DEFAULT NULL,
  `updated_content_at` DATETIME DEFAULT NULL COMMENT '资料最近更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_building_floor_room` (`building_id`,`floor_id`,`room_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室';

-- 5. 教室设备反馈/修正提交表（多人提交同一字段并存多条 PENDING，由审核员裁定）
CREATE TABLE IF NOT EXISTS `t_classroom_equipment_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `classroom_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `field_name` VARCHAR(64) NOT NULL COMMENT '修正字段名',
  `new_value` VARCHAR(255) NOT NULL COMMENT '新值',
  `remark` VARCHAR(255) DEFAULT NULL,
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/OFFLINE',
  `auditor_id` BIGINT DEFAULT NULL,
  `audit_remark` VARCHAR(255) DEFAULT NULL,
  `audit_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_classroom_status` (`classroom_id`,`audit_status`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室设备修正反馈';

-- 6. 教室照片表
CREATE TABLE IF NOT EXISTS `t_classroom_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `classroom_id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `oss_key` VARCHAR(255) NOT NULL,
  `category` VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'OVERALL/FRONT/BACK/SOCKET/AC/PROJECTOR/PC/WHITEBOARD/OTHER',
  `description` VARCHAR(255) DEFAULT NULL,
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_classroom` (`classroom_id`),
  KEY `idx_audit` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室照片';

-- 7. 插座位置标记表
CREATE TABLE IF NOT EXISTS `t_socket_mark` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `classroom_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `image_id` BIGINT DEFAULT NULL COMMENT '标记所在照片id可空',
  `pos_x` DECIMAL(5,2) NOT NULL COMMENT '0-100百分比',
  `pos_y` DECIMAL(5,2) NOT NULL,
  `wall_pos` VARCHAR(20) NOT NULL COMMENT 'FRONT/BACK/LEFT/RIGHT/NEAR_PODIUM/WINDOW',
  `accuracy_up` INT NOT NULL DEFAULT 0,
  `accuracy_down` INT NOT NULL DEFAULT 0,
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_classroom` (`classroom_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插座位置标记';

-- 8. 教室评价表
CREATE TABLE IF NOT EXISTS `t_classroom_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `classroom_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `rating` INT NOT NULL COMMENT '1-5',
  `content` VARCHAR(512) DEFAULT NULL,
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_classroom` (`classroom_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室评价';

-- 9. 临时教室状态表
CREATE TABLE IF NOT EXISTS `t_room_status` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `classroom_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `occupancy` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'FREE/PARTLY/FULL/UNKNOWN',
  `quiet_level` INT DEFAULT NULL COMMENT '1-5',
  `ac_working` TINYINT DEFAULT NULL COMMENT '0/1/null',
  `socket_working` TINYINT DEFAULT NULL,
  `occupied_reason` VARCHAR(255) DEFAULT NULL,
  `expires_at` DATETIME NOT NULL COMMENT '有效期',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_classroom` (`classroom_id`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时教室状态';

-- 10. 学期表
CREATE TABLE IF NOT EXISTS `t_semester` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `start_date` DATE NOT NULL COMMENT '第一周周一',
  `total_weeks` INT NOT NULL DEFAULT 20,
  `is_current` TINYINT NOT NULL DEFAULT 0,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学期';

-- 11. 节次配置表
CREATE TABLE IF NOT EXISTS `t_period` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `semester_id` BIGINT NOT NULL,
  `period_no` INT NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_semester_no` (`semester_id`,`period_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节次配置';

-- 12. 课程表
CREATE TABLE IF NOT EXISTS `t_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `semester_id` BIGINT NOT NULL,
  `course_name` VARCHAR(128) NOT NULL,
  `teacher` VARCHAR(64) DEFAULT NULL,
  `weekday` INT NOT NULL COMMENT '1-7',
  `start_period` INT NOT NULL,
  `end_period` INT NOT NULL,
  `week_type` VARCHAR(10) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/ODD/EVEN',
  `weeks` VARCHAR(255) DEFAULT NULL COMMENT 'json数组如[1,2,3,15,16]',
  `building_id` BIGINT DEFAULT NULL,
  `classroom_id` BIGINT DEFAULT NULL,
  `room_raw_text` VARCHAR(128) DEFAULT NULL COMMENT '原始教室文本',
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_semester` (`user_id`,`semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程记录';

-- 13. 失物招领表（audit_status 为契约外补充字段：发布后先 PENDING，审核通过才可被公开检索）
CREATE TABLE IF NOT EXISTS `t_lost_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `kind` VARCHAR(10) NOT NULL COMMENT 'LOST/FOUND',
  `title` VARCHAR(128) NOT NULL,
  `category` VARCHAR(20) NOT NULL COMMENT '证件/卡类/电子产品/书籍/钥匙/雨伞/衣物/首饰/现金/其他',
  `description` VARCHAR(512) DEFAULT NULL,
  `lost_time` DATETIME DEFAULT NULL COMMENT '发生时间',
  `building_id` BIGINT DEFAULT NULL,
  `location_desc` VARCHAR(255) DEFAULT NULL,
  `contact_visible` TINYINT NOT NULL DEFAULT 0,
  `contact_value` VARCHAR(255) DEFAULT NULL COMMENT '联系方式默认不公开',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/MATCHED/DONE/CLOSED/EXPIRED',
  `expire_at` DATETIME DEFAULT NULL,
  `view_count` INT NOT NULL DEFAULT 0,
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '补充字段:公开检索需APPROVED',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kind_category_status` (`kind`,`category`,`status`),
  KEY `idx_user` (`user_id`),
  KEY `idx_audit` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领';

-- 14. 失物图片表
CREATE TABLE IF NOT EXISTS `t_item_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lost_item_id` BIGINT NOT NULL,
  `oss_key` VARCHAR(255) NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lost_item` (`lost_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物图片';

-- 15. 认领记录表
CREATE TABLE IF NOT EXISTS `t_claim` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lost_item_id` BIGINT NOT NULL,
  `claimant_id` BIGINT NOT NULL,
  `proof_desc` VARCHAR(512) NOT NULL COMMENT '物品特征证明',
  `contact` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/CANCELLED',
  `handler_id` BIGINT DEFAULT NULL,
  `handle_time` DATETIME DEFAULT NULL,
  `handle_remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lost_item` (`lost_item_id`),
  KEY `idx_claimant` (`claimant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认领记录';

-- 16. 站内消息表
CREATE TABLE IF NOT EXISTS `t_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '接收者',
  `type` VARCHAR(20) NOT NULL COMMENT 'SYSTEM/LOST_CONTACT/CLAIM/MATCH/AUDIT/REMIND',
  `title` VARCHAR(128) NOT NULL,
  `content` VARCHAR(512) DEFAULT NULL,
  `related_type` VARCHAR(32) DEFAULT NULL,
  `related_id` BIGINT DEFAULT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息';

-- 17. 收藏表
CREATE TABLE IF NOT EXISTS `t_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'CLASSROOM/LOST_ITEM',
  `target_id` BIGINT NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';

-- 18. 举报表
CREATE TABLE IF NOT EXISTS `t_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `target_type` VARCHAR(32) NOT NULL COMMENT 'CLASSROOM_IMAGE/LOST_ITEM/REVIEW/ROOM_STATUS/USER',
  `target_id` BIGINT NOT NULL,
  `reason_type` VARCHAR(20) NOT NULL COMMENT 'PORN/AD/PRIVACY/FALSE/OTHER',
  `reason_desc` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLED/REJECTED',
  `handler_id` BIGINT DEFAULT NULL,
  `handle_result` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报';

-- 19. 内容审核记录表
CREATE TABLE IF NOT EXISTS `t_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(32) NOT NULL,
  `target_id` BIGINT NOT NULL,
  `auditor_id` BIGINT NOT NULL COMMENT '管理员id',
  `action` VARCHAR(20) NOT NULL COMMENT 'APPROVE/REJECT/OFFLINE/DELETE',
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核记录';

-- 20. 操作日志表
CREATE TABLE IF NOT EXISTS `t_op_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `actor_type` VARCHAR(10) NOT NULL COMMENT 'USER/ADMIN',
  `actor_id` BIGINT DEFAULT NULL,
  `module` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(64) DEFAULT NULL,
  `detail` TEXT DEFAULT NULL COMMENT 'json文本',
  `ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_actor` (`actor_type`,`actor_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- 21. 系统配置表
CREATE TABLE IF NOT EXISTS `t_sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(64) NOT NULL COMMENT '配置键(唯一)',
  `config_value` VARCHAR(1024) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- 22. OCR 任务表
CREATE TABLE IF NOT EXISTS `t_ocr_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `oss_key` VARCHAR(255) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PROCESSING/DONE/FAILED',
  `result_json` TEXT DEFAULT NULL COMMENT 'OCR结构化识别结果',
  `error_msg` VARCHAR(512) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR任务';

-- 23. 微信用户会话表
CREATE TABLE IF NOT EXISTS `t_wx_user_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_key` VARCHAR(128) DEFAULT NULL,
  `last_active` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信用户会话';

-- 24. 管理员表
CREATE TABLE IF NOT EXISTS `t_admin_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL COMMENT 'bcrypt',
  `real_name` VARCHAR(64) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'AUDITOR' COMMENT 'SUPER_ADMIN/AUDITOR/MAINTAINER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员';

-- 25. 楼宇/教室二维码表
CREATE TABLE IF NOT EXISTS `t_building_qr` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'BUILDING/CLASSROOM',
  `target_id` BIGINT NOT NULL,
  `qr_token` VARCHAR(64) NOT NULL COMMENT '唯一随机码,扫码跳转',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qr_token` (`qr_token`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼宇/教室二维码';
