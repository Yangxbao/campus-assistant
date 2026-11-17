package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.assistant.entity.UserDomain;
import com.campus.assistant.entity.UserDomain.WxUserSession;

/** WxUserSessionMapper：MyBatis-Plus 通用 Mapper（#{} 预编译参数绑定，防 SQL 注入）。 */
public interface WxUserSessionMapper extends BaseMapper<UserDomain.WxUserSession> {
}
