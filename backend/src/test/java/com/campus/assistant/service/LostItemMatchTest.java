package com.campus.assistant.service;

import com.campus.assistant.dto.LostDto.MatchVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 失物自动匹配规则测试（契约 2.4）：LOST↔FOUND 同类别、时间窗 30 天、地点/关键词加分，score>=40 才返回。
 */
@SpringBootTest
@ActiveProfiles("test")
class LostItemMatchTest {

    @Autowired
    private LostItemService service;

    @Autowired
    private JdbcTemplate jdbc;

    private static final long USER_LOST = 920001L;
    private static final long USER_FOUND = 920002L;

    private void setupUsers() {
        jdbc.update("MERGE INTO t_user (id, openid, nickname, role, status, contribute_count, deleted) KEY(id) VALUES (?, ?, ?, 'user', 'ACTIVE', 0, 0)",
                USER_LOST, "dev-match-lost", "丢东西的");
        jdbc.update("MERGE INTO t_user (id, openid, nickname, role, status, contribute_count, deleted) KEY(id) VALUES (?, ?, ?, 'user', 'ACTIVE', 0, 0)",
                USER_FOUND, "dev-match-found", "捡东西的");
    }

    private long insertItem(long uid, String kind, String category, String title, Long buildingId,
                            String locationDesc, LocalDateTime lostTime) {
        jdbc.update("""
                MERGE INTO t_lost_item (id, user_id, kind, title, category, description, lost_time,
                    building_id, location_desc, contact_visible, status, view_count, audit_status, deleted) KEY(id)
                VALUES (?, ?, ?, ?, ?, '测试描述', ?, ?, ?, 0, 'OPEN', 0, 'APPROVED', 0)
                """, nextId(), uid, kind, title, category, lostTime, buildingId, locationDesc);
        return jdbc.queryForObject("SELECT MAX(id) FROM t_lost_item", Long.class);
    }

    private static long seq = 920100L;

    private long nextId() {
        return ++seq;
    }

    @Test
    void 同类别同楼宇时间相近关键词重合应匹配() {
        setupUsers();
        LocalDateTime now = LocalDateTime.now();
        long lostId = insertItem(USER_LOST, "LOST", "电子产品", "丢失蓝色 iPhone 手机", 920010L, "图书馆三楼", now);
        long foundId = insertItem(USER_FOUND, "FOUND", "电子产品", "捡到一部 iPhone 手机", 920010L, "图书馆三楼", now);

        List<MatchVO> matches = service.matches(USER_LOST, lostId);
        assertFalse(matches.isEmpty(), "同楼宇+时间相近+关键词重合应命中");
        assertEquals(foundId, matches.get(0).getId());
        assertTrue(matches.get(0).getScore() >= 40);
        assertTrue(matches.get(0).getMatchReasons().stream().anyMatch(r -> r.contains("同一楼宇")));
        assertTrue(matches.get(0).getMatchReasons().stream().anyMatch(r -> r.contains("时间相近")));
    }

    @Test
    void 不同类别不应匹配() {
        setupUsers();
        LocalDateTime now = LocalDateTime.now();
        long lostId = insertItem(USER_LOST, "LOST", "电子产品", "丢失黑色钱包卡包", 920010L, "食堂一楼", now);
        insertItem(USER_FOUND, "FOUND", "钥匙", "捡到一串钥匙", 920010L, "食堂一楼", now);
        List<MatchVO> matches = service.matches(USER_LOST, lostId);
        assertTrue(matches.isEmpty(), "类别不同（电子产品 vs 钥匙）不应命中");
    }

    @Test
    void 超出30天时间窗且无地点关键词重合不应匹配() {
        setupUsers();
        long lostId = insertItem(USER_LOST, "LOST", "书籍", "丢失高等数学教材", 920011L, "教学楼A栋",
                LocalDateTime.now().minusDays(60));
        insertItem(USER_FOUND, "FOUND", "书籍", "捡到一本英语词典", 920012L, "教学楼B栋",
                LocalDateTime.now());
        List<MatchVO> matches = service.matches(USER_LOST, lostId);
        assertTrue(matches.isEmpty(), "时间窗超30天+不同楼宇+无关键词重合不应命中");
    }

    @Test
    void 自己发布的信息不参与匹配() {
        setupUsers();
        LocalDateTime now = LocalDateTime.now();
        long lostId = insertItem(USER_LOST, "LOST", "卡类", "丢失校园卡一张", 920010L, "体育馆", now);
        insertItem(USER_LOST, "FOUND", "卡类", "捡到校园卡", 920010L, "体育馆", now);
        List<MatchVO> matches = service.matches(USER_LOST, lostId);
        assertTrue(matches.isEmpty(), "同一用户发布的不应命中");
    }
}
