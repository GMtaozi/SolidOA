package com.solidoa.attendance.service;

import com.solidoa.attendance.form.RepairForm;
import com.solidoa.attendance.vo.RepairVO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

public interface RepairService {
    Long create(RepairForm form, Long userId);

    PageVO<RepairVO> pageList(Long userId, Integer pageNum, Integer pageSize);

    List<RepairVO> getPendingList();

    void approve(Long id, String result, Long approverId);
}