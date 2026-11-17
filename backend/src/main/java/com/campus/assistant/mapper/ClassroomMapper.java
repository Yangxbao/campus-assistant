package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.dto.Dto.ClassroomVO;
import com.campus.assistant.dto.Dto.SearchQuery;
import com.campus.assistant.entity.ClassroomDomain;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 教室 Mapper：通用 CRUD + 多条件组合筛选自定义 SQL（#{...} 预编译参数绑定，防 SQL 注入）。
 */
public interface ClassroomMapper extends BaseMapper<ClassroomDomain.Classroom> {

    /** 多条件组合筛选，支持派生条件 suitableForSelfStudy（有插座+安静>=4）与 review_desc 排序 */
    @Select("""
            <script>
            SELECT c.id, c.building_id, c.room_no, c.name, c.type, c.seat_count, c.has_ac,
                   c.socket_desc, c.socket_count, c.has_projector, c.has_pc, c.has_mic,
                   c.has_whiteboard, c.has_wifi, c.env_score_quiet, c.env_desc, c.status,
                   c.updated_content_at, c.created_at,
                   b.code AS building_code, b.name AS building_name, f.floor_no AS floor_no,
                   rv.rc AS review_count, rv.ar AS review_avg
            FROM t_classroom c
            JOIN t_building b ON b.id = c.building_id AND b.deleted = 0
            LEFT JOIN t_floor f ON f.id = c.floor_id
            LEFT JOIN (SELECT classroom_id, COUNT(*) rc, ROUND(AVG(rating),2) ar
                       FROM t_classroom_review WHERE audit_status = 'APPROVED' AND deleted = 0
                       GROUP BY classroom_id) rv ON rv.classroom_id = c.id
            WHERE c.deleted = 0 AND c.status = 'APPROVED'
            <if test="q.keyword != null and q.keyword != ''">
              AND (c.name LIKE CONCAT('%', #{q.keyword}, '%')
                   OR c.room_no LIKE CONCAT('%', #{q.keyword}, '%')
                   OR c.env_desc LIKE CONCAT('%', #{q.keyword}, '%'))
            </if>
            <if test="q.buildingId != null"> AND c.building_id = #{q.buildingId} </if>
            <if test="q.floorNo != null"> AND f.floor_no = #{q.floorNo} </if>
            <if test="q.roomNo != null and q.roomNo != ''"> AND c.room_no = #{q.roomNo} </if>
            <if test="q.type != null and q.type != ''"> AND c.type = #{q.type} </if>
            <if test="q.hasAc != null and q.hasAc > 0"> AND c.has_ac = 1 </if>
            <if test="q.hasSocket != null and q.hasSocket > 0">
              AND (c.socket_count &gt;= #{q.hasSocket}
                   OR (c.socket_desc IS NOT NULL AND c.socket_desc != ''))
            </if>
            <if test="q.hasProjector != null and q.hasProjector > 0"> AND c.has_projector = 1 </if>
            <if test="q.hasPc != null and q.hasPc > 0"> AND c.has_pc = 1 </if>
            <if test="q.hasMic != null and q.hasMic > 0"> AND c.has_mic = 1 </if>
            <if test="q.hasWhiteboard != null and q.hasWhiteboard > 0"> AND c.has_whiteboard = 1 </if>
            <if test="q.hasWifi != null and q.hasWifi > 0"> AND c.has_wifi = 1 </if>
            <if test="q.minQuiet != null and q.minQuiet > 0"> AND c.env_score_quiet &gt;= #{q.minQuiet} </if>
            <if test="q.suitableForSelfStudy != null and q.suitableForSelfStudy > 0">
              AND (COALESCE(c.socket_count, 0) &gt;= 1
                   OR (c.socket_desc IS NOT NULL AND c.socket_desc != ''))
              AND c.env_score_quiet &gt;= 4
            </if>
            ORDER BY
            <choose>
              <when test="q.sort == 'review_desc'">rv.ar DESC, rv.rc DESC, c.updated_at DESC</when>
              <otherwise>COALESCE(c.updated_content_at, c.updated_at) DESC, c.updated_at DESC</otherwise>
            </choose>
            </script>
            """)
    IPage<ClassroomVO> search(Page<ClassroomVO> page, @Param("q") SearchQuery q);

    /** 每栋楼的教室数（已审核通过、未删除） */
    @Select("""
            SELECT b.id, COUNT(c.id) AS cnt
            FROM t_building b
            LEFT JOIN t_classroom c ON c.building_id = b.id AND c.deleted = 0 AND c.status = 'APPROVED'
            WHERE b.deleted = 0
            GROUP BY b.id
            """)
    List<Map<String, Object>> countPerBuilding();

    /** 各楼层教室列表（楼宇详情用） */
    @Select("""
            SELECT c.id, c.building_id, c.room_no, c.name, c.type, c.seat_count, c.has_ac,
                   c.socket_desc, c.socket_count, c.has_projector, c.has_pc, c.has_mic,
                   c.has_whiteboard, c.has_wifi, c.env_score_quiet, c.env_desc, c.status,
                   c.updated_content_at, c.created_at,
                   b.code AS building_code, b.name AS building_name, f.floor_no AS floor_no
            FROM t_classroom c
            JOIN t_building b ON b.id = c.building_id AND b.deleted = 0
            LEFT JOIN t_floor f ON f.id = c.floor_id
            WHERE c.deleted = 0 AND c.status = 'APPROVED' AND c.building_id = #{buildingId}
            ORDER BY f.floor_no ASC, c.room_no ASC
            """)
    List<ClassroomVO> listByBuilding(@Param("buildingId") Long buildingId);
}
