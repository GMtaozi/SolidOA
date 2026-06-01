package com.solidoa.hr.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.finance.entity.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
    Budget selectByDeptAndMonth(@Param("deptId") Long deptId, @Param("year") Integer year, @Param("month") Integer month);

    /**
     * 使用悲观锁查询预算记录，防止并发修改
     * FOR UPDATE 会在事务期间锁定该行，直到事务提交或回滚
     */
    Budget selectByIdForUpdate(@Param("id") Long id);
}