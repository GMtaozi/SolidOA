package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.AttendanceGroupForm;
import com.solidoa.hr.attendance.form.HolidayForm;
import com.solidoa.hr.attendance.form.RuleConfigForm;
import com.solidoa.hr.attendance.form.ShiftForm;
import com.solidoa.hr.attendance.vo.AttendanceGroupVO;
import com.solidoa.hr.attendance.vo.HolidayVO;
import com.solidoa.hr.attendance.vo.RuleConfigVO;
import com.solidoa.hr.attendance.vo.ShiftVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.util.List;
import java.util.Map;

/**
 * 考勤规则服务接口
 */
public interface AttendanceRuleService {

    // ========== 班次管理 ==========

    /**
     * 获取班次列表
     */
    List<ShiftVO> listShifts();

    /**
     * 获取班次详情
     */
    ShiftVO getShiftById(Long id);

    /**
     * 新增班次
     */
    Long createShift(ShiftForm form);

    /**
     * 修改班次
     */
    void updateShift(Long id, ShiftForm form);

    /**
     * 删除班次
     */
    void deleteShift(Long id);

    // ========== 考勤组管理 ==========

    /**
     * 获取考勤组列表
     */
    PageVO<AttendanceGroupVO> listGroups(PageDTO dto);

    /**
     * 获取考勤组详情
     */
    AttendanceGroupVO getGroupById(Long id);

    /**
     * 新增考勤组
     */
    Long createGroup(AttendanceGroupForm form);

    /**
     * 修改考勤组
     */
    void updateGroup(Long id, AttendanceGroupForm form);

    /**
     * 删除考勤组
     */
    void deleteGroup(Long id);

    // ========== 规则配置 ==========

    /**
     * 获取所有规则配置
     */
    Map<String, Object> getRules();

    /**
     * 获取指定规则配置
     */
    RuleConfigVO getRuleByCode(String ruleCode);

    /**
     * 更新规则配置
     */
    void updateRule(String ruleCode, Map<String, Object> config);

    /**
     * 批量更新规则配置
     */
    void updateRules(Map<String, Object> rules);

    // ========== 节假日管理 ==========

    /**
     * 获取节假日列表
     */
    List<HolidayVO> listHolidays(Integer year);

    /**
     * 获取调休上班日列表
     */
    List<HolidayVO> listWorkdays(Integer year);

    /**
     * 新增节假日
     */
    Long createHoliday(HolidayForm form);

    /**
     * 修改节假日
     */
    void updateHoliday(Long id, HolidayForm form);

    /**
     * 删除节假日
     */
    void deleteHoliday(Long id);

    /**
     * 批量导入节假日
     */
    void importHolidays(List<HolidayForm> holidays);

    // ========== 年假初始化 ==========

    /**
     * 初始化年度假期余额
     */
    void initAnnualLeaveBalance(Integer year);
}