package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.vo.AttendanceVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.util.List;
import java.util.Map;

/**
 * 考勤服务接口
 *
 * 考勤数据来源说明：
 * - 主数据源：钉钉同步（通过 DingTalk 服务）
 * - 本地打卡功能已禁用，保留 check 方法作为备用
 */
public interface AttendanceService {

    /**
     * 本地打卡（已禁用）
     * 保留方法签名作为备用模式
     */
    // AttendanceVO check(CheckForm form, Long userId);

    /**
     * 获取打卡记录
     */
    PageVO<AttendanceVO> getRecords(PageDTO dto, String checkDate, Long userId);

    /**
     * 获取考勤汇总
     */
    Map<String, Object> getSummary(String yearMonth, Long userId);

    /**
     * 获取合并后的打卡记录（钉钉 + 本地）
     * 钉钉数据优先级高，本地数据作为备用补充
     */
    List<Map<String, Object>> getMergedClockRecords(Long userId, String startDate, String endDate);
}