package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.AttendanceRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤规则Mapper
 */
@Mapper
public interface AttendanceRuleMapper extends BaseMapper<AttendanceRule> {
}