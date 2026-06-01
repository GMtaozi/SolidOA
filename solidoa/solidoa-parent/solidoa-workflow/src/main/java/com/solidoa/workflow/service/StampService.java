package com.solidoa.workflow.service;

import com.solidoa.workflow.form.StampForm;
import com.solidoa.workflow.form.StampRecordForm;
import com.solidoa.workflow.form.ApproveForm;
import com.solidoa.workflow.vo.StampVO;
import com.solidoa.workflow.vo.StampStatisticsVO;
import java.util.List;

/**
 * 用印申请服务接口
 */
public interface StampService {

    /**
     * 创建用印申请
     */
    Long createStamp(StampForm form, Long userId);

    /**
     * 用印申请列表
     */
    List<StampVO> listStamp(Long userId, String status);

    /**
     * 用印申请详情
     */
    StampVO getStampById(Long id, Long userId);

    /**
     * 审批用印申请
     */
    void approveStamp(Long id, ApproveForm form, Long approverId);

    /**
     * 撤回用印申请
     */
    void cancelStamp(Long id, Long userId);

    /**
     * 物理用印登记
     */
    void recordStamp(Long id, StampRecordForm form, Long operatorId);

    /**
     * 获取用印统计
     */
    StampStatisticsVO getStatistics(Long deptId);
}