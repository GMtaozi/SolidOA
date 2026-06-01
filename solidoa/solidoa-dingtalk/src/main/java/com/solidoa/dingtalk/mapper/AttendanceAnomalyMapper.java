package com.solidoa.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.dingtalk.entity.AttendanceAnomaly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import java.time.LocalDate;
import java.util.List;

/**
 * 考勤异常Mapper
 */
@Mapper
public interface AttendanceAnomalyMapper extends BaseMapper<AttendanceAnomaly> {

    /**
     * 插入异常记录
     */
    @Insert("INSERT INTO oa_attendance_anomaly (user_id, anomaly_type, anomaly_date, description, severity, status) " +
            "VALUES (#{userId}, #{anomalyType}, #{anomalyDate}, #{description}, #{severity}, 'PENDING')")
    Long insertAnomaly(AttendanceAnomaly anomaly);

    /**
     * 查询用户指定月份的异常
     */
    @Select("SELECT * FROM oa_attendance_anomaly WHERE user_id = #{userId} " +
            "AND DATE_FORMAT(anomaly_date, '%Y-%m') = #{month} ORDER BY anomaly_date DESC")
    List<AttendanceAnomaly> selectByUserAndMonth(Long userId, String month);

    /**
     * 查询未处理的异常
     */
    @Select("SELECT * FROM oa_attendance_anomaly WHERE status = 'PENDING' ORDER BY severity DESC, create_time ASC")
    List<AttendanceAnomaly> selectPending();

    /**
     * 处理异常
     */
    @Update("UPDATE oa_attendance_anomaly SET status = #{status}, process_type = #{processType}, " +
            "process_reason = #{processReason}, process_time = NOW(), process_user_id = #{processUserId}, " +
            "update_time = NOW() WHERE id = #{id}")
    void processAnomaly(Long id, String status, String processType, String processReason, Long processUserId);

    /**
     * 检查指定日期是否已有异常
     */
    @Select("SELECT COUNT(*) FROM oa_attendance_anomaly WHERE user_id = #{userId} " +
            "AND anomaly_type = #{anomalyType} AND anomaly_date = #{anomalyDate}")
    int countByUserAndDateAndType(Long userId, LocalDate anomalyDate, String anomalyType);
}