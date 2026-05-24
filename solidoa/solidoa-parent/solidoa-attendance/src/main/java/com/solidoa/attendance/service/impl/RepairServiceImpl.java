package com.solidoa.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.attendance.entity.RepairCard;
import com.solidoa.attendance.form.RepairForm;
import com.solidoa.attendance.mapper.RepairCardMapper;
import com.solidoa.attendance.service.RepairService;
import com.solidoa.attendance.vo.RepairVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepairServiceImpl implements RepairService {

    @Autowired
    private RepairCardMapper repairCardMapper;

    @Override
    @Transactional
    public Long create(RepairForm form, Long userId) {
        RepairCard repair = new RepairCard();
        BeanUtils.copyProperties(form, repair);
        repair.setUserId(userId);
        repair.setStatus("PENDING");

        if (repair.getRepairTime() == null) {
            repair.setRepairTime(LocalDateTime.now());
        }

        repairCardMapper.insert(repair);
        log.info("创建补卡申请: userId={}, repairDate={}", userId, form.getRepairDate());
        return repair.getId();
    }

    @Override
    public PageVO<RepairVO> pageList(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<RepairCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairCard::getUserId, userId)
               .orderByDesc(RepairCard::getCreateTime);

        List<RepairCard> repairs = repairCardMapper.selectList(wrapper);
        List<RepairVO> voList = repairs.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        PageVO<RepairVO> page = new PageVO<>();
        page.setRecords(voList);
        page.setTotal(voList.size());
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public List<RepairVO> getPendingList() {
        List<RepairCard> repairs = repairCardMapper.selectPendingList(0, 100);
        return repairs.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approve(Long id, String result, Long approverId) {
        RepairCard repair = repairCardMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("补卡申请不存在");
        }

        repair.setStatus("APPROVED".equals(result) ? "APPROVED" : "REJECTED");
        repair.setApproverId(approverId);
        repairCardMapper.updateById(repair);

        log.info("审批补卡申请: id={}, result={}, approver={}", id, result, approverId);
    }

    private RepairVO convertToVO(RepairCard repair) {
        if (repair == null) return null;
        RepairVO vo = new RepairVO();
        BeanUtils.copyProperties(repair, vo);
        return vo;
    }
}