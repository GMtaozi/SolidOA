package com.solidoa.hr.finance.service;

import com.solidoa.hr.finance.form.ExpenseForm;
import com.solidoa.hr.finance.vo.ExpenseVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.math.BigDecimal;
import java.util.Map;

public interface ExpenseService {
    Long create(ExpenseForm form, Long userId, Long deptId);

    void confirmPayment(Long id, Long cashierId, String confirmNote);

    void approve(Long id, String approveResult, String comment, Long approverId);

    void cancel(Long id, Long userId);

    PageVO<ExpenseVO> pageList(PageDTO dto, Long userId, String status);
    ExpenseVO getById(Long id);
    Map<String, Object> getStatistics(String startDate, String endDate);

    // 供其他服务 Feign 调用
    BigDecimal getMonthlyExpenseAmount(Long userId, String yearMonth);
    BigDecimal getYearlyExpenseAmount(Long userId, String year);
}