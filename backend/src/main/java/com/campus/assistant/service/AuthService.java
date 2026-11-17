package com.campus.assistant.service;

import com.campus.assistant.common.BizException;
import com.campus.assistant.common.JwtUtil;
import com.campus.assistant.common.RedisHelper;
import com.campus.assistant.common.XssUtil;
import com.campus.assistant.dto.AuthDto.LoginResp;
import com.campus.assistant.dto.AuthDto.UserVO;
import com.campus.assistant.entity.UserDomain.AdminUser;
import com.campus.assistant.entity.UserDomain.User;
import com.campus.assistant.mapper.AdminUserMapper;
import com.campus.assistant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * 认证服务：
 * - 微信登录：配置 WX_APPID/WX_SECRET 时真实调用 jscode2session；
 *   未配置时 devMode 按 code 生成确定性虚拟 openid（dev-{md5 前 12 位}），响应带 devMode:true。
 * - 管理员登录：bcrypt 校验，Redis 记录失败次数（5 次锁 10 分钟，Redis 不可用自动降级）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    private final RedisHelper redis;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${campus.wx.appid}")
    private String wxAppid;

    @Value("${campus.wx.secret}")
    private String wxSecret;

    /** 小程序登录 */
    public LoginResp wxLogin(String code) {
        if (code == null || code.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "code 不能为空");
        }
        boolean devMode = wxAppid == null || wxAppid.isBlank() || wxSecret == null || wxSecret.isBlank();
        String openid;
        String sessionKey = null;
        if (devMode) {
            // 开发模式：按 code 确定性生成虚拟 openid
            openid = "dev-" + md5(code).substring(0, 12);
        } else {
            Map<?, ?> resp = callJscode2session(code);
            if (resp == null || resp.get("openid") == null) {
                throw BizException.of(BizException.BAD_REQUEST,
                        "微信登录失败：" + (resp == null ? "无响应" : String.valueOf(resp.get("errmsg"))));
            }
            openid = String.valueOf(resp.get("openid"));
            sessionKey = resp.get("session_key") == null ? null : String.valueOf(resp.get("session_key"));
        }

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("用户" + openid.substring(Math.min(openid.length(), 8)));
            user.setRole("user");
            user.setStatus("ACTIVE");
            user.setContributeCount(0);
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            if ("BANNED".equals(user.getStatus())) {
                throw BizException.of(BizException.FORBIDDEN, "账号已被封禁");
            }
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }

        LoginResp resp = new LoginResp();
        resp.setToken(jwtUtil.createUserToken(user.getId(), user.getOpenid(), user.getRole()));
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setContributeCount(user.getContributeCount());
        resp.setUserInfo(vo);
        resp.setDevMode(devMode);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> callJscode2session(String code) {
        try {
            return RestClient.create().get()
                    .uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",
                            wxAppid, wxSecret, code)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("jscode2session 调用失败: {}", e.getMessage());
            throw BizException.of(BizException.BAD_REQUEST, "微信登录失败，请稍后重试");
        }
    }

    /** 管理后台登录 */
    public com.campus.assistant.dto.AuthDto.AdminLoginResp adminLogin(String username, String password) {
        if (username == null || password == null) {
            throw BizException.of(BizException.BAD_REQUEST, "用户名和密码不能为空");
        }
        // 登录失败计数（Redis，降级时跳过）
        String failKey = "admin:login:fail:" + XssUtil.escape(username);
        if (Long.parseLong(String.valueOf(redis.incrRead(failKey))) >= 5) {
            throw BizException.of(BizException.RATE_LIMITED, "失败次数过多，请 10 分钟后再试");
        }
        AdminUser admin = adminUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username));
        if (admin == null || admin.getDeleted() == 1 || !encoder.matches(password, admin.getPassword())) {
            redis.incr(failKey, Duration.ofMinutes(10));
            throw BizException.of(BizException.BAD_REQUEST, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(admin.getStatus())) {
            throw BizException.of(BizException.FORBIDDEN, "账号已停用");
        }
        redis.delete(failKey);
        admin.setLastLoginTime(LocalDateTime.now());
        adminUserMapper.updateById(admin);

        com.campus.assistant.dto.AuthDto.AdminLoginResp resp = new com.campus.assistant.dto.AuthDto.AdminLoginResp();
        resp.setToken(jwtUtil.createAdminToken(admin.getId(), admin.getUsername(), admin.getRole()));
        resp.setRole(admin.getRole());
        resp.setRealName(admin.getRealName());
        return resp;
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
