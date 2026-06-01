package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.GoOutForm;
import com.solidoa.hr.attendance.vo.GoOutVO;
import com.solidoa.common.vo.PageVO;

/**
 * 外出申请服务接口
 */
public interface GoOutService {

    /**
     * 新建外出申请
     */
    Long create(GoOutForm form, Long userId);

    /**
     * 外出列表
     */
    PageVO<GoOutVO> pageList(Long userId, int pageNum, int pageSize);

    /**
     * 外出详情（带权限校验）
     */
    GoOutVO getById(Long id, Long userId);

    /**
     * 审批外出申请
     */
    void approve(Long id, String result, Long approverId);

    /**
     * 撤回外出申请
     */
    void cancel(Long id, Long userId);
}