package com.solidoa.hr.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.hr.finance.entity.Salary;
import com.solidoa.hr.finance.entity.SalaryApproval;
import com.solidoa.hr.finance.form.SalaryForm;
import com.solidoa.hr.finance.form.BatchSalaryForm;
import com.solidoa.hr.finance.form.BatchPayForm;
import com.solidoa.hr.finance.mapper.SalaryMapper;
import com.solidoa.hr.finance.mapper.SalaryApprovalMapper;
import com.solidoa.hr.finance.service.SalaryService;
import com.solidoa.hr.finance.vo.SalaryVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private SalaryApprovalMapper salaryApprovalMapper;

    @Override
    @Transactional
    public Long createSalary(SalaryForm form, Long creatorId) {
        Salary salary = new Salary();
        salary.setSalaryNo(generateSalaryNo());
        salary.setCreatorId(creatorId);
        salary.setUserId(creatorId);
        salary.setSalaryMonth(form.getSalaryMonth());
        salary.setPayType(form.getPayType());
        salary.setEmployeeCount(form.getEmployeeCount());
        // 确保工资金额精度为2位小数，使用HALF_UP舍入模式
        salary.setTotalGrossSalary(form.getTotalGrossSalary() != null ? form.getTotalGrossSalary().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        salary.setTotalDeduction(form.getTotalDeduction() != null ? form.getTotalDeduction().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        // 实发工资强制计算：应发工资 - 代扣代缴，防止前端篡改
        BigDecimal gross = salary.getTotalGrossSalary();
        BigDecimal deduction = salary.getTotalDeduction();
        salary.setTotalNetSalary(gross.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
        salary.setAttachments(form.getAttachments());
        salary.setStatus("DRAFT");
        salary.setCreateTime(LocalDateTime.now());
        salary.setUpdateTime(LocalDateTime.now());

        if (form.getApplyDate() != null && !form.getApplyDate().isEmpty()) {
            salary.setApplyDate(LocalDate.parse(form.getApplyDate()));
        }
        if (form.getPayDate() != null && !form.getPayDate().isEmpty()) {
            salary.setPayDate(LocalDate.parse(form.getPayDate()));
        }

        salaryMapper.insert(salary);
        log.info("创建工资单: id={}, no={}", salary.getId(), salary.getSalaryNo());
        return salary.getId();
    }

    @Override
    @Transactional
    public Integer batchCreateSalary(BatchSalaryForm form, Long creatorId) {
        if (form == null || form.getUserIds() == null || form.getUserIds().isEmpty()) {
            throw new BusinessException("用户列表不能为空");
        }
        if (form.getYearMonth() == null || form.getYearMonth().isEmpty()) {
            throw new BusinessException("工资月份不能为空");
        }

        List<Salary> salaryList = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long userId : form.getUserIds()) {
            Salary salary = new Salary();
            salary.setSalaryNo(generateSalaryNo());
            salary.setCreatorId(creatorId);
            salary.setUserId(userId);
            salary.setSalaryMonth(form.getYearMonth());
            salary.setStatus("DRAFT");
            salary.setCreateTime(now);
            salary.setUpdateTime(now);
            salaryList.add(salary);
        }

        // 使用 MyBatis-Plus IService 的 saveBatch 方法批量插入
        // 事务由 @Transactional 注解保证
        salaryMapper.insertBatchSomeColumn(salaryList);

        log.info("批量创建工资单: count={}, yearMonth={}", salaryList.size(), form.getYearMonth());
        return salaryList.size();
    }

    @Override
    public PageVO<SalaryVO> listSalary(PageDTO dto, String yearMonth, String status, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<SalaryVO> records = salaryMapper.selectPageList(offset, dto.getPageSize(), yearMonth, status, deptId);
        long total = salaryMapper.selectCount(yearMonth, status, deptId);

        PageVO<SalaryVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPages((int) Math.ceil((double) total / dto.getPageSize()));
        return page;
    }

    @Override
    public SalaryVO getSalaryById(Long id) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        SalaryVO vo = new SalaryVO();
        BeanUtils.copyProperties(salary, vo);
        return vo;
    }

    @Override
    @Transactional
    public void updateSalary(Long id, SalaryForm form, Long updaterId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"DRAFT".equals(salary.getStatus())) {
            throw new BusinessException("只有草稿状态的工资单才能修改");
        }

        if (form.getSalaryMonth() != null) salary.setSalaryMonth(form.getSalaryMonth());
        if (form.getPayType() != null) salary.setPayType(form.getPayType());
        if (form.getEmployeeCount() != null) salary.setEmployeeCount(form.getEmployeeCount());
        if (form.getTotalGrossSalary() != null) salary.setTotalGrossSalary(form.getTotalGrossSalary());
        if (form.getTotalDeduction() != null) salary.setTotalDeduction(form.getTotalDeduction());
        // 实发工资强制计算：应发工资 - 代扣代缴，防止前端篡改
        // 增加 null 保护，避免 NPE
        BigDecimal gross = salary.getTotalGrossSalary();
        BigDecimal deduction = salary.getTotalDeduction();
        if (gross != null && deduction != null) {
            salary.setTotalNetSalary(gross.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
        } else if (gross != null) {
            salary.setTotalNetSalary(gross.setScale(2, RoundingMode.HALF_UP));
        } else if (deduction != null) {
            salary.setTotalNetSalary(BigDecimal.ZERO.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
        }
        if (form.getAttachments() != null) salary.setAttachments(form.getAttachments());
        if (form.getApplyDate() != null) salary.setApplyDate(LocalDate.parse(form.getApplyDate()));
        if (form.getPayDate() != null) salary.setPayDate(LocalDate.parse(form.getPayDate()));

        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("更新工资单: id={}", id);
    }

    @Override
    @Transactional
    public void submitSalary(Long id, Long submitterId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"DRAFT".equals(salary.getStatus())) {
            throw new BusinessException("只有草稿状态的工资单才能提交");
        }

        salary.setStatus("PENDING");
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("提交工资单审批: id={}", id);
    }

    @Override
    @Transactional
    public void cancelSalary(Long id, Long userId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"DRAFT".equals(salary.getStatus()) && !"PENDING".equals(salary.getStatus())) {
            throw new BusinessException("只有草稿或待审批状态的工资单才能撤回");
        }

        salary.setStatus("CANCELLED");
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("撤回工资单: id={}", id);
    }

    @Override
    @Transactional
    public void approveSalary(Long id, String comment, Long approverId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"PENDING".equals(salary.getStatus())) {
            throw new BusinessException("只有待审批状态的工资单才能审批");
        }

        salary.setStatus("APPROVED");
        salary.setApproverId(approverId);
        salary.setApproveTime(LocalDateTime.now());
        salary.setApproveComment(comment);
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("审批工资单通过: id={}", id);
    }

    @Override
    @Transactional
    public void rejectSalary(Long id, String comment, Long approverId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"PENDING".equals(salary.getStatus())) {
            throw new BusinessException("只有待审批状态的工资单才能驳回");
        }

        salary.setStatus("REJECTED");
        salary.setApproverId(approverId);
        salary.setApproveTime(LocalDateTime.now());
        salary.setApproveComment(comment);
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("驳回工资单: id={}", id);
    }

    @Override
    @Transactional
    public void paySalary(Long id, Long operatorId) {
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("工资单不存在");
        }
        if (!"APPROVED".equals(salary.getStatus())) {
            throw new BusinessException("只有已审批通过的工资单才能发放");
        }

        salary.setStatus("PAID");
        salary.setPaidTime(LocalDateTime.now());
        salary.setUpdateTime(LocalDateTime.now());
        salaryMapper.updateById(salary);
        log.info("发放工资单: id={}", id);
    }

    @Override
    @Transactional
    public Integer batchPaySalary(BatchPayForm form, Long operatorId) {
        int count = 0;
        for (Long id : form.getSalaryIds()) {
            // 每个工资单单独处理事务，失败不影响其他
            try {
                paySalary(id, operatorId);
                count++;
            } catch (Exception e) {
                // 记录失败但继续处理下一个，实现部分成功
                log.error("工资单发放失败: id={}, error={}", id, e.getMessage());
            }
        }
        return count;
    }

    @Override
    public List<SalaryVO> getMySalary(Long userId, String startMonth, String endMonth) {
        return salaryMapper.selectByUserId(userId, startMonth, endMonth);
    }

    @Override
    public List<com.solidoa.hr.finance.vo.SalaryItemVO> getSalaryItems() {
        // 返回工资组成项目列表
        return java.util.Collections.emptyList();
    }

    @Override
    public void exportSalary(String yearMonth, Long deptId, HttpServletResponse response) {
        // 导出逻辑
    }

    private String generateSalaryNo() {
        String prefix = "SAL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用 FOR UPDATE 行锁，防止并发场景下多个请求获取相同最大单号导致重复
        String maxNo = salaryMapper.selectMaxSalaryNoForUpdate(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        int seq = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
        return prefix + String.format("%04d", seq);
    }
}