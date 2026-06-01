package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.ApproveForm;
import com.solidoa.hr.attendance.form.OvertimeForm;
import com.solidoa.hr.attendance.vo.OvertimeBalanceVO;
import com.solidoa.hr.attendance.vo.OvertimeVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.time.LocalDate;

/**
 * 加班服务接口
 */
public interface OvertimeService {

    /**
     * 新建加班申请
     */
    Long createOvertime(OvertimeForm form, Long userId);

    /**
     * 加班列表
     */
    PageVO<OvertimeVO> listOvertime(PageDTO dto, String status, String overtimeType,
                                    LocalDate startDate, LocalDate endDate);

    /**
     * 加班详情
     */
    OvertimeVO getOvertimeById(Long id, Long userId);

    /**
     * 审批加班
     */
    void approveOvertime(Long id, ApproveForm form, Long approverId);

    /**
     * 撤回加班申请
     */
    void cancelOvertime(Long id, Long userId);

    /**
     * 查询调休余额
     */
    OvertimeBalanceVO getOvertimeBalance(Long userId);
}