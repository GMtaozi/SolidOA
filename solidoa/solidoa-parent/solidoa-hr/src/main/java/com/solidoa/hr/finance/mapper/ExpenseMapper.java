package com.solidoa.hr.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.finance.vo.ExpenseVO;
import com.solidoa.hr.finance.vo.DeptExpenseReportVO;
import com.solidoa.hr.finance.entity.Expense;
import com.solidoa.hr.finance.entity.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {

    ExpenseVO selectVOById(@Param("id") Long id);

    List<ExpenseVO> selectPageList(@Param("offset") int offset,
                                    @Param("limit") int limit,
                                    @Param("userId") Long userId,
                                    @Param("status") String status);

    long selectCount(@Param("userId") Long userId, @Param("status") String status);

    List<ExpenseVO> selectStatistics(@Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    Budget selectBudgetForUpdate(@Param("deptId") Long deptId,
                                  @Param("year") Integer year,
                                  @Param("month") Integer month);

    List<DeptExpenseReportVO> selectDeptExpenseReport(@Param("year") Integer year, @Param("month") Integer month);

    List<ExpenseVO> selectExportPage(@Param("offset") int offset, @Param("limit") int limit,
                                      @Param("year") Integer year, @Param("month") Integer month);

    long selectExportCount(@Param("year") Integer year, @Param("month") Integer month);
}