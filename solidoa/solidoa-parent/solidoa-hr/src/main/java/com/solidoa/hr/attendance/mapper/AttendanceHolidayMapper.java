package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.AttendanceHoliday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * 节假日配置Mapper
 */
@Mapper
public interface AttendanceHolidayMapper extends BaseMapper<AttendanceHoliday> {

    /**
     * 查询某年节假日列表
     */
    List<AttendanceHoliday> selectByYear(@Param("year") Integer year);

    /**
     * 查询指定日期范围内的节假日
     */
    List<AttendanceHoliday> selectByDateRange(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * 查询调休上班日
     */
    List<AttendanceHoliday> selectWorkdays(@Param("year") Integer year);

    /**
     * 查询节假日
     */
    List<AttendanceHoliday> selectHolidays(@Param("year") Integer year);
}