package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.AttendanceGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤组Mapper
 */
@Mapper
public interface AttendanceGroupMapper extends BaseMapper<AttendanceGroup> {
}