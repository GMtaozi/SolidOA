package com.solidoa.workflow.service;

import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.workflow.vo.ReminderVO;

public interface ReminderService {

    void sendReminder(String businessType, Long businessId, Long requestUserId);

    PageVO<ReminderVO> getReminderHistory(String businessType, Long businessId, PageDTO dto);
}