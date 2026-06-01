package com.solidoa.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.hr.attendance.entity.Attendance;
import com.solidoa.hr.attendance.entity.RepairCard;
import com.solidoa.hr.attendance.form.RepairForm;
import com.solidoa.hr.attendance.mapper.AttendanceMapper;
import com.solidoa.hr.attendance.mapper.RepairCardMapper;
import com.solidoa.hr.attendance.service.RepairService;
import com.solidoa.hr.attendance.vo.RepairVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RepairServiceImpl implements RepairService {

    @Autowired
    private RepairCardMapper repairCardMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Override
    @Transactional
    public Long create(RepairForm form, Long userId) {
        // 补卡日期范围校验：只能补最近30天内的卡，防止恶意填写历史记录
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        if (form.getRepairDate() == null) {
            throw new BusinessException("补卡日期不能为空");
        }
        if (form.getRepairDate().isAfter(today)) {
            throw new BusinessException("补卡日期不能是未来日期");
        }
        if (form.getRepairDate().isBefore(thirtyDaysAgo)) {
            throw new BusinessException("补卡申请只能补最近30天内的记录");
        }

        // 带行锁检查重复补卡（同一日期同一类型，已审批或待审批）
        // 使用 FOR UPDATE 锁住相关行，防止高并发场景下的重复提交
        int duplicateCount = repairCardMapper.checkAndLockForCreate(
            userId, form.getRepairDate().toString(), form.getRepairType());
        if (duplicateCount > 0) {
            throw new BusinessException("该日期已有相同类型的补卡申请，请勿重复提交");
        }

        RepairCard repair = new RepairCard();
        BeanUtils.copyProperties(form, repair);
        repair.setUserId(userId);
        repair.setStatus("PENDING");

        if (repair.getRepairTime() == null) {
            repair.setRepairTime(LocalDateTime.now());
        }

        repairCardMapper.insert(repair);
        log.info("创建补卡申请: userId={}, repairDate={}, type={}",
            userId, form.getRepairDate(), form.getRepairType());
        return repair.getId();
    }

    @Override
    public PageVO<RepairVO> pageList(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<RepairCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairCard::getUserId, userId)
               .orderByDesc(RepairCard::getCreateTime);

        Page<RepairCard> page = new Page<>(pageNum, pageSize);
        Page<RepairCard> result = repairCardMapper.selectPage(page, wrapper);

        List<RepairVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        PageVO<RepairVO> pageVO = new PageVO<>();
        pageVO.setRecords(voList);
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(pageNum);
        pageVO.setPageSize(pageSize);
        return pageVO;
    }

    @Override
    public List<RepairVO> getPendingList() {
        return getPendingList(0, 100);
    }

    @Override
    public List<RepairVO> getPendingList(int offset, int limit) {
        List<RepairCard> repairs = repairCardMapper.selectPendingList(offset, limit);
        return repairs.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approve(Long id, String result, Long approverId) {
        RepairCard repair = repairCardMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("补卡申请不存在");
        }

        String newStatus = "APPROVED".equals(result) ? "APPROVED" : "REJECTED";
        repair.setStatus(newStatus);
        repair.setApproverId(approverId);
        repairCardMapper.updateById(repair);

        // 如果是批准，更新考勤记录
        if ("APPROVED".equals(newStatus)) {
            updateAttendanceRecord(repair);
        }

        log.info("审批补卡申请: id={}, result={}, approver={}", id, result, approverId);
    }

    /**
     * 更新考勤打卡记录
     */
    private void updateAttendanceRecord(RepairCard repair) {
        LocalDate repairDate = repair.getRepairDate();

        if ("SIGN_IN".equals(repair.getRepairType())) {
            // 检查是否已存在当天签到记录
            LambdaQueryWrapper<Attendance> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(Attendance::getUserId, repair.getUserId())
                       .eq(Attendance::getCheckDate, repairDate)
                       .eq(Attendance::getCheckType, "SIGN_IN");
            if (attendanceMapper.selectCount(checkWrapper) > 0) {
                log.warn("补签到记录已存在，跳过插入: userId={}, date={}", repair.getUserId(), repairDate);
                return;
            }

            // 补签到：插入一条签到记录
            Attendance signInRecord = new Attendance();
            signInRecord.setUserId(repair.getUserId());
            signInRecord.setCheckDate(repairDate);
            signInRecord.setCheckType("SIGN_IN");
            signInRecord.setCheckTime(repair.getRepairTime());
            signInRecord.setLocation("补卡");
            signInRecord.setDeviceType("REPAIR");
            signInRecord.setIsLate(0);
            signInRecord.setIsEarlyLeave(0);
            signInRecord.setCreateTime(LocalDateTime.now());
            attendanceMapper.insert(signInRecord);

            log.info("补签到记录已创建: userId={}, time={}", repair.getUserId(), repair.getRepairTime());

        } else if ("SIGN_OUT".equals(repair.getRepairType())) {
            // 检查是否已存在当天签退记录
            LambdaQueryWrapper<Attendance> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(Attendance::getUserId, repair.getUserId())
                       .eq(Attendance::getCheckDate, repairDate)
                       .eq(Attendance::getCheckType, "SIGN_OUT");
            if (attendanceMapper.selectCount(checkWrapper) > 0) {
                log.warn("补签退记录已存在，跳过插入: userId={}, date={}", repair.getUserId(), repairDate);
                return;
            }

            // 补签退：插入签退记录
            Attendance signOutRecord = new Attendance();
            signOutRecord.setUserId(repair.getUserId());
            signOutRecord.setCheckDate(repairDate);
            signOutRecord.setCheckType("SIGN_OUT");
            signOutRecord.setCheckTime(repair.getRepairTime());
            signOutRecord.setLocation("补卡");
            signOutRecord.setDeviceType("REPAIR");
            signOutRecord.setIsLate(0);
            signOutRecord.setIsEarlyLeave(0);
            signOutRecord.setCreateTime(LocalDateTime.now());
            attendanceMapper.insert(signOutRecord);

            log.info("补签退记录已创建: userId={}, time={}", repair.getUserId(), repair.getRepairTime());
        }
    }

    private RepairVO convertToVO(RepairCard repair) {
        if (repair == null) return null;
        RepairVO vo = new RepairVO();
        BeanUtils.copyProperties(repair, vo);
        return vo;
    }
}