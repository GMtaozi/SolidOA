package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.workflow.entity.ApprovalRecord;
import com.solidoa.workflow.vo.ApprovalRecordVO;
import com.solidoa.workflow.vo.TaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 审批记录Mapper
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    /**
     * 根据业务类型和ID查询审批记录
     */
    List<ApprovalRecord> selectByBusiness(@Param("businessType") String businessType,
                                          @Param("businessId") Long businessId);

    /**
     * 查询已处理任务列表
     */
    List<TaskVO> selectProcessedTaskList(@Param("approverId") Long userId);

    /**
     * 分页查询已处理任务列表
     * 采用手动分页（offset/limit），因该查询涉及多表关联，
     * MyBatis-Plus Page 对象在复杂 SQL 中分页计数不准确，故手动处理
     */
    List<TaskVO> selectProcessedTaskListPage(@Param("approverId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 统计已处理任务数量
     */
    long countProcessedTaskList(@Param("approverId") Long userId);

    /**
     * 查询我发起的申请列表
     */
    IPage<ApprovalRecordVO> selectMyApply(Page<?> page,
                                          @Param("userId") Long userId,
                                          @Param("businessType") String businessType,
                                          @Param("status") String status,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    /**
     * 查询我审批过的申请列表
     */
    IPage<ApprovalRecordVO> selectMyApproved(Page<?> page,
                                              @Param("userId") Long userId,
                                              @Param("businessType") String businessType,
                                              @Param("status") String status,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    /**
     * 查询全部审批记录(管理员)
     */
    IPage<ApprovalRecordVO> selectAllRecord(Page<?> page,
                                             @Param("businessType") String businessType,
                                             @Param("status") String status,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("userId") Long userId,
                                             @Param("deptId") Long deptId);

    /**
     * 统计我发起的申请数量
     */
    long countMyApply(@Param("userId") Long userId,
                      @Param("businessType") String businessType,
                      @Param("status") String status);

    /**
     * 统计我审批过的申请数量
     */
    long countMyApproved(@Param("userId") Long userId,
                         @Param("businessType") String businessType,
                         @Param("status") String status);

    /**
     * 查询当日最大审批编号
     */
    String selectMaxRecordNoByPrefix(@Param("prefix") String prefix);

    /**
     * 查询当日最大审批编号（使用 FOR UPDATE 行锁保证并发唯一性）
     */
    @Select("SELECT record_no FROM oa_approval_record WHERE record_no LIKE #{prefix} ORDER BY record_no DESC LIMIT 1 FOR UPDATE")
    String selectMaxRecordNoByPrefixForUpdate(@Param("prefix") String prefix);
}