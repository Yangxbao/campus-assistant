package com.campus.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.common.BizException;
import com.campus.assistant.common.RedisHelper;
import com.campus.assistant.common.XssUtil;
import com.campus.assistant.dto.Dto.BuildingDetailVO;
import com.campus.assistant.dto.Dto.BuildingVO;
import com.campus.assistant.dto.Dto.ClassroomDetailVO;
import com.campus.assistant.dto.Dto.ClassroomVO;
import com.campus.assistant.dto.Dto.FloorGroup;
import com.campus.assistant.dto.Dto.ReviewVO;
import com.campus.assistant.dto.Dto.RoomStatusVO;
import com.campus.assistant.dto.Dto.SearchQuery;
import com.campus.assistant.dto.Dto.SocketMarkVO;
import com.campus.assistant.entity.ClassroomDomain.Building;
import com.campus.assistant.entity.ClassroomDomain.BuildingQr;
import com.campus.assistant.entity.ClassroomDomain.Classroom;
import com.campus.assistant.entity.ClassroomDomain.ClassroomImage;
import com.campus.assistant.entity.ClassroomDomain.EquipmentReport;
import com.campus.assistant.entity.ClassroomDomain.Floor;
import com.campus.assistant.entity.ClassroomDomain.Review;
import com.campus.assistant.entity.ClassroomDomain.RoomStatus;
import com.campus.assistant.entity.ClassroomDomain.SocketMark;
import com.campus.assistant.entity.UserDomain.User;
import com.campus.assistant.mapper.BuildingMapper;
import com.campus.assistant.mapper.BuildingQrMapper;
import com.campus.assistant.mapper.ClassroomImageMapper;
import com.campus.assistant.mapper.ClassroomMapper;
import com.campus.assistant.mapper.EquipmentReportMapper;
import com.campus.assistant.mapper.FloorMapper;
import com.campus.assistant.mapper.ReviewMapper;
import com.campus.assistant.mapper.RoomStatusMapper;
import com.campus.assistant.mapper.SocketMarkMapper;
import com.campus.assistant.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 楼宇/教室服务：搜索筛选、详情、照片、插座标记、评价、临时状态、申请新增、二维码、导航。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingClassroomService {

    private final BuildingMapper buildingMapper;
    private final FloorMapper floorMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomImageMapper classroomImageMapper;
    private final SocketMarkMapper socketMarkMapper;
    private final ReviewMapper reviewMapper;
    private final RoomStatusMapper roomStatusMapper;
    private final EquipmentReportMapper equipmentReportMapper;
    private final BuildingQrMapper buildingQrMapper;
    private final UserMapper userMapper;
    private final RedisHelper redis;

    // ---------- 楼宇 ----------

    public List<BuildingVO> listBuildings() {
        List<BuildingVO> list = new ArrayList<>();
        Map<Long, Long> cnt = new LinkedHashMap<>();
        classroomMapper.countPerBuilding()
                .forEach(m -> cnt.put(((Number) m.get("id")).longValue(), ((Number) m.get("cnt")).longValue()));
        for (Building b : buildingMapper.selectList(new LambdaQueryWrapper<Building>().orderByAsc(Building::getCode))) {
            BuildingVO vo = new BuildingVO();
            vo.setId(b.getId());
            vo.setCode(b.getCode());
            vo.setName(b.getName());
            vo.setFloorsCount(b.getFloorsCount());
            vo.setDescription(b.getDescription());
            vo.setCampus(b.getCampus());
            vo.setLocationDesc(b.getLocationDesc());
            vo.setClassroomCount(cnt.getOrDefault(b.getId(), 0L).intValue());
            list.add(vo);
        }
        return list;
    }

    public BuildingDetailVO buildingDetail(long id) {
        Building b = buildingMapper.selectById(id);
        if (b == null || b.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "楼宇不存在");
        }
        BuildingDetailVO vo = new BuildingDetailVO();
        vo.setId(b.getId());
        vo.setCode(b.getCode());
        vo.setName(b.getName());
        vo.setFloorsCount(b.getFloorsCount());
        vo.setDescription(b.getDescription());
        vo.setCampus(b.getCampus());
        vo.setLocationDesc(b.getLocationDesc());
        List<FloorGroup> floors = new ArrayList<>();
        Map<Integer, List<ClassroomVO>> byFloor = new LinkedHashMap<>();
        for (ClassroomVO c : classroomMapper.listByBuilding(id)) {
            int fno = c.getFloorNo() == null ? 0 : c.getFloorNo();
            byFloor.computeIfAbsent(fno, k -> new ArrayList<>()).add(c);
        }
        for (Floor f : floorMapper.selectList(new LambdaQueryWrapper<Floor>()
                .eq(Floor::getBuildingId, id).orderByDesc(Floor::getFloorNo))) {
            FloorGroup g = new FloorGroup();
            g.setFloorId(f.getId());
            g.setFloorNo(f.getFloorNo());
            g.setDescription(f.getDescription());
            g.setClassrooms(byFloor.getOrDefault(f.getFloorNo(), List.of()));
            floors.add(g);
        }
        vo.setFloors(floors);
        return vo;
    }

    /** 文字导航：楼入口→楼层→教室 的文字指引 */
    public Map<String, Object> buildingGuide(long buildingId, long classroomId) {
        BuildingDetailVO detail = buildingDetail(buildingId);
        Classroom c = classroomMapper.selectById(classroomId);
        if (c == null || c.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "教室不存在");
        }
        Integer floorNo = null;
        for (FloorGroup g : detail.getFloors()) {
            if (g.getClassrooms().stream().anyMatch(x -> x.getId().equals(classroomId))) {
                floorNo = g.getFloorNo();
                break;
            }
        }
        final Integer floorNoFinal = floorNo;
        List<String> steps = new ArrayList<>();
        steps.add("进入「" + detail.getName() + "」"
                + (detail.getLocationDesc() == null || detail.getLocationDesc().isBlank() ? "" : "（" + detail.getLocationDesc() + "）"));
        if (floorNo != null) {
            steps.add("乘电梯或走楼梯至 " + floorNo + " 层");
            detail.getFloors().stream().filter(g -> g.getFloorNo().equals(floorNoFinal)).findFirst()
                    .ifPresent(g -> {
                        if (g.getDescription() != null && !g.getDescription().isBlank()) {
                            steps.add("本层指引：" + g.getDescription());
                        }
                    });
        }
        steps.add("到达教室 " + c.getRoomNo() + (c.getName() == null ? "" : "（" + c.getName() + "）"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildingId", buildingId);
        result.put("classroomId", classroomId);
        result.put("steps", steps);
        return result;
    }

    // ---------- 教室搜索/详情 ----------

    public com.baomidou.mybatisplus.core.metadata.IPage<ClassroomVO> search(SearchQuery q) {
        if (q.getPage() == null || q.getPage() < 1) {
            q.setPage(1);
        }
        if (q.getSize() == null || q.getSize() < 1 || q.getSize() > 100) {
            q.setSize(10);
        }
        return classroomMapper.search(new Page<>(q.getPage(), q.getSize()), q);
    }

    public List<ClassroomVO> latestUpdated(int limit) {
        SearchQuery q = new SearchQuery();
        q.setPage(1);
        q.setSize(Math.min(Math.max(limit, 1), 50));
        return classroomMapper.search(new Page<>(q.getPage(), q.getSize()), q).getRecords();
    }

    public List<ClassroomVO> recentViewed(long uid, int limit) {
        List<String> ids = redis.zRevRange("room:recent:" + uid, Math.min(Math.max(limit, 1), 50));
        List<ClassroomVO> list = new ArrayList<>();
        for (String s : ids) {
            try {
                Classroom c = classroomMapper.selectById(Long.parseLong(s));
                if (c != null && c.getDeleted() == 0) {
                    list.add(toVO(c, null, null, null));
                }
            } catch (NumberFormatException ignored) {
                // 跳过非法成员
            }
        }
        return list;
    }

    public ClassroomDetailVO classroomDetail(long uid, long id) {
        Classroom c = classroomMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "教室不存在");
        }
        // 记录最近浏览（Redis zset，不可用自动降级）
        redis.zAdd("room:recent:" + uid, String.valueOf(id), System.currentTimeMillis());

        Building b = buildingMapper.selectById(c.getBuildingId());
        Floor f = c.getFloorId() == null ? null : floorMapper.selectById(c.getFloorId());
        ClassroomDetailVO vo = new ClassroomDetailVO();
        vo.setBase(toVO(c, b, f, null));
        vo.setUpdatedContentAt(c.getUpdatedContentAt());

        // 审核通过的照片
        List<String> images = classroomImageMapper.selectList(new LambdaQueryWrapper<ClassroomImage>()
                        .eq(ClassroomImage::getClassroomId, id)
                        .eq(ClassroomImage::getAuditStatus, "APPROVED")
                        .orderByDesc(ClassroomImage::getCreatedAt))
                .stream().map(ClassroomImage::getOssKey).toList();
        vo.setImages(images);

        // 插座标记
        List<SocketMarkVO> marks = socketMarkMapper.selectList(new LambdaQueryWrapper<SocketMark>()
                        .eq(SocketMark::getClassroomId, id)
                        .eq(SocketMark::getAuditStatus, "APPROVED")
                        .orderByDesc(SocketMark::getCreatedAt))
                .stream().map(this::toMarkVO).toList();
        vo.setSocketMarks(marks);

        // 最近评价
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getClassroomId, id)
                .eq(Review::getAuditStatus, "APPROVED")
                .orderByDesc(Review::getCreatedAt).last("LIMIT 20"));
        vo.setReviews(reviews.stream().map(r -> {
            ReviewVO rv = new ReviewVO();
            rv.setId(r.getId());
            rv.setUserId(r.getUserId());
            rv.setNickname(nickname(r.getUserId()));
            rv.setRating(r.getRating());
            rv.setContent(r.getContent());
            rv.setCreatedAt(r.getCreatedAt());
            return rv;
        }).toList());

        // 最新未过期状态反馈
        RoomStatus st = roomStatusMapper.selectOne(new LambdaQueryWrapper<RoomStatus>()
                .eq(RoomStatus::getClassroomId, id)
                .gt(RoomStatus::getExpiresAt, LocalDateTime.now())
                .orderByDesc(RoomStatus::getCreatedAt).last("LIMIT 1"));
        if (st != null) {
            RoomStatusVO sv = new RoomStatusVO();
            sv.setOccupancy(st.getOccupancy());
            sv.setQuietLevel(st.getQuietLevel());
            sv.setAcWorking(st.getAcWorking());
            sv.setSocketWorking(st.getSocketWorking());
            sv.setOccupiedReason(st.getOccupiedReason());
            sv.setExpiresAt(st.getExpiresAt());
            sv.setCreatedAt(st.getCreatedAt());
            vo.setLatestStatus(sv);
        }
        return vo;
    }

    // ---------- 照片/插座标记/评价/状态/反馈 ----------

    public Long addImage(long uid, long classroomId, String ossKey, String category, String description) {
        checkClassroom(classroomId);
        if (ossKey == null || ossKey.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "ossKey 不能为空");
        }
        ClassroomImage img = new ClassroomImage();
        img.setClassroomId(classroomId);
        img.setUserId(uid);
        img.setOssKey(XssUtil.escape(ossKey));
        img.setCategory(category == null ? "OTHER" : category);
        img.setDescription(XssUtil.escape(description));
        img.setAuditStatus("PENDING");
        classroomImageMapper.insert(img);
        contribute(uid);
        return img.getId();
    }

    public List<String> listImages(long classroomId) {
        return classroomImageMapper.selectList(new LambdaQueryWrapper<ClassroomImage>()
                        .eq(ClassroomImage::getClassroomId, classroomId)
                        .eq(ClassroomImage::getAuditStatus, "APPROVED")
                        .orderByDesc(ClassroomImage::getCreatedAt))
                .stream().map(ClassroomImage::getOssKey).toList();
    }

    /**
     * 【管理员上传照片】与下面的 addImage()（用户上传）唯一的区别：
     * 审核状态直接 APPROVED —— 管理员自己就是"发布者"，不需要审核员再过一道。
     * adminId 存进 user_id 字段仅作记录来源用（表字段复用，不参与贡献值统计）。
     */
    public Long addImageAdmin(long adminId, long classroomId, String ossKey, String category, String description) {
        checkClassroom(classroomId);   // 教室必须真实存在，防止挂到不存在的教室上
        if (ossKey == null || ossKey.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "ossKey 不能为空");
        }
        ClassroomImage img = new ClassroomImage();
        img.setClassroomId(classroomId);
        img.setUserId(adminId);
        img.setOssKey(XssUtil.escape(ossKey));   // XSS 转义：即使有人往描述里塞 <script> 也只会当纯文本显示
        img.setCategory(category == null ? "OTHER" : category);
        img.setDescription(XssUtil.escape(description));
        img.setAuditStatus("APPROVED");          // 与用户上传（PENDING）的区别就在这一行
        classroomImageMapper.insert(img);        // MyBatis-Plus：insert 自动生成 INSERT INTO t_classroom_image ...
        return img.getId();
    }

    /**
     * 【管理端照片列表】返回全部审核状态（含已删除除外），前端照片墙按这个渲染。
     * 对比 listImages()（小程序端）：那边只查 APPROVED。
     * 注意：MyBatis-Plus 逻辑删除（deleted 字段）是自动生效的——
     * 查询时框架自动追加 "AND deleted = 0"，代码里不用写。
     */
    public List<Map<String, Object>> listImagesAdmin(long classroomId) {
        checkClassroom(classroomId);
        // LambdaQueryWrapper 是 MyBatis-Plus 的链式查询构造器，等价于：
        // SELECT * FROM t_classroom_image WHERE classroom_id = ? AND deleted = 0 ORDER BY created_at DESC
        List<ClassroomImage> records = classroomImageMapper.selectList(
                new LambdaQueryWrapper<ClassroomImage>()
                        .eq(ClassroomImage::getClassroomId, classroomId)
                        .orderByDesc(ClassroomImage::getCreatedAt));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassroomImage img : records) {
            // 拼上 url 字段：前端 <img :src="url"> 直接用（/api/files/** 是静态资源映射）
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", img.getId());
            m.put("ossKey", img.getOssKey());
            m.put("url", "/api/files/" + img.getOssKey());
            m.put("category", img.getCategory());
            m.put("description", img.getDescription());
            m.put("auditStatus", img.getAuditStatus());
            m.put("createdAt", img.getCreatedAt());
            list.add(m);
        }
        return list;
    }

    /**
     * 【管理端删除照片】逻辑删除：不真删数据，只把 deleted 置 1（可追溯、可恢复）。
     * 坑点记录：MyBatis-Plus 开启逻辑删除后，updateById() 不会更新 deleted 字段，
     * 必须用 deleteById()（框架会把它翻译成 UPDATE ... SET deleted = 1）。
     * 当天踩过：先用 updateById 写 deleted=1，结果列表里照片还在。
     */
    public void deleteImageAdmin(long imageId) {
        ClassroomImage img = classroomImageMapper.selectById(imageId);
        if (img == null) {
            throw BizException.of(BizException.NOT_FOUND, "照片不存在");
        }
        classroomImageMapper.deleteById(imageId);
    }

    public Long addSocketMark(long uid, long classroomId, Long imageId,
                              java.math.BigDecimal posX, java.math.BigDecimal posY,
                              String wallPos, String note) {
        checkClassroom(classroomId);
        if (posX == null || posY == null) {
            throw BizException.of(BizException.BAD_REQUEST, "标记坐标不能为空");
        }
        SocketMark m = new SocketMark();
        m.setClassroomId(classroomId);
        m.setUserId(uid);
        m.setImageId(imageId);
        m.setPosX(posX);
        m.setPosY(posY);
        m.setWallPos(wallPos);
        if (note != null && !note.isBlank()) {
            // 备注并入 socket_desc 类信息：保存到评价表外的独立备注列不存在，写入 description 语义由前端展示
        }
        m.setAccuracyUp(0);
        m.setAccuracyDown(0);
        m.setAuditStatus("APPROVED");
        socketMarkMapper.insert(m);
        contribute(uid);
        return m.getId();
    }

    public List<SocketMarkVO> listSocketMarks(long classroomId) {
        return socketMarkMapper.selectList(new LambdaQueryWrapper<SocketMark>()
                        .eq(SocketMark::getClassroomId, classroomId)
                        .eq(SocketMark::getAuditStatus, "APPROVED")
                        .orderByDesc(SocketMark::getCreatedAt))
                .stream().map(this::toMarkVO).toList();
    }

    /** 插座标记准确性反馈（点赞/点踩计数） */
    public void feedbackSocketMark(long uid, long markId, boolean accurate) {
        SocketMark m = socketMarkMapper.selectById(markId);
        if (m == null || m.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "标记不存在");
        }
        socketMarkMapper.update(null, new LambdaUpdateWrapper<SocketMark>()
                .eq(SocketMark::getId, markId)
                .setSql(accurate ? "accuracy_up = accuracy_up + 1" : "accuracy_down = accuracy_down + 1"));
    }

    public Long addEquipmentReport(long uid, long classroomId, String fieldName,
                                   String newValue, String remark) {
        checkClassroom(classroomId);
        if (fieldName == null || fieldName.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "fieldName 不能为空");
        }
        EquipmentReport r = new EquipmentReport();
        r.setClassroomId(classroomId);
        r.setUserId(uid);
        r.setFieldName(XssUtil.escape(fieldName));
        r.setNewValue(XssUtil.escape(newValue));
        r.setRemark(XssUtil.escape(remark));
        r.setAuditStatus("PENDING");
        equipmentReportMapper.insert(r);
        contribute(uid);
        return r.getId();
    }

    public Long addReview(long uid, long classroomId, Integer rating, String content) {
        checkClassroom(classroomId);
        if (rating == null || rating < 1 || rating > 5) {
            throw BizException.of(BizException.BAD_REQUEST, "rating 取值 1-5");
        }
        Review r = new Review();
        r.setClassroomId(classroomId);
        r.setUserId(uid);
        r.setRating(rating);
        r.setContent(XssUtil.escape(content));
        r.setAuditStatus("APPROVED");
        reviewMapper.insert(r);
        return r.getId();
    }

    public List<ReviewVO> listReviews(long classroomId, int page, int size) {
        Page<Review> p = reviewMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 50)),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getClassroomId, classroomId)
                        .eq(Review::getAuditStatus, "APPROVED")
                        .orderByDesc(Review::getCreatedAt));
        return p.getRecords().stream().map(r -> {
            ReviewVO rv = new ReviewVO();
            rv.setId(r.getId());
            rv.setUserId(r.getUserId());
            rv.setNickname(nickname(r.getUserId()));
            rv.setRating(r.getRating());
            rv.setContent(r.getContent());
            rv.setCreatedAt(r.getCreatedAt());
            return rv;
        }).toList();
    }

    /** 提交临时现场状态，默认 4 小时有效 */
    public RoomStatusVO postStatus(long uid, long classroomId, String occupancy, Integer quietLevel,
                                   Integer acWorking, Integer socketWorking, String occupiedReason,
                                   Integer hoursValid) {
        checkClassroom(classroomId);
        int hours = hoursValid == null || hoursValid <= 0 ? 4 : Math.min(hoursValid, 24);
        RoomStatus s = new RoomStatus();
        s.setClassroomId(classroomId);
        s.setUserId(uid);
        s.setOccupancy(occupancy == null ? "UNKNOWN" : occupancy);
        s.setQuietLevel(quietLevel);
        s.setAcWorking(acWorking);
        s.setSocketWorking(socketWorking);
        s.setOccupiedReason(XssUtil.escape(occupiedReason));
        s.setExpiresAt(LocalDateTime.now().plusHours(hours));
        s.setAuditStatus("APPROVED");
        roomStatusMapper.insert(s);

        RoomStatusVO vo = new RoomStatusVO();
        vo.setOccupancy(s.getOccupancy());
        vo.setQuietLevel(s.getQuietLevel());
        vo.setAcWorking(s.getAcWorking());
        vo.setSocketWorking(s.getSocketWorking());
        vo.setOccupiedReason(s.getOccupiedReason());
        vo.setExpiresAt(s.getExpiresAt());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }

    public RoomStatusVO latestStatus(long classroomId) {
        RoomStatus st = roomStatusMapper.selectOne(new LambdaQueryWrapper<RoomStatus>()
                .eq(RoomStatus::getClassroomId, classroomId)
                .gt(RoomStatus::getExpiresAt, LocalDateTime.now())
                .orderByDesc(RoomStatus::getCreatedAt).last("LIMIT 1"));
        if (st == null) {
            return null;
        }
        RoomStatusVO vo = new RoomStatusVO();
        vo.setOccupancy(st.getOccupancy());
        vo.setQuietLevel(st.getQuietLevel());
        vo.setAcWorking(st.getAcWorking());
        vo.setSocketWorking(st.getSocketWorking());
        vo.setOccupiedReason(st.getOccupiedReason());
        vo.setExpiresAt(st.getExpiresAt());
        vo.setCreatedAt(st.getCreatedAt());
        return vo;
    }

    /** 教室未收录时申请新增：生成 PENDING 审核记录（复用 t_classroom_equipment_report，fieldName=APPLY_NEW） */
    public Long applyNewClassroom(long uid, Long buildingId, Integer floorNo, String roomNo, String remark) {
        if (buildingId == null || roomNo == null || roomNo.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "buildingId 与 roomNo 不能为空");
        }
        EquipmentReport r = new EquipmentReport();
        r.setClassroomId(buildingId);
        r.setUserId(uid);
        r.setFieldName("APPLY_NEW");
        r.setNewValue(floorNo + "层/" + XssUtil.escape(roomNo));
        r.setRemark(XssUtil.escape(remark));
        r.setAuditStatus("PENDING");
        equipmentReportMapper.insert(r);
        return r.getId();
    }

    // ---------- 二维码 ----------

    @Transactional
    public String ensureQrToken(String targetType, long targetId) {
        BuildingQr qr = buildingQrMapper.selectOne(new LambdaQueryWrapper<BuildingQr>()
                .eq(BuildingQr::getTargetType, targetType)
                .eq(BuildingQr::getTargetId, targetId).last("LIMIT 1"));
        if (qr != null) {
            return qr.getQrToken();
        }
        String token = HexFormat.of().formatHex(randomBytes(16));
        qr = new BuildingQr();
        qr.setTargetType(targetType);
        qr.setTargetId(targetId);
        qr.setQrToken(token);
        buildingQrMapper.insert(qr);
        return token;
    }

    public Map<String, Object> resolveQr(String token) {
        BuildingQr qr = buildingQrMapper.selectOne(new LambdaQueryWrapper<BuildingQr>()
                .eq(BuildingQr::getQrToken, token).last("LIMIT 1"));
        if (qr == null) {
            throw BizException.of(BizException.NOT_FOUND, "二维码无效");
        }
        return Map.of("targetType", qr.getTargetType(), "targetId", qr.getTargetId());
    }

    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    // ---------- 内部工具 ----------

    private void checkClassroom(long id) {
        Classroom c = classroomMapper.selectById(id);
        if (c == null || c.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "教室不存在");
        }
    }

    private String nickname(Long uid) {
        if (uid == null) {
            return "匿名用户";
        }
        User u = userMapper.selectById(uid);
        return u == null || u.getNickname() == null ? "匿名用户" : u.getNickname();
    }

    private void contribute(long uid) {
        try {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, uid).setSql("contribute_count = contribute_count + 1"));
        } catch (Exception e) {
            log.warn("贡献数更新失败: {}", e.getMessage());
        }
    }

    private SocketMarkVO toMarkVO(SocketMark m) {
        SocketMarkVO vo = new SocketMarkVO();
        vo.setId(m.getId());
        vo.setImageId(m.getImageId());
        vo.setPosX(m.getPosX() == null ? null : m.getPosX().doubleValue());
        vo.setPosY(m.getPosY() == null ? null : m.getPosY().doubleValue());
        vo.setWallPos(m.getWallPos());
        vo.setAccuracyUp(m.getAccuracyUp());
        vo.setAccuracyDown(m.getAccuracyDown());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }

    private ClassroomVO toVO(Classroom c, Building b, Floor f, Object reviewStat) {
        ClassroomVO vo = new ClassroomVO();
        vo.setId(c.getId());
        vo.setBuildingId(c.getBuildingId());
        if (b != null) {
            vo.setBuildingCode(b.getCode());
            vo.setBuildingName(b.getName());
        }
        if (f != null) {
            vo.setFloorNo(f.getFloorNo());
        }
        vo.setRoomNo(c.getRoomNo());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setSeatCount(c.getSeatCount());
        vo.setHasAc(c.getHasAc());
        vo.setSocketDesc(c.getSocketDesc());
        vo.setSocketCount(c.getSocketCount());
        vo.setHasProjector(c.getHasProjector());
        vo.setHasPc(c.getHasPc());
        vo.setHasMic(c.getHasMic());
        vo.setHasWhiteboard(c.getHasWhiteboard());
        vo.setHasWifi(c.getHasWifi());
        vo.setEnvScoreQuiet(c.getEnvScoreQuiet());
        vo.setEnvDesc(c.getEnvDesc());
        vo.setStatus(c.getStatus());
        vo.setUpdatedContentAt(c.getUpdatedContentAt());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }
}
