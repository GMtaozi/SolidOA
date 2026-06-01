package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤组VO
 */
@Data
public class AttendanceGroupVO {

    private Long id;

    /**
     * 考勤组名称
     */
    private String groupName;

    /**
     * 班次ID
     */
    private Long shiftId;

    /**
     * 班次名称
     */
    private String shiftName;

    /**
     * 适用部门
     */
    private List<Long> applicableDepts;

    /**
     * 适用用户
     */
    private List<Long> applicableUsers;

    /**
     * 考勤地点
     */
    private List<String> checkLocation;

    /**
     * 考勤范围(米)
     */
    private Integer checkRange;

    /**
     * 允许远程打卡
     */
    private Boolean allowRemoteCheck;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}