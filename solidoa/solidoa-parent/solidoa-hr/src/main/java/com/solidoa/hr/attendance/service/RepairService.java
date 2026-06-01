package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.RepairForm;
import com.solidoa.hr.attendance.vo.RepairVO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

public interface RepairService {
    Long create(RepairForm form, Long userId);

    PageVO<RepairVO> pageList(Long userId, Integer pageNum, Integer pageSize);

    List<RepairVO> getPendingList();

    List<RepairVO> getPendingList(int offset, int limit);

    void approve(Long id, String result, Long approverId);
}