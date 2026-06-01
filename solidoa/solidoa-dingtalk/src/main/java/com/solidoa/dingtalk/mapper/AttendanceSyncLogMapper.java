package com.solidoa.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.dingtalk.entity.AttendanceSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import java.time.LocalDateTime;

/**
 * 考勤同步日志Mapper
 */
@Mapper
public interface AttendanceSyncLogMapper extends BaseMapper<AttendanceSyncLog> {

    /**
     * 插入同步日志
     */
    @Insert("INSERT INTO oa_attendance_sync_log (sync_type, sync_start_time, status, sync_params, retry_count) " +
            "VALUES (#{syncType}, #{syncStartTime}, 'RUNNING', #{syncParams}, 0)")
    Long insertSyncLog(AttendanceSyncLog log);

    /**
     * 更新同步成功状态
     */
    @Update("UPDATE oa_attendance_sync_log SET status = 'SUCCESS', sync_end_time = #{endTime}, " +
            "records_count = #{count}, update_time = NOW() WHERE id = #{id}")
    void updateSuccess(Long id, LocalDateTime endTime, Integer count);

    /**
     * 更新同步失败状态
     */
    @Update("UPDATE oa_attendance_sync_log SET status = 'FAILED', sync_end_time = #{endTime}, " +
            "error_message = #{errorMessage}, retry_count = retry_count + 1, update_time = NOW() WHERE id = #{id}")
    void updateFailed(Long id, LocalDateTime endTime, String errorMessage);

    /**
     * 获取最近的成功同步记录
     */
    @Select("SELECT * FROM oa_attendance_sync_log WHERE sync_type = #{syncType} AND status = 'SUCCESS' " +
            "ORDER BY create_time DESC LIMIT 1")
    AttendanceSyncLog selectLastSuccess(String syncType);
}