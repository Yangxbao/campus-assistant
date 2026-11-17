package com.campus.assistant.service;

import com.campus.assistant.entity.CommunityDomain.Message;
import com.campus.assistant.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站内消息发送服务：供审核流、失物认领、匹配提醒等模块复用。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public void send(Long userId, String type, String title, String content,
                     String relatedType, Long relatedId) {
        Message m = new Message();
        m.setUserId(userId);
        m.setType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setRelatedType(relatedType);
        m.setRelatedId(relatedId);
        m.setIsRead(0);
        messageMapper.insert(m);
    }

    public long unreadCount(long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId).eq(Message::getIsRead, 0));
    }
}
