package com.solidoa.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.workflow.vo.*;

/**
 * 审批记录服务接口
 */
public interface ApprovalRecordService {

    /**
     * 获取我发起的申请列表
     */
    IPage<ApprovalRecordVO> getMyApply(int pageNum, int pageSize, String businessType, String status,
                                        String startDate, String endDate, Long userId);

    /**
     * 获取我审批过的申请列表
     */
    IPage<ApprovalRecordVO> getMyApproved(int pageNum, int pageSize, String businessType, String status,
                                           String startDate, String endDate, Long userId);

    /**
     * 获取全部审批记录(管理员)
     */
    IPage<ApprovalRecordVO> getAllRecord(int pageNum, int pageSize, String businessType, String status,
                                          String startDate, String endDate, Long userId, Long deptId);

    /**
     * 获取审批记录详情
     */
    ApprovalRecordDetailVO getRecordDetail(Long id);

    /**
     * 获取审批统计
     */
    ApprovalStatisticsVO getStatistics(String startDate, String endDate, Long deptId, Long userId);

    /**
     * 导出审批记录
     */
    void exportRecord(String businessType, String status, String startDate, String endDate,
                     Long deptId, jakarta.servlet.http.HttpServletResponse response);

    /**
     * 创建审批记录
     */
    Long createRecord(String businessType, Long businessId, Long userId, String userName,
                      Long deptId, String deptName, String title, String content,
                      java.math.BigDecimal amount, java.time.LocalDate startDate,
                      java.time.LocalDate endDate);
}