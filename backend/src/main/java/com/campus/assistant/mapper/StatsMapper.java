package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.assistant.entity.UserDomain;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理后台统计查询 Mapper（聚合统计专用）。
 */
public interface StatsMapper extends BaseMapper<UserDomain.SysConfig> {

    /** 每日统计：N 天内每日新增教室资料、失物发布、审核量 */
    @Select("""
            <script>
            SELECT d.date,
                   (SELECT COUNT(*) FROM t_classroom c
                     WHERE c.deleted = 0 AND DATE(c.created_at) = d.date) AS classroom_added,
                   (SELECT COUNT(*) FROM t_lost_item l
                     WHERE l.deleted = 0 AND DATE(l.created_at) = d.date) AS lost_published,
                   (SELECT COUNT(*) FROM t_audit_record a
                     WHERE DATE(a.created_at) = d.date) AS audit_count
            FROM (
              SELECT DATE_SUB(CURDATE(), INTERVAL n DAY) AS date
              FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
                    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
                    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
                    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
                    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
                    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
                    UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34
                    UNION SELECT 35 UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39
                    UNION SELECT 40 UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44
                    UNION SELECT 45 UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49
                    UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54
                    UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59) nums
              WHERE n &lt; #{days}
            ) d
            ORDER BY d.date DESC
            </script>
            """)
    List<Map<String, Object>> dailyStats(@Param("days") int days);

    /** 贡献排行：照片 + 资料修正 + 评价数量 */
    @Select("""
            SELECT u.id AS user_id, COALESCE(u.nickname, '未命名用户') AS nickname,
                   IFNULL(p.cnt, 0) AS photo_count,
                   IFNULL(r.cnt, 0) AS report_count,
                   IFNULL(v.cnt, 0) AS review_count,
                   IFNULL(p.cnt, 0) + IFNULL(r.cnt, 0) + IFNULL(v.cnt, 0) AS total
            FROM t_user u
            LEFT JOIN (SELECT user_id, COUNT(*) cnt FROM t_classroom_image
                       WHERE deleted = 0 GROUP BY user_id) p ON p.user_id = u.id
            LEFT JOIN (SELECT user_id, COUNT(*) cnt FROM t_classroom_equipment_report
                       WHERE deleted = 0 GROUP BY user_id) r ON r.user_id = u.id
            LEFT JOIN (SELECT user_id, COUNT(*) cnt FROM t_classroom_review
                       WHERE deleted = 0 GROUP BY user_id) v ON v.user_id = u.id
            WHERE u.deleted = 0
            HAVING total > 0
            ORDER BY total DESC
            LIMIT 50
            """)
    List<Map<String, Object>> contributorRanking();
}
