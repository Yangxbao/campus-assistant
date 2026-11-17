package com.campus.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.assistant.entity.TimetableDomain;
import com.campus.assistant.entity.TimetableDomain.Semester;

/** SemesterMapper：MyBatis-Plus 通用 Mapper（#{} 预编译参数绑定，防 SQL 注入）。 */
public interface SemesterMapper extends BaseMapper<TimetableDomain.Semester> {
}
