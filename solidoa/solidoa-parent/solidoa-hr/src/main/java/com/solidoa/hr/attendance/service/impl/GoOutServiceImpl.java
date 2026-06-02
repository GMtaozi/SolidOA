package com.solidoa.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.hr.attendance.entity.GoOut;
import com.solidoa.hr.attendance.form.GoOutForm;
import com.solidoa.hr.attendance.mapper.GoOutMapper;
import com.solidoa.hr.attendance.service.GoOutService;
import com.solidoa.hr.attendance.vo.GoOutVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.security.PermissionHelper;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 外出申请服务实现
 */
@Service
@Slf4j
public class GoOutServiceImpl implements GoOutService {

    @Autowired
    private GoOutMapper goOutMapper;

    @Autowired
    private com.solidoa.common.client.WorkflowClient workflowClient;

    @Override
    @Transactional
    public Long create(GoOutForm form, Long userId) {
        GoOut goOut = new GoOut();
        BeanUtils.copyProperties(form, goOut);
        goOut.setUserId(userId);
        goOut.setStatus("PENDING");
        goOut.setOutNo(generateOutNo());

        goOutMapper.insert(goOut);
        // Sprint 3.4 修复：同步写审批节点
        syncApprovalNode("GO_OUT", goOut.getId(), userId);
        log.info("创建外出申请: userId={}, outNo={}", userId, goOut.getOutNo());
        return goOut.getId();
    }

    private void syncApprovalNode(String businessType, Long businessId, Long applicantId) {
        try {
            workflowClient.createApprovalNodes(businessType, businessId, applicantId);
            log.debug("审批节点同步成功: {}#{}", businessType, businessId);
        } catch (Exception e) {
            log.warn("审批节点同步失败（不影响主流程）: {}#{}, reason={}", businessType, businessId, e.getMessage());
        }
    }

    @Override
    public PageVO<GoOutVO> pageList(Long userId, int pageNum, int pageSize) {
        Page<GoOut> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GoOut> wrapper = new LambdaQueryWrapper<GoOut>()
            .eq(GoOut::getUserId, userId)
            .orderByDesc(GoOut::getCreateTime);
        goOutMapper.selectPage(page, wrapper);

        List<GoOutVO> voList = page.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        PageVO<GoOutVO> result = new PageVO<>();
        result.setRecords(voList);
        result.setTotal(page.getTotal());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    public GoOutVO getById(Long id, Long userId) {
        GoOut goOut = goOutMapper.selectById(id);
        if (goOut == null) {
            throw new BusinessException("外出申请不存在");
        }
        // 越权校验：管理员可查看所有申请，普通用户只能查看自己的申请
        if (!goOut.getUserId().equals(userId) && !PermissionHelper.isAdmin()) {
            throw new BusinessException("无权限查看此外出申请");
        }
        return convertToVO(goOut);
    }

    @Override
    @Transactional
    public void approve(Long id, String result, Long approverId) {
        GoOut goOut = goOutMapper.selectById(id);
        if (goOut == null) {
            throw new BusinessException("外出申请不存在");
        }
        if (!"PENDING".equals(goOut.getStatus())) {
            throw new BusinessException("只能审批待审批的申请");
        }

        String newStatus = "APPROVED".equals(result) ? "APPROVED" : "REJECTED";
        goOut.setStatus(newStatus);
        goOutMapper.updateById(goOut);

        log.info("审批外出申请: id={}, result={}, approver={}", id, result, approverId);
    }

    @Override
    @Transactional
    public void cancel(Long id, Long userId) {
        GoOut goOut = goOutMapper.selectById(id);
        if (goOut == null) {
            throw new BusinessException("外出申请不存在");
        }
        if (!goOut.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的申请");
        }
        if (!"PENDING".equals(goOut.getStatus())) {
            throw new BusinessException("只能撤回待审批的申请");
        }

        goOut.setStatus("CANCELLED");
        goOutMapper.updateById(goOut);
        log.info("撤回外出申请: id={}, userId={}", id, userId);
    }

    private GoOutVO convertToVO(GoOut goOut) {
        if (goOut == null) return null;
        GoOutVO vo = new GoOutVO();
        BeanUtils.copyProperties(goOut, vo);
        return vo;
    }

    private String generateOutNo() {
        String prefix = "WC";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }
}