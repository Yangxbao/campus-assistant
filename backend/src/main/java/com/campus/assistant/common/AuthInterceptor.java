package com.campus.assistant.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 【新手导读】JWT 认证拦截器 —— 整个项目"门禁保安"。
 *
 * 它是怎么工作的？
 *   前端每次调接口，都会在请求头带上 "Authorization: Bearer <token>"（token = 登录时发的通行证）。
 *   请求到达 Controller 之前，Spring 会先调用本类的 preHandle() 做"安检"：
 *     1. 从请求头把 token 抠出来；
 *     2. 用 JwtUtil 验证签名和有效期（伪造的、过期的 → 直接拦下返回 401）；
 *     3. 验证通过后，把"这是谁"（用户 id / 管理员 id / 角色）塞进 UserContext（ThreadLocal），
 *        后续任何代码都能通过 UserContext 拿到当前登录人，不用层层传参；
 *     4. 管理端接口还要看角色：Controller 上有 @RequireRole 注解，角色不匹配 → 403。
 *   请求结束后 afterCompletion() 会清掉 ThreadLocal（防止内存泄漏 + 串号）。
 *
 * 两种 token 互不通用：
 *   - /api/mp/**    要求 type=user   的小程序用户 token
 *   - /api/admin/** 要求 type=admin  的管理员 token（并校验 @RequireRole 角色）
 * 认证失败返回业务码 401，角色不足返回业务码 403（HTTP 状态同步返回）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /**
     * preHandle：Controller 方法执行【之前】跑。
     * 返回 true = 放行（继续执行 Controller）；返回 false = 拦截（请求到此为止）。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器跨域时会先发一个 OPTIONS "预检"请求，它不带 token，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 第 1 步：从请求头抠 token（形如 "Bearer eyJhbGci..."，去掉前缀取后半段）
        String token = extractToken(request);
        // 第 2 步：验 token。parse() 内部会验签名 + 验有效期，非法/过期都返回 null
        Claims claims = token == null ? null : jwtUtil.parse(token);
        if (claims == null) {
            // 没带 token 或 token 无效 → 401，请求到此为止
            return reject(response, BizException.UNAUTHORIZED, "未登录或登录已过期");
        }
        // 第 3 步：token 是合法的，从 payload（claims）里读出"这是谁、什么身份"
        // claims 就是登录时 JwtUtil 写进去的那张"名片"：type/uid/openid/role 等
        String type = String.valueOf(claims.get("type", String.class));
        String path = request.getRequestURI();

        if (path.startsWith("/api/admin/")) {
            // ---- 管理端接口：必须拿的是"管理员通行证" ----
            if (!"admin".equals(type)) {
                // 拿用户 token 来调管理接口 → 拒绝
                return reject(response, BizException.UNAUTHORIZED, "需要管理员登录");
            }
            // 把当前管理员信息放进 ThreadLocal 上下文，后面 Controller 用 UserContext.adminId() 就能拿到
            UserContext.Ctx ctx = new UserContext.Ctx();
            ctx.setType("admin");
            ctx.setAdminId(claims.get("adminId", Long.class));
            ctx.setUsername(String.valueOf(claims.getOrDefault("username", "")));
            ctx.setRole(String.valueOf(claims.getOrDefault("role", "")));
            UserContext.set(ctx);

            // 角色校验：方法注解优先，其次类注解
            // 例：AdminClassroomController 类上标了 @RequireRole({"SUPER_ADMIN","MAINTAINER"})，
            //     登录的是 AUDITOR（审核员）→ 调教室管理接口会被这里拦下（403）
            if (handler instanceof HandlerMethod hm) {
                RequireRole rr = hm.getMethodAnnotation(RequireRole.class);
                if (rr == null) {
                    rr = hm.getBeanType().getAnnotation(RequireRole.class);
                }
                if (rr != null && !checkRole(ctx.getRole(), rr.value())) {
                    return reject(response, BizException.FORBIDDEN, "当前角色无权访问该接口");
                }
            }
            return true;
        }

        // ---- 小程序端接口：必须拿的是"用户通行证" ----
        if (!"user".equals(type)) {
            return reject(response, BizException.UNAUTHORIZED, "需要用户登录");
        }
        UserContext.Ctx ctx = new UserContext.Ctx();
        ctx.setType("user");
        ctx.setUid(claims.get("uid", Long.class));
        ctx.setOpenid(String.valueOf(claims.getOrDefault("openid", "")));
        ctx.setRole(String.valueOf(claims.getOrDefault("role", "user")));
        UserContext.set(ctx);
        return true;
    }

    /** 角色是否在允许列表里，比如 role=SUPER_ADMIN，allowed=[SUPER_ADMIN, MAINTAINER] → true */
    private boolean checkRole(String role, String[] allowed) {
        for (String a : allowed) {
            if (a.equals(role)) {
                return true;
            }
        }
        return false;
    }

    /** 从请求头 "Authorization: Bearer xxx" 中抠出 token 本体（去掉 "Bearer " 前缀） */
    private String extractToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }

    /** 拦下请求：直接往响应里写统一格式的 JSON（{code, message}），不再进 Controller */
    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code == BizException.UNAUTHORIZED ? 401 : 403);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                objectMapper.writeValueAsString(ApiResponse.fail(code, msg)).getBytes(StandardCharsets.UTF_8));
        return false;
    }

    /**
     * afterCompletion：整个请求处理完【之后】必跑（无论成功还是报错）。
     * ThreadLocal 是"每个请求线程一份"的存档，用完必须清掉：
     * 否则 Tomcat 的线程是复用的，下一个请求可能读到上一个人的登录信息（串号事故）。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
