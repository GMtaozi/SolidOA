package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.RepairForm;
import com.solidoa.hr.attendance.vo.RepairVO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

/**
 * 补卡服务接口
 */
public interface RepairCardService {

    /**
     * 新建补卡申请
     */
    Long create(RepairForm form, Long userId);

    /**
     * 补卡列表
     */
    PageVO<RepairVO> pageList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 补卡详情
     */
    RepairVO getById(Long id);

    /**
     * 审批补卡
     */
    void approve(Long id, String result, Long approverId);

    /**
     * 撤回补卡申请
     */
    void cancel(Long id, Long userId);

    /**
     * 获取补卡统计
     */
    Object getStatistics(Long userId, String yearMonth);

    /**
     * 获取待审批列表
     */
    List<RepairVO> getPendingList();
}