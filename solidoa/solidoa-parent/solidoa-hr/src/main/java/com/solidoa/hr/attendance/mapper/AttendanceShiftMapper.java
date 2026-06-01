package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.AttendanceShift;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班次配置Mapper
 */
@Mapper
public interface AttendanceShiftMapper extends BaseMapper<AttendanceShift> {
}