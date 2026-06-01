package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.Leave;
import com.solidoa.workflow.vo.LeaveVO;
import com.solidoa.workflow.vo.TaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface LeaveMapper extends BaseMapper<Leave> {

    Leave selectLeaveById(@Param("id") Long id);

    LeaveVO selectVOById(@Param("id") Long id);

    List<LeaveVO> selectPageList(@Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("userId") Long userId,
                                  @Param("status") String status);

    long selectCount(@Param("userId") Long userId, @Param("status") String status);

    List<TaskVO> selectMyTaskList(@Param("userId") Long userId, @Param("businessType") String businessType);

    List<TaskVO> selectMyTaskListPage(@Param("userId") Long userId, @Param("businessType") String businessType,
                                      @Param("offset") int offset, @Param("limit") int limit);

    long countMyTaskList(@Param("userId") Long userId, @Param("businessType") String businessType);

    List<TaskVO> selectPendingTaskList(@Param("approverId") Long approverId);

    List<TaskVO> selectPendingTaskListPage(@Param("approverId") Long approverId,
                                           @Param("offset") int offset, @Param("limit") int limit);

    long countPendingTaskList(@Param("approverId") Long approverId);

    /**
     * 查询最大请假单号
     */
    String selectMaxLeaveNoByDate(@Param("pattern") String pattern);

    /**
     * 查询最大请假单号（使用 FOR UPDATE 行锁保证并发唯一性）
     */
    @Select("SELECT leave_no FROM oa_leave WHERE leave_no LIKE #{pattern} ORDER BY leave_no DESC LIMIT 1 FOR UPDATE")
    String selectMaxLeaveNoByDateForUpdate(@Param("pattern") String pattern);
}