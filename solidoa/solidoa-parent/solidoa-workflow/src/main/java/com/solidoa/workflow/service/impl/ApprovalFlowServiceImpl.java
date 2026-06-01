package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.workflow.entity.ApprovalFlowConfig;
import com.solidoa.workflow.form.ApprovalFlowConfigForm;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.mapper.ApprovalFlowConfigMapper;
import com.solidoa.workflow.service.ApprovalFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程配置服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final ApprovalFlowConfigMapper flowConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long createConfig(ApprovalFlowConfigForm form) {
        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setBusinessType(form.getBusinessType());
        config.setFlowName(form.getFlowName());
        config.setIsDefault(form.getIsDefault() != null && form.getIsDefault());
        config.setCreateTime(LocalDateTime.now());

        try {
            config.setConfig(objectMapper.writeValueAsString(form));
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "配置序列化失败");
        }

        flowConfigMapper.insert(config);

        // 如果设置为默认，则取消其他默认
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            clearDefault(config.getBusinessType(), config.getId());
        }

        log.info("创建审批流程配置: id={}, businessType={}, flowName={}", config.getId(), form.getBusinessType(), form.getFlowName());
        return config.getId();
    }

    @Override
    @Transactional
    public void updateConfig(Long id, ApprovalFlowConfigForm form) {
        ApprovalFlowConfig config = flowConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(404, "审批流程配置不存在");
        }

        config.setFlowName(form.getFlowName());
        config.setIsDefault(form.getIsDefault() != null && form.getIsDefault());

        try {
            config.setConfig(objectMapper.writeValueAsString(form));
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "配置序列化失败");
        }

        flowConfigMapper.updateById(config);

        // 如果设置为默认，则取消其他默认
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            clearDefault(config.getBusinessType(), config.getId());
        }

        log.info("更新审批流程配置: id={}", id);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        flowConfigMapper.deleteById(id);
        log.info("删除审批流程配置: id={}", id);
    }

    @Override
    public ApprovalFlowConfig getDefaultConfig(String businessType) {
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
               .eq(ApprovalFlowConfig::getIsDefault, true)
               .last("LIMIT 1");
        return flowConfigMapper.selectOne(wrapper);
    }

    @Override
    public List<ApprovalFlowConfig> listByBusinessType(String businessType) {
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
               .orderByDesc(ApprovalFlowConfig::getIsDefault)
               .orderByDesc(ApprovalFlowConfig::getCreateTime);
        return flowConfigMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void setDefault(Long id, String businessType) {
        // 清除该业务类型的所有默认
        clearDefault(businessType, null);

        // 设置新的默认
        ApprovalFlowConfig config = flowConfigMapper.selectById(id);
        if (config != null) {
            config.setIsDefault(true);
            flowConfigMapper.updateById(config);
        }

        log.info("设置默认审批流程: id={}, businessType={}", id, businessType);
    }

    private void clearDefault(String businessType, Long excludeId) {
        LambdaUpdateWrapper<ApprovalFlowConfig> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                     .eq(ApprovalFlowConfig::getIsDefault, true);
        if (excludeId != null) {
            updateWrapper.ne(ApprovalFlowConfig::getId, excludeId);
        }
        updateWrapper.set(ApprovalFlowConfig::getIsDefault, false);
        flowConfigMapper.update(null, updateWrapper);
    }
}
