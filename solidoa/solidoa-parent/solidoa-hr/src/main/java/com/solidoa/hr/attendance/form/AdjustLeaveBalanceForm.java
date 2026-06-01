package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 调整假期余额表单
 */
@Data
public class AdjustLeaveBalanceForm {
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 假期类型 */
    @NotBlank(message = "假期类型不能为空")
    private String leaveType;

    /** 年度 */
    @NotNull(message = "年度不能为空")
    private Integer year;

    /** 调整类型: ADD增加, REDUCE减少, SET设置 */
    @NotBlank(message = "调整类型不能为空")
    @Pattern(regexp = "^(ADD|REDUCE|SET)$", message = "调整类型只能是 ADD、REDUCE 或 SET")
    private String adjustType;

    /** 调整天数 */
    @NotNull(message = "调整天数不能为空")
    @DecimalMin(value = "0.5", message = "调整天数至少为0.5天")
    private BigDecimal adjustDays;

    /** 调整原因 */
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}