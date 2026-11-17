package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.assistant.entity.ClassroomDomain;
import com.campus.assistant.entity.ClassroomDomain.Floor;

/** FloorMapper：MyBatis-Plus 通用 Mapper（#{} 预编译参数绑定，防 SQL 注入）。 */
public interface FloorMapper extends BaseMapper<ClassroomDomain.Floor> {
}
