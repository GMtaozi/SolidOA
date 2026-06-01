package com.solidoa.workflow.service;

import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.workflow.form.ExpenseForm;
import com.solidoa.workflow.vo.ExpenseVO;
import java.util.List;

public interface ExpenseService {
    Long createExpense(ExpenseForm form, Long userId);

    PageVO<ExpenseVO> pageList(Long userId, String status, PageDTO dto);

    ExpenseVO getById(Long id);

    void approve(Long id, String approveResult, String comment, Long approverId);

    void addSign(Long id, Long addUserId, Long currentApproverId);

    void transfer(Long id, Long toUserId, String reason, Long currentApproverId);
}