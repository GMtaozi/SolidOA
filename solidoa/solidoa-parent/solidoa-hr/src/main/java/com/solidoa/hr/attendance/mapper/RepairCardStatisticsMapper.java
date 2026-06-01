package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.RepairCardStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 补卡次数统计Mapper
 */
@Mapper
public interface RepairCardStatisticsMapper extends BaseMapper<RepairCardStatistics> {

    /**
     * 根据用户ID和月份查询统计
     */
    RepairCardStatistics selectByUserAndMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    /**
     * 更新或插入统计记录
     */
    void upsert(@Param("userId") Long userId, @Param("yearMonth") String yearMonth, @Param("repairCount") Integer repairCount);
}