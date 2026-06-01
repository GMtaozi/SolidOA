package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.Expense;
import com.solidoa.workflow.vo.ExpenseVO;
import com.solidoa.workflow.vo.TaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {
    List<ExpenseVO> selectPageList(@Param("userId") Long userId,
                                   @Param("status") String status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long selectCount(@Param("userId") Long userId, @Param("status") String status);

    Expense selectExpenseById(@Param("id") Long id);

    List<TaskVO> selectMyTaskList(@Param("userId") Long userId, @Param("businessType") String businessType);

    List<TaskVO> selectMyTaskListPage(@Param("userId") Long userId, @Param("businessType") String businessType,
                                      @Param("offset") int offset, @Param("limit") int limit);

    long countMyTaskList(@Param("userId") Long userId, @Param("businessType") String businessType);

    List<TaskVO> selectPendingTaskList(@Param("approverId") Long approverId);

    List<TaskVO> selectPendingTaskListPage(@Param("approverId") Long approverId,
                                           @Param("offset") int offset, @Param("limit") int limit);

    long countPendingTaskList(@Param("approverId") Long approverId);

    String selectMaxExpenseNoByDate(@Param("prefix") String prefix);

    /**
     * 查询最大报销单号（使用 FOR UPDATE 行锁保证并发唯一性）
     */
    @Select("SELECT expense_no FROM oa_expense WHERE expense_no LIKE #{prefix} ORDER BY expense_no DESC LIMIT 1 FOR UPDATE")
    String selectMaxExpenseNoByDateForUpdate(@Param("prefix") String prefix);
}