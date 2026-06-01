package com.solidoa.workflow.service;

import com.solidoa.workflow.form.LeaveForm;
import com.solidoa.workflow.vo.LeaveVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;

public interface LeaveService {
    Long createLeave(LeaveForm form, Long userId);
    PageVO<LeaveVO> pageList(PageDTO dto, Long userId, String status);
    LeaveVO getById(Long id, Long userId);
    LeaveVO getByIdSimple(Long id);
    void approve(Long id, String approveResult, String comment, Long approverId);
    void cancel(Long id, Long userId);
    void addSign(Long id, Long addUserId, Long currentApproverId);
    void transfer(Long id, Long toUserId, String reason, Long currentApproverId);
}