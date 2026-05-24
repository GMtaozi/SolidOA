package com.solidoa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.attendance.entity.Attendance;
import com.solidoa.attendance.vo.AttendanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    Attendance selectLastRecord(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);

    List<AttendanceVO> selectPageList(@Param("offset") int offset,
                                      @Param("limit") int limit,
                                      @Param("userId") Long userId,
                                      @Param("checkDate") LocalDate checkDate);

    long selectCount(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);

    List<Attendance> selectByMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}