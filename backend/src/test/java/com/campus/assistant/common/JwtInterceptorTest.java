package com.campus.assistant.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT 拦截器测试（契约 0/2/3 节）：
 * - 无 token → HTTP 401 + 业务码 401
 * - 小程序 token 可访问 /api/mp/**
 * - 小程序 token 访问 /api/admin/** → 401（需管理员 token）
 * - 管理员 token 访问 /api/admin/** → 200；角色不足 → 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void 无token访问受保护mp接口返回401业务码() throws Exception {
        mockMvc.perform(get("/api/mp/classrooms/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 无token访问受保护admin接口返回401业务码() throws Exception {
        mockMvc.perform(get("/api/admin/stats/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 非法token返回401() throws Exception {
        mockMvc.perform(get("/api/mp/classrooms/search")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 用户token可访问mp接口() throws Exception {
        String token = jwtUtil.createUserToken(90001L, "dev-test-user", "user");
        mockMvc.perform(get("/api/mp/classrooms/search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 用户token访问admin接口返回401() throws Exception {
        String token = jwtUtil.createUserToken(90001L, "dev-test-user", "user");
        mockMvc.perform(get("/api/admin/stats/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 管理员token可访问admin接口() throws Exception {
        String token = jwtUtil.createAdminToken(1L, "admin", "SUPER_ADMIN");
        mockMvc.perform(get("/api/admin/stats/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void 角色不足返回403() throws Exception {
        // /api/admin/users 要求 SUPER_ADMIN，AUDITOR 角色应被拒绝
        String token = jwtUtil.createAdminToken(2L, "auditor", "AUDITOR");
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
