package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.AttendanceSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttendanceSummaryMapper extends BaseMapper<AttendanceSummary> {
    AttendanceSummary selectByUserAndMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}