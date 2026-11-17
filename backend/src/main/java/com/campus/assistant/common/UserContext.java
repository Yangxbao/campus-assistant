package com.campus.assistant.common;

import lombok.Data;

/**
 * 当前登录上下文（ThreadLocal）。区分小程序用户与管理员两类主体。
 */
public class UserContext {

    @Data
    public static class Ctx {
        private String type;        // user / admin
        private Long uid;           // 用户 id（user）
        private String openid;
        private String role;        // user角色 或 SUPER_ADMIN/AUDITOR/MAINTAINER
        private Long adminId;       // 管理员 id（admin）
        private String username;
    }

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    public static void set(Ctx ctx) {
        HOLDER.set(ctx);
    }

    public static Ctx get() {
        return HOLDER.get();
    }

    /** 获取当前用户 id，未登录抛业务异常 */
    public static long uid() {
        Ctx c = HOLDER.get();
        if (c == null || c.getUid() == null) {
            throw BizException.of(BizException.UNAUTHORIZED, "未登录或登录已过期");
        }
        return c.getUid();
    }

    public static long adminId() {
        Ctx c = HOLDER.get();
        if (c == null || c.getAdminId() == null) {
            throw BizException.of(BizException.UNAUTHORIZED, "未登录或登录已过期");
        }
        return c.getAdminId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
