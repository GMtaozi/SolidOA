package com.solidoa.hr.attendance.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 考勤组表单
 */
@Data
public class AttendanceGroupForm {

    private Long id;

    /**
     * 考勤组名称
     */
    @NotBlank(message = "考勤组名称不能为空")
    private String groupName;

    /**
     * 班次ID
     */
    @NotNull(message = "班次ID不能为空")
    private Long shiftId;

    /**
     * 适用部门(JSON数组)
     */
    private List<Long> applicableDepts;

    /**
     * 适用用户(JSON数组)
     */
    private List<Long> applicableUsers;

    /**
     * 考勤地点(JSON数组)
     */
    private List<String> checkLocation;

    /**
     * 考勤范围(米)
     */
    private Integer checkRange;

    /**
     * 允许远程打卡:0否,1是
     */
    private Boolean allowRemoteCheck;

    /**
     * 状态:0禁用,1启用
     */
    private Integer status;
}