package com.solidoa.system.service;

import com.solidoa.common.vo.PageVO;
import com.solidoa.system.form.ScheduleForm;
import com.solidoa.system.vo.ScheduleVO;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {
    Long create(ScheduleForm form, Long userId);
    void update(Long id, ScheduleForm form, Long userId);
    void delete(Long id, Long userId);
    ScheduleVO getById(Long id);
    List<ScheduleVO> listByDateRange(LocalDateTime startDate, LocalDateTime endDate, Long userId);
    List<ScheduleVO> listAllByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}