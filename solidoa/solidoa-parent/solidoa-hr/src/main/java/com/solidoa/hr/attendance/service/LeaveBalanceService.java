package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.AdjustLeaveBalanceForm;
import com.solidoa.hr.attendance.form.InitLeaveBalanceForm;
import com.solidoa.hr.attendance.vo.LeaveBalanceVO;
import com.solidoa.hr.attendance.vo.LeaveTypeVO;
import java.util.List;
import java.util.Map;

/**
 * 假期余额服务接口
 */
public interface LeaveBalanceService {

    /**
     * 获取假期类型列表
     */
    List<LeaveTypeVO> getLeaveTypes();

    /**
     * 新增假期类型
     */
    Long createLeaveType(Map<String, Object> form);

    /**
     * 修改假期类型
     */
    void updateLeaveType(Long id, Map<String, Object> form);

    /**
     * 删除假期类型
     */
    void deleteLeaveType(Long id);

    /**
     * 获取用户假期余额
     */
    List<LeaveBalanceVO> getLeaveBalance(Long userId, Integer year);

    /**
     * 初始化员工假期余额
     */
    void initLeaveBalance(InitLeaveBalanceForm form, Long adminId);

    /**
     * 调整员工假期余额
     */
    void adjustLeaveBalance(AdjustLeaveBalanceForm form, Long adminId);
}