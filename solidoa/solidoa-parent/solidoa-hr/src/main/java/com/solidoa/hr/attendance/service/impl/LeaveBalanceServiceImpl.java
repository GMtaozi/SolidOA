package com.solidoa.hr.attendance.service.impl;

import com.solidoa.hr.attendance.entity.LeaveBalance;
import com.solidoa.hr.attendance.entity.LeaveType;
import com.solidoa.hr.attendance.form.AdjustLeaveBalanceForm;
import com.solidoa.hr.attendance.form.InitLeaveBalanceForm;
import com.solidoa.hr.attendance.mapper.LeaveBalanceMapper;
import com.solidoa.hr.attendance.mapper.LeaveTypeMapper;
import com.solidoa.hr.attendance.service.LeaveBalanceService;
import com.solidoa.hr.attendance.vo.LeaveBalanceVO;
import com.solidoa.hr.attendance.vo.LeaveTypeVO;
import com.solidoa.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 假期余额服务实现
 */
@Service
@Slf4j
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    @Autowired
    private LeaveTypeMapper leaveTypeMapper;

    @Autowired
    private LeaveBalanceMapper leaveBalanceMapper;

    @Override
    public List<LeaveTypeVO> getLeaveTypes() {
        return leaveTypeMapper.selectActiveList();
    }

    @Override
    @Transactional
    public Long createLeaveType(Map<String, Object> form) {
        LeaveType entity = new LeaveType();
        entity.setLeaveCode((String) form.get("leaveCode"));
        entity.setLeaveName((String) form.get("leaveName"));
        entity.setDefaultDays(new BigDecimal(String.valueOf(form.getOrDefault("defaultDays", "0"))));
        entity.setMaxDays(new BigDecimal(String.valueOf(form.getOrDefault("maxDays", "0"))));
        entity.setRequiresProof((Integer) form.getOrDefault("requiresProof", 0));
        entity.setCanTransfer((Integer) form.getOrDefault("canTransfer", 0));
        entity.setTransferMaxDays(new BigDecimal(String.valueOf(form.getOrDefault("transferMaxDays", "0"))));
        entity.setValidMonths((Integer) form.getOrDefault("validMonths", 0));
        entity.setDeductSalary((Integer) form.getOrDefault("deductSalary", 0));
        entity.setSort((Integer) form.getOrDefault("sort", 0));
        entity.setStatus((Integer) form.getOrDefault("status", 1));
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        leaveTypeMapper.insert(entity);
        log.info("创建假期类型: code={}, name={}", entity.getLeaveCode(), entity.getLeaveName());
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateLeaveType(Long id, Map<String, Object> form) {
        LeaveType entity = leaveTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("假期类型不存在");
        }
        if (form.containsKey("leaveName")) entity.setLeaveName((String) form.get("leaveName"));
        if (form.containsKey("defaultDays")) entity.setDefaultDays(new BigDecimal(String.valueOf(form.get("defaultDays"))));
        if (form.containsKey("maxDays")) entity.setMaxDays(new BigDecimal(String.valueOf(form.get("maxDays"))));
        if (form.containsKey("requiresProof")) entity.setRequiresProof((Integer) form.get("requiresProof"));
        if (form.containsKey("canTransfer")) entity.setCanTransfer((Integer) form.get("canTransfer"));
        if (form.containsKey("transferMaxDays")) entity.setTransferMaxDays(new BigDecimal(String.valueOf(form.get("transferMaxDays"))));
        if (form.containsKey("validMonths")) entity.setValidMonths((Integer) form.get("validMonths"));
        if (form.containsKey("deductSalary")) entity.setDeductSalary((Integer) form.get("deductSalary"));
        if (form.containsKey("sort")) entity.setSort((Integer) form.get("sort"));
        if (form.containsKey("status")) entity.setStatus((Integer) form.get("status"));
        entity.setUpdateTime(LocalDateTime.now());
        leaveTypeMapper.updateById(entity);
        log.info("更新假期类型: id={}, name={}", id, entity.getLeaveName());
    }

    @Override
    @Transactional
    public void deleteLeaveType(Long id) {
        LeaveType entity = leaveTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("假期类型不存在");
        }
        entity.setStatus(0);
        entity.setUpdateTime(LocalDateTime.now());
        leaveTypeMapper.updateById(entity);
        log.info("删除假期类型: id={}", id);
    }

    @Override
    public List<LeaveBalanceVO> getLeaveBalance(Long userId, Integer year) {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        return leaveBalanceMapper.selectByUserId(userId, year);
    }

    @Override
    @Transactional
    public void initLeaveBalance(InitLeaveBalanceForm form, Long adminId) {
        Integer year = form.getYear();
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }

        List<Long> userIds = form.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }

        List<LeaveBalance> balances = new ArrayList<>();
        for (Long userId : userIds) {
            // 查询工龄计算年假天数
            Integer workYears = leaveBalanceMapper.selectWorkYears(userId);
            if (workYears == null) {
                workYears = 0;
            }
            int annualDays = calculateAnnualLeave(workYears);

            // 创建年假余额
            LeaveBalance annualBalance = new LeaveBalance();
            annualBalance.setUserId(userId);
            annualBalance.setLeaveType("ANNUAL");
            annualBalance.setYear(year);
            annualBalance.setTotalDays(BigDecimal.valueOf(annualDays));
            annualBalance.setUsedDays(BigDecimal.ZERO);
            annualBalance.setPendingDays(BigDecimal.ZERO);
            annualBalance.setTransferredDays(BigDecimal.ZERO);
            balances.add(annualBalance);

            // 创建病假余额（默认5天）
            LeaveBalance sickBalance = new LeaveBalance();
            sickBalance.setUserId(userId);
            sickBalance.setLeaveType("SICK");
            sickBalance.setYear(year);
            sickBalance.setTotalDays(BigDecimal.valueOf(5));
            sickBalance.setUsedDays(BigDecimal.ZERO);
            sickBalance.setPendingDays(BigDecimal.ZERO);
            sickBalance.setTransferredDays(BigDecimal.ZERO);
            balances.add(sickBalance);

            // 创建事假余额（默认3天）
            LeaveBalance personalBalance = new LeaveBalance();
            personalBalance.setUserId(userId);
            personalBalance.setLeaveType("PERSONAL");
            personalBalance.setYear(year);
            personalBalance.setTotalDays(BigDecimal.valueOf(3));
            personalBalance.setUsedDays(BigDecimal.ZERO);
            personalBalance.setPendingDays(BigDecimal.ZERO);
            personalBalance.setTransferredDays(BigDecimal.ZERO);
            balances.add(personalBalance);

            log.info("初始化假期余额: userId={}, year={}, annualDays={}", userId, year, annualDays);
        }

        if (!balances.isEmpty()) {
            leaveBalanceMapper.insertOrUpdateBatch(balances);
        }
        log.info("批量初始化假期余额完成: count={}, year={}, adminId={}", userIds.size(), year, adminId);
    }

    @Override
    @Transactional
    public void adjustLeaveBalance(AdjustLeaveBalanceForm form, Long adminId) {
        Long userId = form.getUserId();
        String leaveType = form.getLeaveType();
        Integer year = form.getYear();
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        BigDecimal adjustDays = form.getAdjustDays();
        String adjustType = form.getAdjustType();

        // 查询当前余额
        LeaveBalance balance = leaveBalanceMapper.selectByUserIdAndType(userId, leaveType, year);
        if (balance == null) {
            throw new BusinessException("假期余额记录不存在");
        }

        // 根据调整类型计算新的可用天数
        BigDecimal newTotalDays = balance.getTotalDays();
        switch (adjustType) {
            case "ADD":
                newTotalDays = newTotalDays.add(adjustDays);
                break;
            case "REDUCE":
                newTotalDays = newTotalDays.subtract(adjustDays);
                if (newTotalDays.compareTo(BigDecimal.ZERO) < 0) {
                    newTotalDays = BigDecimal.ZERO;
                }
                break;
            case "SET":
                newTotalDays = adjustDays;
                break;
            default:
                throw new BusinessException("不支持的调整类型");
        }

        balance.setTotalDays(newTotalDays);
        leaveBalanceMapper.updateById(balance);
        log.info("调整假期余额: userId={}, leaveType={}, year={}, adjustType={}, adjustDays={}, adminId={}",
            userId, leaveType, year, adjustType, adjustDays, adminId);
    }

    /**
     * 根据工龄计算年假天数
     * 规则：1年以下0天，1-5年5天，5-10年10天，10-20年15天，20年以上20天
     */
    public int calculateAnnualLeave(int workYears) {
        if (workYears < 1) return 0;
        if (workYears < 5) return 5;
        if (workYears < 10) return 10;
        if (workYears < 20) return 15;
        return 20;
    }
}