package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.system.entity.OperLog;
import com.solidoa.system.mapper.OperLogMapper;
import com.solidoa.system.service.OperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class OperLogServiceImpl implements OperLogService {

    @Autowired
    private OperLogMapper operLogMapper;

    /**
     * 异步记录操作日志 (V2.0 5.3 操作日志扩展: @Async 异步)
     */
    @Override
    @Async
    public void save(OperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("[5.3 操作日志] 记录失败（不影响主流程）: {}", e.getMessage());
        }
    }

    @Override
    public PageVO<OperLog> pageList(PageDTO dto, String module, String userName) {
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperLog::getModule, module);
        }
        if (StringUtils.hasText(userName)) {
            wrapper.like(OperLog::getUserName, userName);
        }
        wrapper.orderByDesc(OperLog::getCreateTime);

        IPage<OperLog> page = operLogMapper.selectPage(
            new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

        PageVO<OperLog> vo = new PageVO<>();
        vo.setRecords(page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setPageNum((int) page.getCurrent());
        vo.setPageSize((int) page.getSize());
        return vo;
    }
}
