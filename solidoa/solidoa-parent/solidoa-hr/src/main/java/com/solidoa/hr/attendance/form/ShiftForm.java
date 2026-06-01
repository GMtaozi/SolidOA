package com.solidoa.hr.attendance.form;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 班次表单
 */
@Data
public class ShiftForm {

    private Long id;

    /**
     * 班次编码
     */
    @NotBlank(message = "班次编码不能为空")
    private String shiftCode;

    /**
     * 班次名称
     */
    @NotBlank(message = "班次名称不能为空")
    private String shiftName;

    /**
     * 上班时间
     */
    @NotNull(message = "上班时间不能为空")
    private LocalTime checkInTime;

    /**
     * 下班时间
     */
    @NotNull(message = "下班时间不能为空")
    private LocalTime checkOutTime;

    /**
     * 弹性时间(分钟)
     */
    private Integer flexibleMinutes;

    /**
     * 宽限时间(分钟)
     */
    private Integer graceMinutes;

    /**
     * 工作日(1=周一到5=周五)
     */
    private String workDays;

    /**
     * 适用部门(JSON数组)
     */
    private String applicableDepts;

    /**
     * 状态:0禁用,1启用
     */
    private Integer status;

    /**
     * 校验下班时间必须晚于上班时间
     */
    @AssertTrue(message = "下班时间必须晚于上班时间")
    public boolean isCheckOutAfterCheckIn() {
        if (checkInTime == null || checkOutTime == null) {
            return true; // 由@NotNull单独校验
        }
        return checkOutTime.isAfter(checkInTime);
    }
}