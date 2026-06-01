package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.RepairCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairCardMapper extends BaseMapper<RepairCard> {

    /**
     * 查询待审批列表
     */
    List<RepairCard> selectPendingList(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 检查重复补卡
     */
    int countDuplicateRepair(@Param("userId") Long userId,
                           @Param("repairDate") String repairDate,
                           @Param("repairType") String repairType);

    /**
     * 带行锁的重复检查，用于防止并发重复提交
     */
    int checkAndLockForCreate(@Param("userId") Long userId,
                              @Param("repairDate") String repairDate,
                              @Param("repairType") String repairType);

    /**
     * 按日期范围统计补卡次数
     */
    int countByDateRange(@Param("userId") Long userId,
                        @Param("startDate") String startDate,
                        @Param("endDate") String endDate);
}