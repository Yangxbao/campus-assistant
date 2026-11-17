package com.campus.assistant.service;

import com.campus.assistant.dto.Dto.ClassroomVO;
import com.campus.assistant.dto.Dto.SearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 教室多条件筛选测试（契约 2.2）：hasAc、minQuiet、suitableForSelfStudy（有插座+安静>=4）、keyword、buildingId。
 */
@SpringBootTest
@ActiveProfiles("test")
class ClassroomSearchTest {

    @Autowired
    private BuildingClassroomService service;

    @Autowired
    private JdbcTemplate jdbc;

    private static final long BID = 990001L;

    ClassroomSearchTest() {
    }

    private void setup() {
        // 楼
        jdbc.update("MERGE INTO t_building (id, code, name, status, deleted) KEY(id) VALUES (?, ?, ?, 'APPROVED', 0)",
                BID, "J99", "测试教学楼");
        // c1: 有空调 + 安静5 + 插座4 → 适合自习
        insertRoom(990101L, "101", 1, 4, 5);
        // c2: 无空调、安静2、无插座
        insertRoom(990102L, "102", 0, 0, 2);
        // c3: 有空调、无插座、安静3
        insertRoom(990103L, "103", 1, 0, 3);
    }

    private void insertRoom(long id, String roomNo, int hasAc, int socketCount, int quiet) {
        jdbc.update("""
                MERGE INTO t_classroom (id, building_id, room_no, type, has_ac, has_projector, has_pc,
                    has_mic, has_whiteboard, has_wifi, socket_count, socket_desc, env_score_quiet,
                    status, deleted) KEY(id)
                VALUES (?, ?, ?, 'MULTIMEDIA', ?, 0, 0, 0, 0, 0, ?, ?, ?, 'APPROVED', 0)
                """, id, BID, roomNo, hasAc, socketCount, socketCount > 0 ? "讲台附近" : null, quiet);
    }

    private SearchQuery q() {
        SearchQuery q = new SearchQuery();
        q.setBuildingId(BID);
        return q;
    }

    @Test
    void 按有空调筛选() {
        setup();
        SearchQuery query = q();
        query.setHasAc(1);
        List<ClassroomVO> list = service.search(query).getRecords();
        assertEquals(2, list.size(), "应返回 101/103 两间有空调教室");
        assertTrue(list.stream().allMatch(v -> v.getId() == 990101L || v.getId() == 990103L));
    }

    @Test
    void 按适合自习筛选() {
        setup();
        SearchQuery query = q();
        query.setSuitableForSelfStudy(1);
        List<ClassroomVO> list = service.search(query).getRecords();
        assertEquals(1, list.size(), "仅有插座且安静>=4 的 101 满足");
        assertEquals(990101L, list.get(0).getId());
    }

    @Test
    void 按最低安静度筛选() {
        setup();
        SearchQuery query = q();
        query.setMinQuiet(3);
        List<ClassroomVO> list = service.search(query).getRecords();
        assertEquals(2, list.size(), "101(5) 与 103(3) 满足安静度>=3");
        assertFalse(list.stream().anyMatch(v -> v.getId() == 990102L));
    }

    @Test
    void 按关键词筛选() {
        setup();
        SearchQuery query = q();
        query.setKeyword("102");
        List<ClassroomVO> list = service.search(query).getRecords();
        assertEquals(1, list.size());
        assertEquals(990102L, list.get(0).getId());
    }

    @Test
    void 不筛选时返回全部已审核教室() {
        setup();
        List<ClassroomVO> list = service.search(q()).getRecords();
        assertEquals(3, list.size());
    }
}
