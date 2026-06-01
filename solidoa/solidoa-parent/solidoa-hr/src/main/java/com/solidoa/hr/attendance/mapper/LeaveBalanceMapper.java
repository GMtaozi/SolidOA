package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.LeaveBalance;
import com.solidoa.hr.attendance.vo.LeaveBalanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 员工假期余额Mapper
 */
@Mapper
public interface LeaveBalanceMapper extends BaseMapper<LeaveBalance> {

    /**
     * 查询用户的假期余额列表
     */
    List<LeaveBalanceVO> selectByUserId(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 查询用户的指定假期类型余额
     */
    LeaveBalance selectByUserIdAndType(@Param("userId") Long userId,
                                        @Param("leaveType") String leaveType,
                                        @Param("year") Integer year);

    /**
     * 查询用户的年假天数（用于计算初始年假）
     */
    Integer selectWorkYears(@Param("userId") Long userId);

    /**
     * 批量插入或更新假期余额
     */
    void insertOrUpdateBatch(@Param("list") List<LeaveBalance> balances);
}