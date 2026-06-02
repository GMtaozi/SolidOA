package com.solidoa.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.hr.attendance.entity.Attendance;
import com.solidoa.hr.attendance.entity.RepairCard;
import com.solidoa.hr.attendance.entity.RepairCardStatistics;
import com.solidoa.hr.attendance.form.RepairForm;
import com.solidoa.hr.attendance.mapper.AttendanceMapper;
import com.solidoa.hr.attendance.mapper.RepairCardMapper;
import com.solidoa.hr.attendance.mapper.RepairCardStatisticsMapper;
import com.solidoa.hr.attendance.service.RepairCardService;
import com.solidoa.hr.attendance.vo.RepairVO;
import com.solidoa.common.enums.RepairType;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 补卡服务实现
 */
@Service
@Slf4j
public class RepairCardServiceImpl implements RepairCardService {

    private static final int MAX_REPAIR_COUNT = 5; // 每月最多补卡次数
    private static final int MAX_WORK_DAYS = 31; // 可申请最近31个工作日内的补卡

    @Autowired
    private RepairCardMapper repairCardMapper;

    @Autowired
    private RepairCardStatisticsMapper repairCardStatisticsMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private com.solidoa.common.client.WorkflowClient workflowClient;

    @Override
    @Transactional
    public Long create(RepairForm form, Long userId) {
        // 验证补卡规则
        validateRepairRules(form, userId);

        // 检查重复补卡
        int duplicateCount = repairCardMapper.countDuplicateRepair(
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

        // 更新补卡统计
        updateStatistics(userId, form.getRepairDate());

        // Sprint 3.4 修复：同步写审批节点到 oa_workflow 库（远程 Feign）
        syncApprovalNode("REPAIR_CARD", repair.getId(), userId);

        log.info("创建补卡申请: userId={}, repairDate={}, type={}",
            userId, form.getRepairDate(), form.getRepairType());
        return repair.getId();
    }

    /**
     * 验证补卡规则
     * 规则：
     * 1. 每月可提交5次（计时周期从每月1日起算）
     * 2. 可申请最近31个工作日内的补卡
     * 3. 补卡时间不能晚于当前时间
     * 4. 补卡类型：缺卡(MISSING)、迟到(LATE)、早退(EARLY_LEAVE)
     */
    private void validateRepairRules(RepairForm form, Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate repairDate = form.getRepairDate();

        // 验证补卡类型
        String repairType = form.getRepairType();
        boolean validType = false;
        for (RepairType type : RepairType.values()) {
            if (type.getCode().equals(repairType)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            throw new BusinessException("无效的补卡类型，支持：MISSING（缺卡）、LATE（迟到）、EARLY_LEAVE（早退）");
        }

        // 规则1：检查补卡次数（每月5次）
        LocalDate monthStart = now.withDayOfMonth(1);
        String startDate = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int count = repairCardMapper.countByDateRange(userId, startDate, endDate);
        if (count >= MAX_REPAIR_COUNT) {
            throw new BusinessException("本月补卡次数已达上限(" + MAX_REPAIR_COUNT + "次)");
        }

        // 规则2：检查补卡时间范围（31个工作日内）
        int workDays = calculateWorkDays(repairDate, now);
        if (workDays > MAX_WORK_DAYS) {
            throw new BusinessException("仅可申请最近" + MAX_WORK_DAYS + "个工作日内的补卡");
        }

        // 规则3：不能补未来的卡
        if (repairDate.isAfter(now)) {
            throw new BusinessException("不能补未来的打卡记录");
        }

        // 规则4：补卡时间有效性
        LocalDateTime repairTime = form.getRepairTime();
        if (repairTime != null && repairTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("补卡时间不能晚于当前时间");
        }
    }

    /**
     * 计算两个日期之间的工作日天数
     */
    private int calculateWorkDays(LocalDate startDate, LocalDate endDate) {
        int workDays = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workDays++;
            }
            date = date.plusDays(1);
        }
        return workDays;
    }

    /**
     * 更新补卡统计
     */
    /**
     * 同步写审批节点（Feign 远程调 workflow-service）
     * 失败不抛出（审批节点写入失败不影响主业务）
     */
    private void syncApprovalNode(String businessType, Long businessId, Long applicantId) {
        try {
            workflowClient.createApprovalNodes(businessType, businessId, applicantId);
            log.debug("审批节点同步成功: {}#{}", businessType, businessId);
        } catch (Exception e) {
            log.warn("审批节点同步失败（不影响主流程）: {}#{}, reason={}", businessType, businessId, e.getMessage());
        }
    }

    private void updateStatistics(Long userId, LocalDate repairDate) {
        updateStatistics(userId, repairDate, 1);
    }

    /**
     * 更新补卡统计（支持 delta）
     */
    private void updateStatistics(Long userId, LocalDate repairDate, int delta) {
        String yearMonth = repairDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        RepairCardStatistics stats = repairCardStatisticsMapper.selectByUserAndMonth(userId, yearMonth);
        int currentCount = (stats != null) ? stats.getRepairCount() : 0;
        int newCount = Math.max(0, currentCount + delta);
        repairCardStatisticsMapper.upsert(userId, yearMonth, newCount);
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
    public RepairVO getById(Long id) {
        RepairCard repair = repairCardMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("补卡申请不存在");
        }
        return convertToVO(repair);
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

    @Override
    @Transactional
    public void cancel(Long id, Long userId) {
        RepairCard repair = repairCardMapper.selectById(id);
        if (repair == null) {
            throw new BusinessException("补卡申请不存在");
        }
        if (!repair.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的补卡申请");
        }
        if (!"PENDING".equals(repair.getStatus())) {
            throw new BusinessException("只能撤回待审批状态的申请");
        }

        repair.setStatus("CANCELLED");
        repairCardMapper.updateById(repair);

        // 回退补卡统计
        updateStatistics(repair.getUserId(), repair.getRepairDate(), -1);

        log.info("撤回补卡申请: id={}, userId={}", id, userId);
    }

    @Override
    public Object getStatistics(Long userId, String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        RepairCardStatistics stats = repairCardStatisticsMapper.selectByUserAndMonth(userId, yearMonth);
        int count = (stats != null) ? stats.getRepairCount() : 0;
        return new RepairStatisticsVO(count, MAX_REPAIR_COUNT, count + "/" + MAX_REPAIR_COUNT);
    }

    @Override
    public List<RepairVO> getPendingList() {
        List<RepairCard> repairs = repairCardMapper.selectPendingList(0, 100);
        return repairs.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 更新考勤打卡记录
     */
    private void updateAttendanceRecord(RepairCard repair) {
        LocalDate repairDate = repair.getRepairDate();

        if (RepairType.MISSING.getCode().equals(repair.getRepairType())) {
            // 缺卡：查找当天考勤记录，标记为正常
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

            log.info("补缺卡记录已创建: userId={}, time={}", repair.getUserId(), repair.getRepairTime());

        } else if (RepairType.LATE.getCode().equals(repair.getRepairType())) {
            // 迟到：标记迟到时间
            Attendance lateRecord = new Attendance();
            lateRecord.setUserId(repair.getUserId());
            lateRecord.setCheckDate(repairDate);
            lateRecord.setCheckType("SIGN_IN");
            lateRecord.setCheckTime(repair.getRepairTime());
            lateRecord.setLocation("补卡");
            lateRecord.setDeviceType("REPAIR");
            lateRecord.setIsLate(1);
            lateRecord.setIsEarlyLeave(0);
            lateRecord.setCreateTime(LocalDateTime.now());
            attendanceMapper.insert(lateRecord);

            log.info("补迟到记录已创建: userId={}, time={}", repair.getUserId(), repair.getRepairTime());

        } else if (RepairType.EARLY_LEAVE.getCode().equals(repair.getRepairType())) {
            // 早退：标记早退时间
            Attendance earlyRecord = new Attendance();
            earlyRecord.setUserId(repair.getUserId());
            earlyRecord.setCheckDate(repairDate);
            earlyRecord.setCheckType("SIGN_OUT");
            earlyRecord.setCheckTime(repair.getRepairTime());
            earlyRecord.setLocation("补卡");
            earlyRecord.setDeviceType("REPAIR");
            earlyRecord.setIsLate(0);
            earlyRecord.setIsEarlyLeave(1);
            earlyRecord.setCreateTime(LocalDateTime.now());
            attendanceMapper.insert(earlyRecord);

            log.info("补早退记录已创建: userId={}, time={}", repair.getUserId(), repair.getRepairTime());
        }
    }

    private RepairVO convertToVO(RepairCard repair) {
        if (repair == null) return null;
        RepairVO vo = new RepairVO();
        BeanUtils.copyProperties(repair, vo);
        return vo;
    }

    /**
     * 补卡统计VO
     */
    public static class RepairStatisticsVO {
        private int usedCount;
        private int maxCount;
        private String usedRatio;

        public RepairStatisticsVO(int usedCount, int maxCount, String usedRatio) {
            this.usedCount = usedCount;
            this.maxCount = maxCount;
            this.usedRatio = usedRatio;
        }

        public int getUsedCount() { return usedCount; }
        public int getMaxCount() { return maxCount; }
        public String getUsedRatio() { return usedRatio; }
    }
}