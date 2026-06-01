package com.solidoa.workflow.service;

import com.solidoa.workflow.vo.TaskVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

public interface TaskService {
    PageVO<TaskVO> getMyTasks(Long userId, PageDTO dto);

    PageVO<TaskVO> getPendingTasks(Long userId, PageDTO dto);

    PageVO<TaskVO> getProcessedTasks(Long userId, PageDTO dto);
}