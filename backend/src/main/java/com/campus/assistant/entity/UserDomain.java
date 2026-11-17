package com.campus.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与管理员相关实体（t_user / t_admin_user / t_sys_config / t_wx_user_session）。
 * 所有实体均为逻辑删除（deleted 0/1）。
 */
public class UserDomain {

    @Data
    @TableName("t_user")
    public static class User {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String openid;
        private String nickname;
        private String avatarUrl;
        /** 手机号 AES 加密存储 */
        private String phone;
        /** user / auditor / maintainer */
        private String role;
        /** ACTIVE / BANNED */
        private String status;
        private Integer contributeCount;
        private LocalDateTime lastLoginTime;
        @TableField("deleted")
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }

    @Data
    @TableName("t_admin_user")
    public static class AdminUser {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String username;
        /** bcrypt */
        private String password;
        private String realName;
        /** SUPER_ADMIN / AUDITOR / MAINTAINER */
        private String role;
        private String status;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginTime;
    }

    @Data
    @TableName("t_sys_config")
    public static class SysConfig {
        @TableId(type = IdType.AUTO)
        private Long id;
        private String configKey;
        private String configValue;
        private String remark;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
    }

    @Data
    @TableName("t_wx_user_session")
    public static class WxUserSession {
        @TableId(type = IdType.AUTO)
        private Long id;
        private Long userId;
        private String sessionKey;
        private LocalDateTime lastActive;
        private Integer deleted;
        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createdAt;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updatedAt;
    }
}
