package com.solidoa.hr.finance.service;

import com.solidoa.hr.finance.form.BudgetForm;
import com.solidoa.hr.finance.vo.BudgetVO;
import com.solidoa.hr.finance.vo.BudgetWarningVO;
import java.util.List;

public interface BudgetService {
    Long create(BudgetForm form);

    void update(BudgetForm form);

    List<BudgetVO> list(Integer year, Integer month);

    List<BudgetWarningVO> getWarnings();

    void adjust(Long budgetId, java.math.BigDecimal amount, String adjustType, String reason);
}