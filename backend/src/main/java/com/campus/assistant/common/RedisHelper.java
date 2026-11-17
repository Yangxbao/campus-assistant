package com.campus.assistant.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Redis 帮助类：所有操作 try-catch 包裹，Redis 不可用时自动降级（返回空结果/跳过），不阻断主流程。
 * 键约定（契约第 4 节）：
 * - room:recent:{uid}  zset 最近浏览教室
 * - rate:upload:{uid} / rate:publish:{uid}  限流
 * - admin:login:fail:{username}  登录失败计数
 * - room:detail:{id}  教室详情缓存（5 分钟）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHelper {

    private final StringRedisTemplate redis;

    public void zAdd(String key, String member, double score) {
        try {
            redis.opsForZSet().add(key, member, score);
        } catch (Exception e) {
            log.debug("Redis 降级 zAdd {}: {}", key, e.getMessage());
        }
    }

    /** 按分数倒序取前 n 个成员，异常返回空列表 */
    public List<String> zRevRange(String key, long n) {
        try {
            Set<String> set = redis.opsForZSet().reverseRange(key, 0, n - 1);
            return set == null ? List.of() : new java.util.ArrayList<>(set);
        } catch (Exception e) {
            log.debug("Redis 降级 zRevRange {}: {}", key, e.getMessage());
            return List.of();
        }
    }

    public void zRemove(String key, String member) {
        try {
            redis.opsForZSet().remove(key, member);
        } catch (Exception e) {
            log.debug("Redis 降级 zRemove: {}", e.getMessage());
        }
    }

    /** 限流：窗口内计数超限返回 true（Redis 不可用时放行） */
    public boolean rateLimited(String key, int limit, Duration window) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            return count != null && count > limit;
        } catch (Exception e) {
            log.debug("Redis 降级 rateLimited: {}", e.getMessage());
            return false;
        }
    }

    /** 失败计数 +1 并返回当前值 */
    public long incr(String key, Duration expire) {
        try {
            Long c = redis.opsForValue().increment(key);
            if (c != null && c == 1L) {
                redis.expire(key, expire);
            }
            return c == null ? 0 : c;
        } catch (Exception e) {
            return 0;
        }
    }

    public void setEx(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.debug("Redis 降级 setEx {}: {}", key, e.getMessage());
        }
    }

    public String get(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("Redis 降级 get {}: {}", key, e.getMessage());
            return null;
        }
    }

    /** 只读查询计数（不累加），Redis 不可用返回 0 */
    public long incrRead(String key) {
        try {
            String v = redis.opsForValue().get(key);
            return v == null ? 0 : Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }

    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.debug("Redis 降级 delete {}: {}", key, e.getMessage());
        }
    }

    public boolean ping() {
        try {
            return Boolean.TRUE.equals(redis.execute(
                    (org.springframework.data.redis.core.RedisCallback<String>) conn -> conn.ping()));
        } catch (Exception e) {
            return false;
        }
    }
}
