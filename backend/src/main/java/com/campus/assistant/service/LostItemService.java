package com.campus.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.assistant.common.BizException;
import com.campus.assistant.common.RedisHelper;
import com.campus.assistant.common.XssUtil;
import com.campus.assistant.dto.LostDto.ClaimVO;
import com.campus.assistant.dto.LostDto.LostItemReq;
import com.campus.assistant.dto.LostDto.LostItemVO;
import com.campus.assistant.dto.LostDto.MatchVO;
import com.campus.assistant.entity.ClassroomDomain.Building;
import com.campus.assistant.entity.CommunityDomain.Claim;
import com.campus.assistant.entity.CommunityDomain.LostItem;
import com.campus.assistant.entity.UserDomain.User;
import com.campus.assistant.mapper.BuildingMapper;
import com.campus.assistant.mapper.ClaimMapper;
import com.campus.assistant.mapper.ItemImageMapper;
import com.campus.assistant.mapper.LostItemMapper;
import com.campus.assistant.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 失物招领服务：发布/列表/详情/认领/自动匹配（纯规则实现，不冒充 AI）/状态流转。
 */
@Service
@RequiredArgsConstructor
public class LostItemService {

    private static final Set<String> CATEGORIES = Set.of(
            "证件", "卡类", "电子产品", "书籍", "钥匙", "雨伞", "衣物", "首饰", "现金", "其他");

    private final LostItemMapper lostItemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ClaimMapper claimMapper;
    private final BuildingMapper buildingMapper;
    private final UserMapper userMapper;
    private final RedisHelper redis;
    private final MessageService messageService;

    @Value("${campus.lost.expire-days:30}")
    private int expireDays;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------- 发布 / 查询 ----------

    public Long publish(long uid, LostItemReq req) {
        if (redis.rateLimited("rate:publish:" + uid, 10, Duration.ofDays(1))) {
            throw BizException.of(BizException.RATE_LIMITED, "今日发布次数已达上限");
        }
        if (req.getKind() == null || !List.of("LOST", "FOUND").contains(req.getKind())) {
            throw BizException.of(BizException.BAD_REQUEST, "kind 取值 LOST/FOUND");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "标题不能为空");
        }
        if (req.getCategory() == null || !CATEGORIES.contains(req.getCategory())) {
            throw BizException.of(BizException.BAD_REQUEST, "物品类别不合法");
        }
        LostItem item = new LostItem();
        item.setUserId(uid);
        item.setKind(req.getKind());
        item.setTitle(XssUtil.escape(req.getTitle()));
        item.setCategory(req.getCategory());
        item.setDescription(XssUtil.escape(req.getDescription()));
        if (req.getLostTime() != null && !req.getLostTime().isBlank()) {
            try {
                item.setLostTime(LocalDateTime.parse(req.getLostTime(), DT));
            } catch (Exception e) {
                throw BizException.of(BizException.BAD_REQUEST, "lostTime 格式应为 yyyy-MM-dd HH:mm:ss");
            }
        }
        item.setBuildingId(req.getBuildingId());
        item.setLocationDesc(XssUtil.escape(req.getLocationDesc()));
        item.setContactVisible(req.getContactVisible() == null ? 0 : req.getContactVisible());
        item.setContactValue(req.getContactValue());
        item.setStatus("OPEN");
        item.setAuditStatus("PENDING");
        item.setExpireAt(LocalDateTime.now().plusDays(expireDays));
        item.setViewCount(0);
        lostItemMapper.insert(item);
        if (req.getOssKeys() != null) {
            for (String key : req.getOssKeys()) {
                com.campus.assistant.entity.CommunityDomain.ItemImage img =
                        new com.campus.assistant.entity.CommunityDomain.ItemImage();
                img.setLostItemId(item.getId());
                img.setOssKey(key);
                itemImageMapper.insert(img);
            }
        }
        return item.getId();
    }

    public Page<LostItemVO> list(String kind, String category, Long buildingId, String keyword,
                                 String status, Integer mine, long uid, int page, int size) {
        LambdaQueryWrapper<LostItem> w = new LambdaQueryWrapper<>();
        if (mine != null && mine == 1) {
            w.eq(LostItem::getUserId, uid);
        } else {
            // 公开检索仅审核通过项
            w.eq(LostItem::getAuditStatus, "APPROVED");
        }
        w.eq(kind != null && !kind.isBlank(), LostItem::getKind, kind);
        w.eq(category != null && !category.isBlank(), LostItem::getCategory, category);
        w.eq(buildingId != null, LostItem::getBuildingId, buildingId);
        w.eq(status != null && !status.isBlank(), LostItem::getStatus, status);
        w.like(keyword != null && !keyword.isBlank(), LostItem::getTitle, keyword)
                .or().like(keyword != null && !keyword.isBlank(), LostItem::getDescription, keyword);
        w.eq(LostItem::getDeleted, 0);
        w.orderByDesc(LostItem::getCreatedAt);
        Page<LostItem> p = lostItemMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 50)), w);
        Page<LostItemVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(i -> toVO(i, uid, false)).toList());
        return result;
    }

    public LostItemVO detail(long uid, long id) {
        LostItem item = lostItemMapper.selectById(id);
        if (item == null || item.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "失物信息不存在");
        }
        if (!"APPROVED".equals(item.getAuditStatus()) && item.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "失物信息不存在");
        }
        lostItemMapper.update(null, new LambdaUpdateWrapper<LostItem>()
                .eq(LostItem::getId, id).setSql("view_count = view_count + 1"));
        item.setViewCount(item.getViewCount() == null ? 1 : item.getViewCount() + 1);
        return toVO(item, uid, true);
    }

    public void update(long uid, long id, LostItemReq req) {
        LostItem item = mustOwn(uid, id);
        if (req.getTitle() != null) {
            item.setTitle(XssUtil.escape(req.getTitle()));
        }
        if (req.getDescription() != null) {
            item.setDescription(XssUtil.escape(req.getDescription()));
        }
        if (req.getCategory() != null && CATEGORIES.contains(req.getCategory())) {
            item.setCategory(req.getCategory());
        }
        if (req.getBuildingId() != null) {
            item.setBuildingId(req.getBuildingId());
        }
        if (req.getLocationDesc() != null) {
            item.setLocationDesc(XssUtil.escape(req.getLocationDesc()));
        }
        if (req.getContactVisible() != null) {
            item.setContactVisible(req.getContactVisible());
        }
        if (req.getContactValue() != null) {
            item.setContactValue(req.getContactValue());
        }
        lostItemMapper.updateById(item);
    }

    public void delete(long uid, long id) {
        mustOwn(uid, id);
        lostItemMapper.deleteById(id);
    }

    /** 本人标记 DONE/CLOSED */
    public void markStatus(long uid, long id, String status) {
        LostItem item = mustOwn(uid, id);
        if (!List.of("DONE", "CLOSED").contains(status)) {
            throw BizException.of(BizException.BAD_REQUEST, "仅允许标记 DONE/CLOSED");
        }
        item.setStatus(status);
        lostItemMapper.updateById(item);
    }

    // ---------- 认领 ----------

    public Long applyClaim(long uid, long itemId, String proofDesc, String contact) {
        LostItem item = lostItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "失物信息不存在");
        }
        if (item.getUserId() == uid) {
            throw BizException.of(BizException.BAD_REQUEST, "不能认领自己发布的信息");
        }
        if (proofDesc == null || proofDesc.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "物品特征证明不能为空");
        }
        Claim c = new Claim();
        c.setLostItemId(itemId);
        c.setClaimantId(uid);
        c.setProofDesc(XssUtil.escape(proofDesc));
        c.setContact(contact);
        c.setStatus("PENDING");
        claimMapper.insert(c);
        // 通知发布者
        messageService.send(item.getUserId(), "CLAIM", "有人申请认领您发布的失物",
                "「" + item.getTitle() + "」收到新的认领申请，请前往查看。", "LOST_ITEM", itemId);
        return c.getId();
    }

    public List<ClaimVO> listClaims(long uid, long itemId) {
        LostItem item = lostItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1 || item.getUserId() != uid) {
            throw BizException.of(BizException.FORBIDDEN, "仅发布者可查看认领申请");
        }
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                        .eq(Claim::getLostItemId, itemId).orderByDesc(Claim::getCreatedAt))
                .stream().map(this::toClaimVO).toList();
    }

    /** 发布者接受/拒绝认领，接受后发站内消息给认领人 */
    public void handleClaim(long uid, long claimId, String action, String remark) {
        Claim c = claimMapper.selectById(claimId);
        if (c == null || c.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "认领记录不存在");
        }
        LostItem item = lostItemMapper.selectById(c.getLostItemId());
        if (item == null || item.getUserId() != uid) {
            throw BizException.of(BizException.FORBIDDEN, "仅发布者可处理认领申请");
        }
        if (!List.of("ACCEPTED", "REJECTED").contains(action)) {
            throw BizException.of(BizException.BAD_REQUEST, "action 取值 ACCEPTED/REJECTED");
        }
        c.setStatus(action);
        c.setHandlerId(uid);
        c.setHandleTime(LocalDateTime.now());
        c.setHandleRemark(remark);
        claimMapper.updateById(c);
        if ("ACCEPTED".equals(action)) {
            item.setStatus("DONE");
            lostItemMapper.updateById(item);
        }
        messageService.send(c.getClaimantId(), "CLAIM",
                "ACCEPTED".equals(action) ? "您的认领申请已被接受" : "您的认领申请被拒绝",
                "「" + item.getTitle() + "」认领申请处理结果：" + action
                        + (remark == null || remark.isBlank() ? "" : "，备注：" + remark),
                "LOST_ITEM", item.getId());
    }

    // ---------- 自动匹配（纯规则） ----------

    public List<MatchVO> matches(long uid, long itemId) {
        LostItem item = lostItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw BizException.of(BizException.NOT_FOUND, "失物信息不存在");
        }
        String opposite = "LOST".equals(item.getKind()) ? "FOUND" : "LOST";
        List<LostItem> candidates = lostItemMapper.selectList(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getKind, opposite)
                .eq(LostItem::getCategory, item.getCategory())
                .eq(LostItem::getAuditStatus, "APPROVED")
                .in(LostItem::getStatus, List.of("OPEN", "MATCHED"))
                .ne(LostItem::getUserId, item.getUserId())
                .ne(LostItem::getId, itemId)
                .eq(LostItem::getDeleted, 0));
        List<MatchVO> result = new ArrayList<>();
        for (LostItem cand : candidates) {
            List<String> reasons = new ArrayList<>();
            int score = 0;
            // 时间窗 30 天
            LocalDateTime a = item.getLostTime() == null ? item.getCreatedAt() : item.getLostTime();
            LocalDateTime b = cand.getLostTime() == null ? cand.getCreatedAt() : cand.getLostTime();
            if (a != null && b != null && !a.isBefore(b.minusDays(30)) && !a.isAfter(b.plusDays(30))) {
                reasons.add("时间相近（30 天内）");
                score += 30;
            }
            // 地点相近
            if (item.getBuildingId() != null && item.getBuildingId().equals(cand.getBuildingId())) {
                reasons.add("同一楼宇");
                score += 30;
            } else if (sameLocationText(item.getLocationDesc(), cand.getLocationDesc())) {
                reasons.add("地点描述相近");
                score += 15;
            }
            // 关键词重合（标题/描述分词重合 ≥1）
            int overlap = keywordOverlap(item.getTitle() + " " + nullToEmpty(item.getDescription()),
                    cand.getTitle() + " " + nullToEmpty(cand.getDescription()));
            if (overlap > 0) {
                reasons.add("关键词重合 " + overlap + " 个");
                score += Math.min(overlap * 10, 30);
            }
            if (score >= 40) {
                result.add(toMatchVO(cand, reasons, score));
            }
        }
        result.sort((x, y) -> y.getScore() - x.getScore());
        return result;
    }

    /** 与我发布的方向相反的疑似匹配（首页/消息流） */
    public List<MatchVO> matchFeeds(long uid) {
        List<LostItem> mine = lostItemMapper.selectList(new LambdaQueryWrapper<LostItem>()
                .eq(LostItem::getUserId, uid)
                .in(LostItem::getStatus, List.of("OPEN", "MATCHED"))
                .eq(LostItem::getDeleted, 0)
                .orderByDesc(LostItem::getCreatedAt).last("LIMIT 20"));
        List<MatchVO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (LostItem item : mine) {
            for (MatchVO m : matches(uid, item.getId())) {
                if (seen.add(m.getId())) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    // ---------- 内部工具 ----------

    private LostItem mustOwn(long uid, long id) {
        LostItem item = lostItemMapper.selectById(id);
        if (item == null || item.getDeleted() == 1 || item.getUserId() != uid) {
            throw BizException.of(BizException.NOT_FOUND, "失物信息不存在或无权操作");
        }
        return item;
    }

    private LostItemVO toVO(LostItem i, long uid, boolean withContact) {
        LostItemVO vo = new LostItemVO();
        vo.setId(i.getId());
        vo.setUserId(i.getUserId());
        User owner = userMapper.selectById(i.getUserId());
        vo.setNickname(owner == null ? null : owner.getNickname());
        vo.setKind(i.getKind());
        vo.setTitle(i.getTitle());
        vo.setCategory(i.getCategory());
        vo.setDescription(i.getDescription());
        vo.setLostTime(i.getLostTime());
        vo.setBuildingId(i.getBuildingId());
        if (i.getBuildingId() != null) {
            Building b = buildingMapper.selectById(i.getBuildingId());
            vo.setBuildingName(b == null ? null : b.getName());
        }
        vo.setLocationDesc(i.getLocationDesc());
        vo.setContactVisible(i.getContactVisible());
        vo.setStatus(i.getStatus());
        vo.setViewCount(i.getViewCount());
        vo.setAuditStatus(i.getAuditStatus());
        vo.setCreatedAt(i.getCreatedAt());
        // 联系方式：仅发布者可见；公开时前端自行提示脱敏
        boolean isOwner = i.getUserId() != null && i.getUserId() == uid;
        if (isOwner) {
            vo.setContactValue(i.getContactValue());
        } else if (i.getContactVisible() != null && i.getContactVisible() == 1) {
            vo.setContactValue(i.getContactValue());
        }
        vo.setImages(itemImageMapper.selectList(new LambdaQueryWrapper<com.campus.assistant.entity.CommunityDomain.ItemImage>()
                        .eq(com.campus.assistant.entity.CommunityDomain.ItemImage::getLostItemId, i.getId()))
                .stream().map(img -> img.getOssKey()).toList());
        return vo;
    }

    private ClaimVO toClaimVO(Claim c) {
        ClaimVO vo = new ClaimVO();
        vo.setId(c.getId());
        vo.setLostItemId(c.getLostItemId());
        vo.setClaimantId(c.getClaimantId());
        User u = userMapper.selectById(c.getClaimantId());
        vo.setClaimantNickname(u == null ? null : u.getNickname());
        vo.setProofDesc(c.getProofDesc());
        vo.setContact(c.getContact());
        vo.setStatus(c.getStatus());
        vo.setHandleRemark(c.getHandleRemark());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    private MatchVO toMatchVO(LostItem i, List<String> reasons, int score) {
        MatchVO vo = new MatchVO();
        vo.setId(i.getId());
        vo.setKind(i.getKind());
        vo.setTitle(i.getTitle());
        vo.setCategory(i.getCategory());
        vo.setDescription(i.getDescription());
        vo.setLostTime(i.getLostTime());
        vo.setLocationDesc(i.getLocationDesc());
        if (i.getBuildingId() != null) {
            Building b = buildingMapper.selectById(i.getBuildingId());
            vo.setBuildingName(b == null ? null : b.getName());
        }
        vo.setImages(itemImageMapper.selectList(new LambdaQueryWrapper<com.campus.assistant.entity.CommunityDomain.ItemImage>()
                        .eq(com.campus.assistant.entity.CommunityDomain.ItemImage::getLostItemId, i.getId()))
                .stream().map(img -> img.getOssKey()).toList());
        vo.setMatchReasons(reasons);
        vo.setScore(score);
        return vo;
    }

    /** 简单 2-gram 关键词重合计数（标题/描述），规则实现 */
    static int keywordOverlap(String textA, String textB) {
        Set<String> gramsA = bigrams(textA);
        Set<String> gramsB = bigrams(textB);
        gramsA.retainAll(gramsB);
        return gramsA.size();
    }

    private static Set<String> bigrams(String text) {
        Set<String> set = new HashSet<>();
        String t = text == null ? "" : text.replaceAll("\\s+", "");
        for (int i = 0; i + 2 <= t.length(); i++) {
            set.add(t.substring(i, i + 2));
        }
        return set;
    }

    private boolean sameLocationText(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
