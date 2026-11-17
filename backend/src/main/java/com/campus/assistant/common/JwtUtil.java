package com.campus.assistant.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 【新手导读】JWT 工具 —— 生成和验证"通行证"。
 *
 * JWT（JSON Web Token）长这样：三段用点隔开的字符串
 *   eyJhbGciOi...  .  eyJ1aWQiOi...  .  SflKxwRJSme...
 *   ├─ 头部（用什么算法签名）
 *   ├─ 载荷 payload（"名片"内容：uid、角色、过期时间……base64 可直接解码看到）
 *   └─ 签名（用服务端密钥 + HMAC 算出来的防伪码）
 *
 * 关键理解：载荷是明文的（别放密码！）——别人能看到你是 uid=2，
 * 但**改不了**：改一个字签名就对不上，服务端 verifyWith(key()) 一验就拒。
 * 所以"这张名片能不能信"完全取决于服务端密钥有没有泄露。
 *
 * 本项目两种 token 的 type 字段不同（user / admin），在 AuthInterceptor 里分开校验，互不通用。
 */
@Component
public class JwtUtil {

    /** 签名密钥：从 application.yml 的 campus.jwt.secret 注入（生产环境必须换强随机值） */
    @Value("${campus.jwt.secret}")
    private String secret;

    /** token 有效期（小时）：过期后 parse() 会抛异常 → 视为未登录，需要重新登录 */
    @Value("${campus.jwt.expire-hours}")
    private long expireHours;

    private SecretKey key() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /** 生成小程序用户 token */
    public String createUserToken(long uid, String openid, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "user");
        claims.put("uid", uid);
        claims.put("openid", openid);
        claims.put("role", role);
        return build(claims);
    }

    /** 生成管理后台 token */
    public String createAdminToken(long adminId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "admin");
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("role", role);
        return build(claims);
    }

    private String build(Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(key())
                .compact();
    }

    /** 校验并解析 token，非法/过期返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
