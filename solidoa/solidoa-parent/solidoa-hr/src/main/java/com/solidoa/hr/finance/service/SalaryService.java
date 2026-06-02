package com.solidoa.hr.finance.service;

import com.solidoa.hr.finance.form.SalaryForm;
import com.solidoa.hr.finance.form.BatchSalaryForm;
import com.solidoa.hr.finance.form.BatchPayForm;
import com.solidoa.hr.finance.vo.SalaryVO;
import com.solidoa.hr.finance.vo.SalaryItemVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface SalaryService {

    List<SalaryItemVO> getSalaryItems();

    Long createSalary(SalaryForm form, Long creatorId);

    Integer batchCreateSalary(BatchSalaryForm form, Long creatorId);

    PageVO<SalaryVO> listSalary(PageDTO dto, String yearMonth, String status, Long deptId);

    SalaryVO getSalaryById(Long id);

    void updateSalary(Long id, SalaryForm form, Long updaterId);

    void submitSalary(Long id, Long submitterId);

    void cancelSalary(Long id, Long userId);

    void approveSalary(Long id, String comment, Long approverId);

    void rejectSalary(Long id, String comment, Long approverId);

    void paySalary(Long id, Long operatorId);

    Integer batchPaySalary(BatchPayForm form, Long operatorId);

    List<SalaryVO> getMySalary(Long userId, String startMonth, String endMonth);

    void exportSalary(String yearMonth, Long deptId, HttpServletResponse response);

    /**
     * 确认工资条
     */
    void confirmSalary(Long id, Long userId);

    /**
     * 提出工资异议
     */
    void disputeSalary(Long id, Long userId, String reason);
}