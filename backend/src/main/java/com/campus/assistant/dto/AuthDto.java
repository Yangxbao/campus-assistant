package com.campus.assistant.dto;

import lombok.Data;

/**
 * 认证与用户 DTO。
 */
public class AuthDto {

    @Data
    public static class WxLoginReq {
        private String code;
    }

    @Data
    public static class LoginResp {
        private String token;
        private UserVO userInfo;
        /** 开发模式标识：微信未配置时 true */
        private Boolean devMode;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String nickname;
        private String avatarUrl;
        private String role;
        private Integer contributeCount;
        private String phone;
    }

    @Data
    public static class UpdateMeReq {
        private String nickname;
        private String avatarUrl;
        /** 手机号（加密存储） */
        private String phone;
        /** 手机号是否可见（隐私设置） */
        private Integer phoneVisible;
    }

    @Data
    public static class AdminLoginReq {
        private String username;
        private String password;
    }

    @Data
    public static class AdminLoginResp {
        private String token;
        private String role;
        private String realName;
    }
}
