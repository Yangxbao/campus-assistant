package com.campus.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.common.BizException;
import com.campus.assistant.common.XssUtil;
import com.campus.assistant.entity.ClassroomDomain.Classroom;
import com.campus.assistant.entity.CommunityDomain.Favorite;
import com.campus.assistant.entity.CommunityDomain.LostItem;
import com.campus.assistant.entity.CommunityDomain.Message;
import com.campus.assistant.entity.CommunityDomain.Report;
import com.campus.assistant.entity.UserDomain.SysConfig;
import com.campus.assistant.mapper.ClassroomMapper;
import com.campus.assistant.mapper.FavoriteMapper;
import com.campus.assistant.mapper.LostItemMapper;
import com.campus.assistant.mapper.MessageMapper;
import com.campus.assistant.mapper.ReportMapper;
import com.campus.assistant.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏、消息、举报、公告服务。
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final FavoriteMapper favoriteMapper;
    private final MessageMapper messageMapper;
    private final ReportMapper reportMapper;
    private final SysConfigMapper sysConfigMapper;
    private final ClassroomMapper classroomMapper;
    private final LostItemMapper lostItemMapper;

    // ---------- 收藏 ----------

    public Long addFavorite(long uid, String targetType, Long targetId) {
        if (!List.of("CLASSROOM", "LOST_ITEM").contains(targetType)) {
            throw BizException.of(BizException.BAD_REQUEST, "targetType 取值 CLASSROOM/LOST_ITEM");
        }
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId).last("LIMIT 1"));
        if (exist != null) {
            if (exist.getDeleted() == 0) {
                return exist.getId();
            }
            exist.setDeleted(0);
            favoriteMapper.updateById(exist);
            return exist.getId();
        }
        Favorite f = new Favorite();
        f.setUserId(uid);
        f.setTargetType(targetType);
        f.setTargetId(targetId);
        favoriteMapper.insert(f);
        return f.getId();
    }

    public void removeFavorite(long uid, String targetType, Long targetId) {
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId).last("LIMIT 1"));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
        }
    }

    public List<Map<String, Object>> listFavorites(long uid, String type) {
        List<Favorite> list = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid)
                .eq(type != null && !type.isBlank(), Favorite::getTargetType, type)
                .orderByDesc(Favorite::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite f : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("targetType", f.getTargetType());
            m.put("targetId", f.getTargetId());
            m.put("createdAt", f.getCreatedAt());
            if ("CLASSROOM".equals(f.getTargetType())) {
                Classroom c = classroomMapper.selectById(f.getTargetId());
                if (c != null && c.getDeleted() == 0) {
                    m.put("title", c.getName() == null ? c.getRoomNo() : c.getName() + " " + c.getRoomNo());
                } else {
                    continue;
                }
            } else {
                LostItem i = lostItemMapper.selectById(f.getTargetId());
                if (i != null && i.getDeleted() == 0) {
                    m.put("title", i.getTitle());
                } else {
                    continue;
                }
            }
            result.add(m);
        }
        return result;
    }

    // ---------- 消息 ----------

    public Page<Message> listMessages(long uid, int page, int size) {
        return messageMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 50)),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, uid)
                        .orderByDesc(Message::getCreatedAt));
    }

    public void readMessage(long uid, long id) {
        Message m = messageMapper.selectById(id);
        if (m == null || m.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "消息不存在");
        }
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    public void readAll(long uid) {
        messageMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, uid).eq(Message::getIsRead, 0).set(Message::getIsRead, 1));
    }

    public long unreadCount(long uid) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, uid).eq(Message::getIsRead, 0));
    }

    // ---------- 举报 ----------

    public Long report(long uid, String targetType, Long targetId, String reasonType, String reasonDesc) {
        if (!List.of("CLASSROOM_IMAGE", "LOST_ITEM", "REVIEW", "ROOM_STATUS", "USER").contains(targetType)) {
            throw BizException.of(BizException.BAD_REQUEST, "targetType 不合法");
        }
        if (!List.of("PORN", "AD", "PRIVACY", "FALSE", "OTHER").contains(reasonType)) {
            throw BizException.of(BizException.BAD_REQUEST, "reasonType 不合法");
        }
        Report r = new Report();
        r.setReporterId(uid);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReasonType(reasonType);
        r.setReasonDesc(XssUtil.escape(reasonDesc));
        r.setStatus("PENDING");
        reportMapper.insert(r);
        return r.getId();
    }

    // ---------- 公告 ----------

    public List<Map<String, Object>> announcements() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysConfig c : sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .likeRight(SysConfig::getConfigKey, "announcement:"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", c.getConfigKey());
            m.put("content", c.getConfigValue());
            m.put("updatedAt", c.getUpdatedAt());
            result.add(m);
        }
        SysConfig single = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "announcement").last("LIMIT 1"));
        if (single != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", "announcement");
            m.put("content", single.getConfigValue());
            m.put("updatedAt", single.getUpdatedAt());
            result.add(0, m);
        }
        return result;
    }
}
