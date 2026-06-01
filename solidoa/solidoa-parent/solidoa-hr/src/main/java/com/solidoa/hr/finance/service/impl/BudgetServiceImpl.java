package com.solidoa.hr.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.hr.finance.entity.Budget;
import com.solidoa.hr.finance.entity.BudgetAdjust;
import com.solidoa.hr.finance.form.BudgetForm;
import com.solidoa.hr.finance.mapper.BudgetMapper;
import com.solidoa.hr.finance.service.BudgetService;
import com.solidoa.hr.finance.vo.BudgetVO;
import com.solidoa.hr.finance.vo.BudgetWarningVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetMapper budgetMapper;

    @Override
    @Transactional
    public Long create(BudgetForm form) {
        Budget budget = new Budget();
        BeanUtils.copyProperties(form, budget);
        budget.setUsedAmount(BigDecimal.ZERO);

        if (budget.getWarningThreshold() == null) {
            budget.setWarningThreshold(new BigDecimal("0.8"));
        }

        budgetMapper.insert(budget);
        log.info("创建预算: deptId={}, year={}, month={}, amount={}",
            budget.getDeptId(), budget.getYear(), budget.getMonth(), budget.getTotalAmount());
        return budget.getId();
    }

    @Override
    @Transactional
    public void update(BudgetForm form) {
        Budget budget = budgetMapper.selectById(form.getId());
        if (budget != null) {
            BeanUtils.copyProperties(form, budget);
            budgetMapper.updateById(budget);
            log.info("更新预算: id={}", form.getId());
        }
    }

    @Override
    public List<BudgetVO> list(Integer year, Integer month) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(year != null, Budget::getYear, year)
               .eq(month != null, Budget::getMonth, month);

        List<Budget> budgets = budgetMapper.selectList(wrapper);
        List<BudgetVO> result = new ArrayList<>();

        for (Budget budget : budgets) {
            BudgetVO vo = convertToVO(budget);
            result.add(vo);
        }

        return result;
    }

    @Override
    public List<BudgetWarningVO> getWarnings() {
        List<Budget> budgets = budgetMapper.selectList(null);
        List<BudgetWarningVO> warnings = new ArrayList<>();

        for (Budget budget : budgets) {
            if (budget.getTotalAmount() == null || budget.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
            BigDecimal total = budget.getTotalAmount() != null ? budget.getTotalAmount() : BigDecimal.ZERO;
            // 计算剩余金额，避免 null 值
            BigDecimal remaining = total.subtract(used);
            double usageRate = used.divide(total, 4, RoundingMode.HALF_UP).doubleValue();

            BigDecimal threshold = budget.getWarningThreshold() != null ?
                budget.getWarningThreshold() : new BigDecimal("0.8");

            if (usageRate >= threshold.doubleValue()) {
                BudgetWarningVO warning = new BudgetWarningVO();
                warning.setDeptId(budget.getDeptId());
                warning.setYear(budget.getYear());
                warning.setMonth(budget.getMonth());
                warning.setTotalAmount(budget.getTotalAmount());
                warning.setUsedAmount(used);
                warning.setRemainingAmount(remaining);
                warning.setUsageRate(usageRate * 100);
                warning.setThreshold(threshold);
                warnings.add(warning);
            }
        }

        return warnings;
    }

    @Override
    @Transactional
    public void adjust(Long budgetId, BigDecimal amount, String adjustType, String reason) {
        // 使用悲观锁查询，防止并发修改导致数据不一致
        Budget budget = budgetMapper.selectByIdForUpdate(budgetId);
        if (budget == null) {
            return;
        }

        BigDecimal newTotal = budget.getTotalAmount();
        if ("ADD".equals(adjustType)) {
            newTotal = newTotal.add(amount);
        } else {
            newTotal = newTotal.subtract(amount);
            if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("预算调减后金额不能为负数");
            }
        }

        budget.setTotalAmount(newTotal);
        budgetMapper.updateById(budget);

        log.info("调整预算: id={}, adjustType={}, amount={}, reason={}", budgetId, adjustType, amount, reason);
    }

    private BudgetVO convertToVO(Budget budget) {
        BudgetVO vo = new BudgetVO();
        BeanUtils.copyProperties(budget, vo);

        if (budget.getTotalAmount() != null && budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
            double usageRate = used.divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP).doubleValue();
            vo.setUsageRate(usageRate * 100);
        }

        return vo;
    }
}