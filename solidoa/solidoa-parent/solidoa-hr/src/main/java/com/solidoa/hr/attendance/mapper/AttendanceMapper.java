package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.Attendance;
import com.solidoa.hr.attendance.vo.AttendanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    Attendance selectLastRecord(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);

    /**
     * 手动分页查询，因MyBatis-Plus的Page在复杂多表联查场景下性能不佳，
     * 且此处需要精确控制SQL以支持按userId/checkDate等条件过滤后分页
     */
    List<AttendanceVO> selectPageList(@Param("offset") int offset,
                                      @Param("limit") int limit,
                                      @Param("userId") Long userId,
                                      @Param("checkDate") LocalDate checkDate);

    long selectCount(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);

    List<Attendance> selectByMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    int countDeviceUsersToday(@Param("deviceId") String deviceId, @Param("checkDate") LocalDate checkDate);

    /**
     * 按日期范围查询打卡记录
     */
    List<Attendance> selectByDateRange(@Param("userId") Long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * 查询所有不同的用户ID
     */
    List<Long> selectDistinctUserIds();
}