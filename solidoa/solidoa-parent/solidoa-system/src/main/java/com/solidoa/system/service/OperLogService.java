package com.solidoa.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.system.entity.OperLog;

public interface OperLogService {
    /**
     * 记录操作日志
     */
    void save(OperLog operLog);

    /**
     * 分页查询 (V2.0 5.3 操作日志可视化)
     */
    PageVO<OperLog> pageList(PageDTO dto, String module, String userName);
}
