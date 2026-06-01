package com.solidoa.hr.attendance.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.solidoa.hr.attendance.entity.*;
import com.solidoa.hr.attendance.form.AttendanceGroupForm;
import com.solidoa.hr.attendance.form.HolidayForm;
import com.solidoa.hr.attendance.form.ShiftForm;
import com.solidoa.hr.attendance.mapper.*;
import com.solidoa.hr.attendance.service.AttendanceRuleService;
import com.solidoa.hr.attendance.vo.AttendanceGroupVO;
import com.solidoa.hr.attendance.vo.HolidayVO;
import com.solidoa.hr.attendance.vo.RuleConfigVO;
import com.solidoa.hr.attendance.vo.ShiftVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤规则服务实现
 */
@Service
@Slf4j
public class AttendanceRuleServiceImpl implements AttendanceRuleService {

    @Autowired
    private AttendanceShiftMapper shiftMapper;

    @Autowired
    private AttendanceGroupMapper groupMapper;

    @Autowired
    private AttendanceRuleMapper ruleMapper;

    @Autowired
    private AttendanceHolidayMapper holidayMapper;

    // ========== 班次管理 ==========

    @Override
    public List<ShiftVO> listShifts() {
        List<AttendanceShift> shifts = shiftMapper.selectList(
            new LambdaQueryWrapper<AttendanceShift>()
                .eq(AttendanceShift::getStatus, 1)
                .orderByDesc(AttendanceShift::getCreateTime)
        );

        return shifts.stream().map(this::convertToShiftVO).collect(Collectors.toList());
    }

    @Override
    public ShiftVO getShiftById(Long id) {
        AttendanceShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }
        return convertToShiftVO(shift);
    }

    @Override
    @Transactional
    public Long createShift(ShiftForm form) {
        // 检查编码唯一性
        AttendanceShift existShift = shiftMapper.selectOne(
            new LambdaQueryWrapper<AttendanceShift>()
                .eq(AttendanceShift::getShiftCode, form.getShiftCode())
        );
        if (existShift != null) {
            throw new BusinessException("班次编码已存在");
        }

        AttendanceShift shift = new AttendanceShift();
        BeanUtils.copyProperties(form, shift);
        if (shift.getStatus() == null) shift.setStatus(1);

        shiftMapper.insert(shift);
        log.info("创建班次: id={}, name={}", shift.getId(), shift.getShiftName());
        return shift.getId();
    }

    @Override
    @Transactional
    public void updateShift(Long id, ShiftForm form) {
        AttendanceShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }

        // 检查编码唯一性（排除自身）
        AttendanceShift existShift = shiftMapper.selectOne(
            new LambdaQueryWrapper<AttendanceShift>()
                .eq(AttendanceShift::getShiftCode, form.getShiftCode())
                .ne(AttendanceShift::getId, id)
        );
        if (existShift != null) {
            throw new BusinessException("班次编码已存在");
        }

        BeanUtils.copyProperties(form, shift);

        shiftMapper.updateById(shift);
        log.info("更新班次: id={}, name={}", id, shift.getShiftName());
    }

    @Override
    @Transactional
    public void deleteShift(Long id) {
        AttendanceShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BusinessException("班次不存在");
        }

        // 检查是否有考勤组使用该班次
        long usedCount = groupMapper.selectCount(
            new LambdaQueryWrapper<AttendanceGroup>()
                .eq(AttendanceGroup::getShiftId, id)
                .eq(AttendanceGroup::getStatus, 1)
        );
        if (usedCount > 0) {
            throw new BusinessException("该班次已被考勤组使用，无法删除");
        }

        // 软删除
        shift.setStatus(0);
        shiftMapper.updateById(shift);
        log.info("删除班次: id={}", id);
    }

    // ========== 考勤组管理 ==========

    @Override
    public PageVO<AttendanceGroupVO> listGroups(PageDTO dto) {
        IPage<AttendanceGroup> page = groupMapper.selectPage(
            new Page<>(dto.getPageNum(), dto.getPageSize()),
            new LambdaQueryWrapper<AttendanceGroup>()
                .eq(AttendanceGroup::getStatus, 1)
                .orderByDesc(AttendanceGroup::getCreateTime)
        );

        PageVO<AttendanceGroupVO> result = new PageVO<>();
        result.setRecords(page.getRecords().stream()
            .map(this::convertToGroupVO)
            .collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setPageNum(dto.getPageNum());
        result.setPageSize(dto.getPageSize());

        return result;
    }

    @Override
    public AttendanceGroupVO getGroupById(Long id) {
        AttendanceGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("考勤组不存在");
        }
        return convertToGroupVO(group);
    }

    @Override
    @Transactional
    public Long createGroup(AttendanceGroupForm form) {
        // 校验班次是否存在
        if (form.getShiftId() != null) {
            AttendanceShift shift = shiftMapper.selectById(form.getShiftId());
            if (shift == null) {
                throw new BusinessException("班次不存在");
            }
        }

        AttendanceGroup group = new AttendanceGroup();
        BeanUtils.copyProperties(form, group);

        // JSON序列化列表字段
        if (form.getApplicableDepts() != null) {
            group.setApplicableDepts(JSON.toJSONString(form.getApplicableDepts()));
        }
        if (form.getApplicableUsers() != null) {
            group.setApplicableUsers(JSON.toJSONString(form.getApplicableUsers()));
        }
        if (form.getCheckLocation() != null) {
            group.setCheckLocation(JSON.toJSONString(form.getCheckLocation()));
        }

        if (group.getCheckRange() == null) group.setCheckRange(500);
        if (group.getAllowRemoteCheck() == null) group.setAllowRemoteCheck(false);
        if (group.getStatus() == null) group.setStatus(1);

        groupMapper.insert(group);
        log.info("创建考勤组: id={}, name={}", group.getId(), group.getGroupName());
        return group.getId();
    }

    @Override
    @Transactional
    public void updateGroup(Long id, AttendanceGroupForm form) {
        AttendanceGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("考勤组不存在");
        }

        // 校验班次是否存在
        if (form.getShiftId() != null) {
            AttendanceShift shift = shiftMapper.selectById(form.getShiftId());
            if (shift == null) {
                throw new BusinessException("班次不存在");
            }
        }

        BeanUtils.copyProperties(form, group);

        // JSON序列化列表字段
        if (form.getApplicableDepts() != null) {
            group.setApplicableDepts(JSON.toJSONString(form.getApplicableDepts()));
        }
        if (form.getApplicableUsers() != null) {
            group.setApplicableUsers(JSON.toJSONString(form.getApplicableUsers()));
        }
        if (form.getCheckLocation() != null) {
            group.setCheckLocation(JSON.toJSONString(form.getCheckLocation()));
        }

        groupMapper.updateById(group);
        log.info("更新考勤组: id={}, name={}", id, group.getGroupName());
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        AttendanceGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("考勤组不存在");
        }

        // 软删除
        group.setStatus(0);
        groupMapper.updateById(group);
        log.info("删除考勤组: id={}", id);
    }

    // ========== 规则配置 ==========

    @Override
    public Map<String, Object> getRules() {
        List<AttendanceRule> rules = ruleMapper.selectList(
            new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getStatus, 1)
                .orderByAsc(AttendanceRule::getRuleType)
        );

        Map<String, Object> result = new HashMap<>();
        for (AttendanceRule rule : rules) {
            Map<String, Object> config = new HashMap<>();
            config.put("id", rule.getId());
            config.put("ruleType", rule.getRuleType());
            config.put("ruleName", rule.getRuleName());
            config.put("threshold", rule.getThreshold());
            config.put("deductDays", rule.getDeductDays());
            config.put("deductSalary", rule.getDeductSalary());
            config.put("status", rule.getStatus());
            result.put(rule.getRuleType(), config);
        }

        return result;
    }

    @Override
    public RuleConfigVO getRuleByCode(String ruleType) {
        AttendanceRule rule = ruleMapper.selectOne(
            new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getRuleType, ruleType)
        );

        if (rule == null) {
            throw new BusinessException("规则不存在: " + ruleType);
        }

        RuleConfigVO vo = new RuleConfigVO();
        vo.setId(rule.getId());
        vo.setRuleType(rule.getRuleType());
        vo.setRuleName(rule.getRuleName());
        vo.setStatus(rule.getStatus());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());

        return vo;
    }

    @Override
    @Transactional
    public void updateRule(String ruleType, Map<String, Object> config) {
        AttendanceRule rule = ruleMapper.selectOne(
            new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getRuleType, ruleType)
        );

        if (rule == null) {
            // 创建新规则
            rule = new AttendanceRule();
            rule.setRuleType(ruleType);
            rule.setRuleName(getRuleName(ruleType));
            rule.setStatus(1);
            ruleMapper.insert(rule);
            log.info("创建规则配置: type={}", ruleType);
        }

        // 更新已有规则
        if (config.containsKey("ruleName")) {
            rule.setRuleName((String) config.get("ruleName"));
        }
        if (config.containsKey("threshold")) {
            rule.setThreshold((Integer) config.get("threshold"));
        }
        if (config.containsKey("deductDays")) {
            rule.setDeductDays(new java.math.BigDecimal(config.get("deductDays").toString()));
        }
        if (config.containsKey("deductSalary")) {
            rule.setDeductSalary(new java.math.BigDecimal(config.get("deductSalary").toString()));
        }
        if (config.containsKey("status")) {
            rule.setStatus((Integer) config.get("status"));
        }

        ruleMapper.updateById(rule);
        log.info("更新规则配置: type={}", ruleType);
    }

    @Override
    @Transactional
    public void updateRules(Map<String, Object> rules) {
        for (Map.Entry<String, Object> entry : rules.entrySet()) {
            String ruleCode = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) value;
                updateRule(ruleCode, config);
            }
        }
        log.info("批量更新规则配置: codes={}", rules.keySet());
    }

    // ========== 节假日管理 ==========

    @Override
    public List<HolidayVO> listHolidays(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        List<AttendanceHoliday> holidays = holidayMapper.selectByYear(year);
        return holidays.stream().map(this::convertToHolidayVO).collect(Collectors.toList());
    }

    @Override
    public List<HolidayVO> listWorkdays(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        List<AttendanceHoliday> workdays = holidayMapper.selectWorkdays(year);
        return workdays.stream().map(this::convertToHolidayVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long createHoliday(HolidayForm form) {
        // 检查日期唯一性
        AttendanceHoliday existHoliday = holidayMapper.selectOne(
            new LambdaQueryWrapper<AttendanceHoliday>()
                .eq(AttendanceHoliday::getHolidayDate, form.getHolidayDate())
        );
        if (existHoliday != null) {
            throw new BusinessException("该日期已配置节假日");
        }

        AttendanceHoliday holiday = new AttendanceHoliday();
        BeanUtils.copyProperties(form, holiday);
        holiday.setYear(form.getHolidayDate().getYear());

        holidayMapper.insert(holiday);
        log.info("创建节假日: id={}, name={}", holiday.getId(), holiday.getHolidayName());
        return holiday.getId();
    }

    @Override
    @Transactional
    public void updateHoliday(Long id, HolidayForm form) {
        AttendanceHoliday holiday = holidayMapper.selectById(id);
        if (holiday == null) {
            throw new BusinessException("节假日不存在");
        }

        // 检查日期唯一性（排除自身）
        AttendanceHoliday existHoliday = holidayMapper.selectOne(
            new LambdaQueryWrapper<AttendanceHoliday>()
                .eq(AttendanceHoliday::getHolidayDate, form.getHolidayDate())
                .ne(AttendanceHoliday::getId, id)
        );
        if (existHoliday != null) {
            throw new BusinessException("该日期已配置节假日");
        }

        BeanUtils.copyProperties(form, holiday);
        holiday.setYear(form.getHolidayDate().getYear());

        holidayMapper.updateById(holiday);
        log.info("更新节假日: id={}, name={}", id, holiday.getHolidayName());
    }

    @Override
    @Transactional
    public void deleteHoliday(Long id) {
        holidayMapper.deleteById(id);
        log.info("删除节假日: id={}", id);
    }

    @Override
    @Transactional
    public void importHolidays(List<HolidayForm> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            return;
        }

        for (HolidayForm form : holidays) {
            try {
                createHoliday(form);
            } catch (Exception e) {
                log.warn("导入节假日失败: date={}, error={}", form.getHolidayDate(), e.getMessage());
            }
        }

        log.info("批量导入节假日: count={}", holidays.size());
    }

    // ========== 年假初始化 ==========

    @Override
    public void initAnnualLeaveBalance(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        // 初始化年度假期余额（年假）
        // 实际应用中应该从用户服务获取所有在职员工
        log.info("初始化{}年度假期余额", year);

        // TODO: 调用用户服务获取所有在职员工
        // List<User> users = userService.getActiveUsers();
        // for (User user : users) {
        //     int workYears = calculateWorkYears(user.getEntryDate());
        //     int annualLeaveDays = calculateAnnualLeave(workYears);
        //     // 创建或更新假期余额记录
        // }
    }

    // ========== 辅助方法 ==========

    private ShiftVO convertToShiftVO(AttendanceShift shift) {
        ShiftVO vo = new ShiftVO();
        BeanUtils.copyProperties(shift, vo);
        return vo;
    }

    private AttendanceGroupVO convertToGroupVO(AttendanceGroup group) {
        AttendanceGroupVO vo = new AttendanceGroupVO();
        BeanUtils.copyProperties(group, vo);

        // 获取班次名称
        if (group.getShiftId() != null) {
            AttendanceShift shift = shiftMapper.selectById(group.getShiftId());
            if (shift != null) {
                vo.setShiftName(shift.getShiftName());
            }
        }

        // 解析JSON字段
        if (StringUtils.hasText(group.getApplicableDepts())) {
            try {
                vo.setApplicableDepts(JSON.parseArray(group.getApplicableDepts(), Long.class));
            } catch (Exception e) {
                log.warn("解析适用部门失败: {}", e.getMessage());
            }
        }
        if (StringUtils.hasText(group.getApplicableUsers())) {
            try {
                vo.setApplicableUsers(JSON.parseArray(group.getApplicableUsers(), Long.class));
            } catch (Exception e) {
                log.warn("解析适用用户失败: {}", e.getMessage());
            }
        }
        if (StringUtils.hasText(group.getCheckLocation())) {
            try {
                vo.setCheckLocation(JSON.parseArray(group.getCheckLocation(), String.class));
            } catch (Exception e) {
                log.warn("解析考勤地点失败: {}", e.getMessage());
            }
        }

        return vo;
    }

    private HolidayVO convertToHolidayVO(AttendanceHoliday holiday) {
        HolidayVO vo = new HolidayVO();
        BeanUtils.copyProperties(holiday, vo);
        vo.setHolidayTypeName("HOLIDAY".equals(holiday.getHolidayType()) ? "节假日" : "调休上班");
        return vo;
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseLongList(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return json.startsWith("[") ? JSON.parseArray(json, Long.class) : null;
        } catch (Exception e) {
            log.warn("解析列表失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return json.startsWith("[") ? JSON.parseArray(json, String.class) : null;
        } catch (Exception e) {
            log.warn("解析列表失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String config) {
        if (!StringUtils.hasText(config)) {
            return new HashMap<>();
        }
        try {
            return JSON.parseObject(config, Map.class);
        } catch (Exception e) {
            log.warn("解析规则配置失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String getRuleName(String ruleCode) {
        Map<String, String> ruleNames = new HashMap<>();
        ruleNames.put("OVERTIME", "加班规则");
        ruleNames.put("LATE", "迟到规则");
        ruleNames.put("EARLY_LEAVE", "早退规则");
        ruleNames.put("ABSENT", "缺勤规则");
        ruleNames.put("REPAIR", "补卡规则");
        return ruleNames.getOrDefault(ruleCode, ruleCode);
    }

    private String getRuleType(String ruleCode) {
        return ruleCode;
    }

    /**
     * 计算工龄
     */
    private int calculateWorkYears(LocalDate entryDate) {
        if (entryDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.YEARS.between(entryDate, LocalDate.now());
    }

    /**
     * 根据工龄计算年假天数
     */
    private int calculateAnnualLeave(int workYears) {
        if (workYears < 1) return 0;
        if (workYears < 5) return 5;
        if (workYears < 10) return 10;
        if (workYears < 20) return 15;
        return 20;
    }
}