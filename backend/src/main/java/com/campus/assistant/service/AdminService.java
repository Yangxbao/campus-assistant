package com.campus.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.common.BizException;
import com.campus.assistant.common.UserContext;
import com.campus.assistant.dto.AdminDto.AuditHandleReq;
import com.campus.assistant.dto.AdminDto.AuditItemVO;
import com.campus.assistant.dto.AdminDto.ContributorRow;
import com.campus.assistant.dto.AdminDto.DailyStat;
import com.campus.assistant.dto.AdminDto.ImportResult;
import com.campus.assistant.dto.AdminDto.QrResp;
import com.campus.assistant.dto.AdminDto.RowError;
import com.campus.assistant.dto.AdminDto.StatsOverview;
import com.campus.assistant.dto.AdminDto.UserAdminVO;
import com.campus.assistant.entity.ClassroomDomain.Building;
import com.campus.assistant.entity.ClassroomDomain.Classroom;
import com.campus.assistant.entity.ClassroomDomain.ClassroomImage;
import com.campus.assistant.entity.ClassroomDomain.EquipmentReport;
import com.campus.assistant.entity.ClassroomDomain.Floor;
import com.campus.assistant.entity.ClassroomDomain.Review;
import com.campus.assistant.entity.ClassroomDomain.RoomStatus;
import com.campus.assistant.entity.CommunityDomain.AuditRecord;
import com.campus.assistant.entity.CommunityDomain.LostItem;
import com.campus.assistant.entity.CommunityDomain.Report;
import com.campus.assistant.entity.UserDomain.SysConfig;
import com.campus.assistant.entity.UserDomain.User;
import com.campus.assistant.mapper.AuditRecordMapper;
import com.campus.assistant.mapper.BuildingMapper;
import com.campus.assistant.mapper.BuildingQrMapper;
import com.campus.assistant.mapper.ClassroomImageMapper;
import com.campus.assistant.mapper.ClassroomMapper;
import com.campus.assistant.mapper.EquipmentReportMapper;
import com.campus.assistant.mapper.FloorMapper;
import com.campus.assistant.mapper.LostItemMapper;
import com.campus.assistant.mapper.ReportMapper;
import com.campus.assistant.mapper.ReviewMapper;
import com.campus.assistant.mapper.RoomStatusMapper;
import com.campus.assistant.mapper.StatsMapper;
import com.campus.assistant.mapper.SysConfigMapper;
import com.campus.assistant.mapper.UserMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理后台服务：楼宇/教室 CRUD、Excel/CSV 导入导出、二维码、审核流（写 t_audit_record + 发消息）、
 * 用户管理、统计、系统配置、举报处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Set<String> ROOM_TYPES = Set.of("LINEAR", "SMALL", "LAB", "MULTIMEDIA", "OTHER");

    private final BuildingMapper buildingMapper;
    private final FloorMapper floorMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassroomImageMapper classroomImageMapper;
    private final EquipmentReportMapper equipmentReportMapper;
    private final ReviewMapper reviewMapper;
    private final RoomStatusMapper roomStatusMapper;
    private final LostItemMapper lostItemMapper;
    private final ReportMapper reportMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final BuildingQrMapper buildingQrMapper;
    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;
    private final StatsMapper statsMapper;
    private final MessageService messageService;
    private final BuildingClassroomService buildingClassroomService;

    @Value("${campus.qr.base-url:https://example-campus.local/qr}")
    private String qrBaseUrl;

    // ---------- 楼宇 CRUD ----------

    public List<Building> listBuildings() {
        return buildingMapper.selectList(new LambdaQueryWrapper<Building>().orderByAsc(Building::getCode));
    }

    @Transactional
    public Long createBuilding(Building b, List<Integer> floors) {
        if (b.getCode() == null || b.getName() == null) {
            throw BizException.of(BizException.BAD_REQUEST, "楼编号与名称不能为空");
        }
        b.setStatus("APPROVED");
        buildingMapper.insert(b);
        saveFloors(b.getId(), floors);
        return b.getId();
    }

    @Transactional
    public void updateBuilding(long id, Building b, List<Integer> floors) {
        Building exist = buildingMapper.selectById(id);
        if (exist == null || exist.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "楼宇不存在");
        }
        exist.setCode(b.getCode() == null ? exist.getCode() : b.getCode());
        exist.setName(b.getName() == null ? exist.getName() : b.getName());
        exist.setFloorsCount(b.getFloorsCount() == null ? exist.getFloorsCount() : b.getFloorsCount());
        exist.setDescription(b.getDescription() == null ? exist.getDescription() : b.getDescription());
        exist.setCampus(b.getCampus() == null ? exist.getCampus() : b.getCampus());
        exist.setLocationDesc(b.getLocationDesc() == null ? exist.getLocationDesc() : b.getLocationDesc());
        buildingMapper.updateById(exist);
        if (floors != null) {
            saveFloors(id, floors);
        }
    }

    public void deleteBuilding(long id) {
        buildingMapper.deleteById(id);
    }

    private void saveFloors(long buildingId, List<Integer> floors) {
        if (floors == null || floors.isEmpty()) {
            return;
        }
        for (Integer no : floors) {
            Floor exist = floorMapper.selectOne(new LambdaQueryWrapper<Floor>()
                    .eq(Floor::getBuildingId, buildingId).eq(Floor::getFloorNo, no).last("LIMIT 1"));
            if (exist == null) {
                Floor f = new Floor();
                f.setBuildingId(buildingId);
                f.setFloorNo(no);
                floorMapper.insert(f);
            }
        }
        Building b = buildingMapper.selectById(buildingId);
        if (b != null && b.getFloorsCount() == null) {
            b.setFloorsCount(floors.size());
            buildingMapper.updateById(b);
        }
    }

    // ---------- 教室 CRUD ----------

    public Page<Classroom> listClassrooms(Long buildingId, Long floorId, String roomNo, String type,
                                          int page, int size) {
        LambdaQueryWrapper<Classroom> w = new LambdaQueryWrapper<>();
        w.eq(buildingId != null, Classroom::getBuildingId, buildingId);
        w.eq(floorId != null, Classroom::getFloorId, floorId);
        w.eq(roomNo != null && !roomNo.isBlank(), Classroom::getRoomNo, roomNo);
        w.eq(type != null && !type.isBlank(), Classroom::getType, type);
        w.orderByAsc(Classroom::getBuildingId).orderByAsc(Classroom::getRoomNo);
        return classroomMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 100)), w);
    }

    @Transactional
    public Long createClassroom(Classroom c, Integer floorNo) {
        validateClassroom(c);
        if (c.getStatus() == null) {
            c.setStatus("APPROVED");
        }
        attachFloor(c, floorNo);
        c.setUpdatedContentAt(LocalDateTime.now());
        classroomMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void updateClassroom(long id, Classroom c, Integer floorNo) {
        Classroom exist = classroomMapper.selectById(id);
        if (exist == null || exist.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "教室不存在");
        }
        if (c.getName() != null) {
            exist.setName(c.getName());
        }
        if (c.getType() != null) {
            exist.setType(c.getType());
        }
        if (c.getRoomNo() != null) {
            exist.setRoomNo(c.getRoomNo());
        }
        if (c.getSeatCount() != null) {
            exist.setSeatCount(c.getSeatCount());
        }
        if (c.getHasAc() != null) {
            exist.setHasAc(c.getHasAc());
        }
        if (c.getSocketDesc() != null) {
            exist.setSocketDesc(c.getSocketDesc());
        }
        if (c.getSocketCount() != null) {
            exist.setSocketCount(c.getSocketCount());
        }
        if (c.getHasProjector() != null) {
            exist.setHasProjector(c.getHasProjector());
        }
        if (c.getHasPc() != null) {
            exist.setHasPc(c.getHasPc());
        }
        if (c.getHasMic() != null) {
            exist.setHasMic(c.getHasMic());
        }
        if (c.getHasWhiteboard() != null) {
            exist.setHasWhiteboard(c.getHasWhiteboard());
        }
        if (c.getHasWifi() != null) {
            exist.setHasWifi(c.getHasWifi());
        }
        if (c.getOtherDevices() != null) {
            exist.setOtherDevices(c.getOtherDevices());
        }
        if (c.getEnvScoreQuiet() != null) {
            exist.setEnvScoreQuiet(c.getEnvScoreQuiet());
        }
        if (c.getEnvDesc() != null) {
            exist.setEnvDesc(c.getEnvDesc());
        }
        if (c.getDeskDesc() != null) {
            exist.setDeskDesc(c.getDeskDesc());
        }
        if (floorNo != null) {
            attachFloor(exist, floorNo);
        }
        exist.setUpdatedContentAt(LocalDateTime.now());
        classroomMapper.updateById(exist);
    }

    public void deleteClassroom(long id) {
        classroomMapper.deleteById(id);
    }

    private void validateClassroom(Classroom c) {
        if (c.getBuildingId() == null || c.getRoomNo() == null || c.getRoomNo().isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "buildingId 与 roomNo 不能为空");
        }
        if (c.getType() != null && !ROOM_TYPES.contains(c.getType())) {
            throw BizException.of(BizException.BAD_REQUEST, "type 取值 LINEAR/SMALL/LAB/MULTIMEDIA/OTHER");
        }
    }

    private void attachFloor(Classroom c, Integer floorNo) {
        if (floorNo == null) {
            return;
        }
        Floor f = floorMapper.selectOne(new LambdaQueryWrapper<Floor>()
                .eq(Floor::getBuildingId, c.getBuildingId()).eq(Floor::getFloorNo, floorNo).last("LIMIT 1"));
        if (f == null) {
            f = new Floor();
            f.setBuildingId(c.getBuildingId());
            f.setFloorNo(floorNo);
            floorMapper.insert(f);
        }
        c.setFloorId(f.getId());
    }

    // ---------- 导入 / 导出 ----------

    /** 上传 xlsx/csv 批量导入教室，返回成功/失败行明细 */
    public ImportResult importClassrooms(String filename, byte[] data) {
        List<List<String>> rows;
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".xlsx")) {
            rows = parseXlsx(data);
        } else if (name.endsWith(".csv")) {
            rows = parseCsv(new String(data, StandardCharsets.UTF_8));
        } else {
            throw BizException.of(BizException.BAD_REQUEST, "仅支持 xlsx/csv 文件");
        }
        ImportResult result = new ImportResult();
        List<RowError> errors = new ArrayList<>();
        int success = 0;
        // 第 0 行为表头，校验表头包含关键字段
        if (rows.isEmpty()) {
            throw BizException.of(BizException.BAD_REQUEST, "文件内容为空");
        }
        List<String> header = rows.get(0);
        if (header.size() < 4 || !header.get(0).contains("楼栋")) {
            throw BizException.of(BizException.BAD_REQUEST, "表头不符合导入模板（首列应为楼栋编号）");
        }
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                String buildingCode = cell(row, 0);
                String buildingName = cell(row, 1);
                Integer floorNo = Integer.parseInt(cell(row, 2));
                String roomNo = cell(row, 3);
                if (buildingCode.isBlank() || roomNo.isBlank()) {
                    throw new IllegalArgumentException("楼栋编号与教室编号必填");
                }
                Building b = buildingMapper.selectOne(new LambdaQueryWrapper<Building>()
                        .eq(Building::getCode, buildingCode).last("LIMIT 1"));
                if (b == null) {
                    b = new Building();
                    b.setCode(buildingCode);
                    b.setName(buildingName == null || buildingName.isBlank() ? buildingCode : buildingName);
                    b.setStatus("APPROVED");
                    buildingMapper.insert(b);
                }
                Floor f = floorMapper.selectOne(new LambdaQueryWrapper<Floor>()
                        .eq(Floor::getBuildingId, b.getId()).eq(Floor::getFloorNo, floorNo).last("LIMIT 1"));
                if (f == null) {
                    f = new Floor();
                    f.setBuildingId(b.getId());
                    f.setFloorNo(floorNo);
                    floorMapper.insert(f);
                }
                Classroom exist = classroomMapper.selectOne(new LambdaQueryWrapper<Classroom>()
                        .eq(Classroom::getBuildingId, b.getId()).eq(Classroom::getRoomNo, roomNo)
                        .last("LIMIT 1"));
                Classroom c = exist == null ? new Classroom() : exist;
                c.setBuildingId(b.getId());
                c.setFloorId(f.getId());
                c.setRoomNo(roomNo);
                c.setName(cell(row, 4));
                String type = cell(row, 5);
                c.setType(ROOM_TYPES.contains(type) ? type : "OTHER");
                c.setSeatCount(intOrNull(cell(row, 6)));
                c.setHasAc("是".equals(cell(row, 7)) || "1".equals(cell(row, 7)) ? 1 : 0);
                c.setSocketDesc(cell(row, 8));
                c.setSocketCount(intOrNull(cell(row, 9)));
                c.setHasProjector("是".equals(cell(row, 10)) || "1".equals(cell(row, 10)) ? 1 : 0);
                c.setHasPc("是".equals(cell(row, 11)) || "1".equals(cell(row, 11)) ? 1 : 0);
                c.setHasMic("是".equals(cell(row, 12)) || "1".equals(cell(row, 12)) ? 1 : 0);
                c.setHasWhiteboard("是".equals(cell(row, 13)) || "1".equals(cell(row, 13)) ? 1 : 0);
                c.setHasWifi("是".equals(cell(row, 14)) || "1".equals(cell(row, 14)) ? 1 : 0);
                c.setOtherDevices(cell(row, 15));
                c.setEnvScoreQuiet(intOrNull(cell(row, 16)));
                c.setEnvDesc(cell(row, 17));
                c.setStatus("APPROVED");
                c.setUpdatedContentAt(LocalDateTime.now());
                if (exist == null) {
                    classroomMapper.insert(c);
                } else {
                    classroomMapper.updateById(c);
                }
                success++;
            } catch (Exception e) {
                RowError err = new RowError();
                err.setRow(i + 1);
                err.setReason(e.getMessage() == null ? "数据格式错误" : e.getMessage());
                errors.add(err);
            }
        }
        result.setSuccessCount(success);
        result.setFailCount(errors.size());
        result.setFailRows(errors);
        return result;
    }

    /** 导出教室 xlsx 字节流 */
    public byte[] exportClassrooms() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("教室");
            Row header = sheet.createRow(0);
            List<String> heads = List.of("楼栋编号", "楼栋名称", "楼层", "教室编号", "教室名称", "类型", "座位数",
                    "有空调", "插座位置描述", "插座数量", "投影仪", "电脑", "麦克风", "白板", "WIFI",
                    "其他设备", "安静度1-5", "环境描述");
            for (int i = 0; i < heads.size(); i++) {
                header.createCell(i).setCellValue(heads.get(i));
            }
            List<Classroom> list = classroomMapper.selectList(new LambdaQueryWrapper<Classroom>()
                    .orderByAsc(Classroom::getBuildingId).orderByAsc(Classroom::getRoomNo));
            int r = 1;
            for (Classroom c : list) {
                Row row = sheet.createRow(r++);
                Building b = buildingMapper.selectById(c.getBuildingId());
                Floor f = c.getFloorId() == null ? null : floorMapper.selectById(c.getFloorId());
                row.createCell(0).setCellValue(b == null ? "" : nz(b.getCode()));
                row.createCell(1).setCellValue(b == null ? "" : nz(b.getName()));
                row.createCell(2).setCellValue(f == null || f.getFloorNo() == null ? "" : String.valueOf(f.getFloorNo()));
                row.createCell(3).setCellValue(nz(c.getRoomNo()));
                row.createCell(4).setCellValue(nz(c.getName()));
                row.createCell(5).setCellValue(nz(c.getType()));
                row.createCell(6).setCellValue(c.getSeatCount() == null ? "" : String.valueOf(c.getSeatCount()));
                row.createCell(7).setCellValue(int1(c.getHasAc()));
                row.createCell(8).setCellValue(nz(c.getSocketDesc()));
                row.createCell(9).setCellValue(c.getSocketCount() == null ? "" : String.valueOf(c.getSocketCount()));
                row.createCell(10).setCellValue(int1(c.getHasProjector()));
                row.createCell(11).setCellValue(int1(c.getHasPc()));
                row.createCell(12).setCellValue(int1(c.getHasMic()));
                row.createCell(13).setCellValue(int1(c.getHasWhiteboard()));
                row.createCell(14).setCellValue(int1(c.getHasWifi()));
                row.createCell(15).setCellValue(nz(c.getOtherDevices()));
                row.createCell(16).setCellValue(c.getEnvScoreQuiet() == null ? "" : String.valueOf(c.getEnvScoreQuiet()));
                row.createCell(17).setCellValue(nz(c.getEnvDesc()));
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出失败", e);
        }
    }

    // ---------- 二维码 ----------

    /** 教室二维码 PNG base64，内容 {qrBaseUrl}/{token} */
    public QrResp classroomQrcode(long classroomId) {
        Classroom c = classroomMapper.selectById(classroomId);
        if (c == null || c.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "教室不存在");
        }
        String token = buildingClassroomService.ensureQrToken("CLASSROOM", classroomId);
        String url = qrBaseUrl + "/" + token;
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);
            QrResp resp = new QrResp();
            resp.setImageBase64("data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(bos.toByteArray()));
            resp.setQrUrl(url);
            resp.setToken(token);
            return resp;
        } catch (Exception e) {
            throw new IllegalStateException("二维码生成失败", e);
        }
    }

    // ---------- 审核流 ----------

    /** 待审列表：照片/资料修正/失物/评价/教室状态（PENDING），可按 targetType 过滤 */
    public List<AuditItemVO> auditList(String targetType, int page, int size) {
        List<AuditItemVO> all = new ArrayList<>();
        if (targetType == null || targetType.isBlank() || targetType.equals("CLASSROOM_IMAGE")) {
            for (ClassroomImage img : classroomImageMapper.selectList(new LambdaQueryWrapper<ClassroomImage>()
                    .eq(ClassroomImage::getAuditStatus, "PENDING").orderByDesc(ClassroomImage::getCreatedAt))) {
                all.add(item("CLASSROOM_IMAGE", img.getId(), img.getUserId(),
                        "教室照片 ossKey=" + img.getOssKey(), img.getAuditStatus(), img.getCreatedAt()));
            }
        }
        if (targetType == null || targetType.isBlank() || targetType.equals("EQUIPMENT_REPORT")) {
            for (EquipmentReport r : equipmentReportMapper.selectList(new LambdaQueryWrapper<EquipmentReport>()
                    .eq(EquipmentReport::getAuditStatus, "PENDING").orderByDesc(EquipmentReport::getCreatedAt))) {
                all.add(item("EQUIPMENT_REPORT", r.getId(), r.getUserId(),
                        "资料修正 " + r.getFieldName() + " → " + r.getNewValue(), r.getAuditStatus(), r.getCreatedAt()));
            }
        }
        if (targetType == null || targetType.isBlank() || targetType.equals("LOST_ITEM")) {
            for (LostItem i : lostItemMapper.selectList(new LambdaQueryWrapper<LostItem>()
                    .eq(LostItem::getAuditStatus, "PENDING").orderByDesc(LostItem::getCreatedAt))) {
                all.add(item("LOST_ITEM", i.getId(), i.getUserId(),
                        "失物发布「" + i.getTitle() + "」", i.getAuditStatus(), i.getCreatedAt()));
            }
        }
        if (targetType == null || targetType.isBlank() || targetType.equals("REVIEW")) {
            for (Review r : reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getAuditStatus, "PENDING").orderByDesc(Review::getCreatedAt))) {
                all.add(item("REVIEW", r.getId(), r.getUserId(),
                        "教室评价 rating=" + r.getRating(), r.getAuditStatus(), r.getCreatedAt()));
            }
        }
        if (targetType == null || targetType.isBlank() || targetType.equals("ROOM_STATUS")) {
            for (RoomStatus s : roomStatusMapper.selectList(new LambdaQueryWrapper<RoomStatus>()
                    .eq(RoomStatus::getAuditStatus, "PENDING").orderByDesc(RoomStatus::getCreatedAt))) {
                all.add(item("ROOM_STATUS", s.getId(), s.getUserId(),
                        "教室状态 " + s.getOccupancy(), s.getAuditStatus(), s.getCreatedAt()));
            }
        }
        int from = Math.max((page - 1) * size, 0);
        int to = Math.min(from + size, all.size());
        return from >= all.size() ? List.of() : all.subList(from, to);
    }

    /** 审核处理：更新目标状态 + 写 t_audit_record + 发站内消息 */
    @Transactional
    public void handleAudit(AuditHandleReq req) {
        if (req.getTargetType() == null || req.getTargetId() == null
                || !List.of("APPROVE", "REJECT", "OFFLINE", "DELETE").contains(req.getAction())) {
            throw BizException.of(BizException.BAD_REQUEST, "审核参数不合法");
        }
        String newStatus = switch (req.getAction()) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "OFFLINE" -> "OFFLINE";
            default -> "DELETE";
        };
        Long submitterId = null;
        String summary = "";
        switch (req.getTargetType()) {
            case "CLASSROOM_IMAGE" -> {
                ClassroomImage img = classroomImageMapper.selectById(req.getTargetId());
                if (img == null) throw BizException.of(BizException.NOT_FOUND, "目标不存在");
                submitterId = img.getUserId();
                summary = "您的教室照片";
                if ("DELETE".equals(req.getAction())) {
                    classroomImageMapper.deleteById(img.getId());
                } else {
                    img.setAuditStatus(newStatus);
                    classroomImageMapper.updateById(img);
                }
            }
            case "EQUIPMENT_REPORT" -> {
                EquipmentReport r = equipmentReportMapper.selectById(req.getTargetId());
                if (r == null) throw BizException.of(BizException.NOT_FOUND, "目标不存在");
                submitterId = r.getUserId();
                summary = "您的资料修正";
                r.setAuditStatus(newStatus);
                r.setAuditorId(UserContext.adminId());
                r.setAuditRemark(req.getRemark());
                r.setAuditTime(LocalDateTime.now());
                equipmentReportMapper.updateById(r);
            }
            case "LOST_ITEM" -> {
                LostItem i = lostItemMapper.selectById(req.getTargetId());
                if (i == null) throw BizException.of(BizException.NOT_FOUND, "目标不存在");
                submitterId = i.getUserId();
                summary = "您发布的失物「" + i.getTitle() + "」";
                if ("DELETE".equals(req.getAction())) {
                    lostItemMapper.deleteById(i.getId());
                } else {
                    i.setAuditStatus(newStatus);
                    lostItemMapper.updateById(i);
                }
            }
            case "REVIEW" -> {
                Review r = reviewMapper.selectById(req.getTargetId());
                if (r == null) throw BizException.of(BizException.NOT_FOUND, "目标不存在");
                submitterId = r.getUserId();
                summary = "您的教室评价";
                r.setAuditStatus(newStatus);
                reviewMapper.updateById(r);
            }
            case "ROOM_STATUS" -> {
                RoomStatus s = roomStatusMapper.selectById(req.getTargetId());
                if (s == null) throw BizException.of(BizException.NOT_FOUND, "目标不存在");
                submitterId = s.getUserId();
                summary = "您提交的教室状态";
                s.setAuditStatus(newStatus);
                roomStatusMapper.updateById(s);
            }
            default -> throw BizException.of(BizException.BAD_REQUEST, "targetType 不合法");
        }
        // 审核记录
        AuditRecord rec = new AuditRecord();
        rec.setTargetType(req.getTargetType());
        rec.setTargetId(req.getTargetId());
        rec.setAuditorId(UserContext.adminId());
        rec.setAction(req.getAction());
        rec.setRemark(req.getRemark());
        auditRecordMapper.insert(rec);
        // 站内消息
        if (submitterId != null) {
            messageService.send(submitterId, "AUDIT",
                    "内容审核结果：" + req.getAction(),
                    summary + " 审核结果为 " + req.getAction()
                            + (req.getRemark() == null || req.getRemark().isBlank() ? "" : "，备注：" + req.getRemark()),
                    req.getTargetType(), req.getTargetId());
        }
    }

    // ---------- 举报处理 ----------

    public Page<Report> listReports(String status, int page, int size) {
        return reportMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 100)),
                new LambdaQueryWrapper<Report>()
                        .eq(status != null && !status.isBlank(), Report::getStatus, status)
                        .orderByDesc(Report::getCreatedAt));
    }

    public void handleReport(long id, String handleResult, String status) {
        Report r = reportMapper.selectById(id);
        if (r == null || r.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "举报记录不存在");
        }
        if (!List.of("HANDLED", "REJECTED").contains(status)) {
            throw BizException.of(BizException.BAD_REQUEST, "status 取值 HANDLED/REJECTED");
        }
        r.setStatus(status);
        r.setHandlerId(UserContext.adminId());
        r.setHandleResult(handleResult);
        reportMapper.updateById(r);
    }

    // ---------- 用户管理 ----------

    public Page<UserAdminVO> listUsers(String keyword, String status, int page, int size) {
        Page<User> p = userMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 100)),
                new LambdaQueryWrapper<User>()
                        .like(keyword != null && !keyword.isBlank(), User::getNickname, keyword)
                        .eq(status != null && !status.isBlank(), User::getStatus, status)
                        .orderByDesc(User::getCreatedAt));
        Page<UserAdminVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toUserVO).toList());
        return result;
    }

    public Map<String, Object> userDetail(long id) {
        User u = userMapper.selectById(id);
        if (u == null || u.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", toUserVO(u));
        // 违规记录：被举报记录
        m.put("reportsAgainst", reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, "USER").eq(Report::getTargetId, id)
                .orderByDesc(Report::getCreatedAt)));
        return m;
    }

    public void updateUserStatus(long id, String status) {
        if (!List.of("ACTIVE", "BANNED").contains(status)) {
            throw BizException.of(BizException.BAD_REQUEST, "status 取值 ACTIVE/BANNED");
        }
        User u = userMapper.selectById(id);
        if (u == null || u.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "用户不存在");
        }
        u.setStatus(status);
        userMapper.updateById(u);
    }

    // ---------- 统计 ----------

    public StatsOverview statsOverview() {
        StatsOverview vo = new StatsOverview();
        vo.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        vo.setBuildingCount(buildingMapper.selectCount(new LambdaQueryWrapper<Building>().eq(Building::getDeleted, 0)));
        vo.setClassroomCount(classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>().eq(Classroom::getDeleted, 0)));
        vo.setImageCount(classroomImageMapper.selectCount(new LambdaQueryWrapper<ClassroomImage>().eq(ClassroomImage::getDeleted, 0)));
        vo.setLostItemCount(lostItemMapper.selectCount(new LambdaQueryWrapper<LostItem>().eq(LostItem::getDeleted, 0)));
        vo.setRecoveredCount(lostItemMapper.selectCount(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getDeleted, 0).eq(LostItem::getStatus, "DONE")));
        return vo;
    }

    public List<DailyStat> dailyStats(int days) {
        List<DailyStat> list = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.dailyStats(Math.min(Math.max(days, 1), 60))) {
            DailyStat s = new DailyStat();
            Object date = row.get("date");
            s.setDate(date == null ? null : String.valueOf(date));
            s.setClassroomAdded(((Number) row.getOrDefault("classroom_added", 0)).longValue());
            s.setLostPublished(((Number) row.getOrDefault("lost_published", 0)).longValue());
            s.setAuditCount(((Number) row.getOrDefault("audit_count", 0)).longValue());
            list.add(s);
        }
        return list;
    }

    public List<ContributorRow> contributors() {
        List<ContributorRow> list = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.contributorRanking()) {
            ContributorRow r = new ContributorRow();
            r.setUserId(((Number) row.get("user_id")).longValue());
            r.setNickname((String) row.get("nickname"));
            r.setPhotoCount(((Number) row.getOrDefault("photo_count", 0)).longValue());
            r.setReportCount(((Number) row.getOrDefault("report_count", 0)).longValue());
            r.setReviewCount(((Number) row.getOrDefault("review_count", 0)).longValue());
            r.setTotal(((Number) row.getOrDefault("total", 0)).longValue());
            list.add(r);
        }
        return list;
    }

    // ---------- 系统配置 ----------

    public List<SysConfig> listConfigs() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigKey));
    }

    public void updateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> e : configs.entrySet()) {
            SysConfig exist = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, e.getKey()).last("LIMIT 1"));
            if (exist == null) {
                SysConfig c = new SysConfig();
                c.setConfigKey(e.getKey());
                c.setConfigValue(e.getValue());
                sysConfigMapper.insert(c);
            } else {
                exist.setConfigValue(e.getValue());
                sysConfigMapper.updateById(exist);
            }
        }
    }

    // ---------- 内部工具 ----------

    private AuditItemVO item(String type, Long id, Long submitter, String summary, String status, LocalDateTime t) {
        AuditItemVO vo = new AuditItemVO();
        vo.setTargetType(type);
        vo.setTargetId(id);
        vo.setSubmitterId(submitter);
        vo.setSummary(summary);
        vo.setAuditStatus(status);
        vo.setCreatedAt(t);
        return vo;
    }

    private UserAdminVO toUserVO(User u) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(u.getId());
        vo.setNickname(u.getNickname());
        vo.setOpenid(u.getOpenid());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setContributeCount(u.getContributeCount());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setLastLoginTime(u.getLastLoginTime());
        return vo;
    }

    private static String cell(List<String> row, int idx) {
        return idx < row.size() && row.get(idx) != null ? row.get(idx).trim() : "";
    }

    private static Integer intOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String int1(Integer v) {
        return v != null && v == 1 ? "是" : "否";
    }

    private List<List<String>> parseXlsx(byte[] data) {
        List<List<String>> rows = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (int i = 0; i < 18; i++) {
                    Cell cell = row.getCell(i);
                    cells.add(cell == null ? "" : switch (cell.getCellType()) {
                        case STRING -> cell.getStringCellValue();
                        case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                        default -> "";
                    });
                }
                rows.add(cells);
            }
        } catch (Exception e) {
            throw BizException.of(BizException.BAD_REQUEST, "xlsx 解析失败：" + e.getMessage());
        }
        return rows;
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            boolean inQuote = false;
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '"') {
                    inQuote = !inQuote;
                } else if (ch == ',' && !inQuote) {
                    cells.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
            cells.add(cur.toString());
            rows.add(cells);
        }
        return rows;
    }
}
