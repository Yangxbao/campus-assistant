package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.assistant.entity.CommunityDomain;
import com.campus.assistant.entity.CommunityDomain.AuditRecord;

/** AuditRecordMapper：MyBatis-Plus 通用 Mapper（#{} 预编译参数绑定，防 SQL 注入）。 */
public interface AuditRecordMapper extends BaseMapper<CommunityDomain.AuditRecord> {
}
