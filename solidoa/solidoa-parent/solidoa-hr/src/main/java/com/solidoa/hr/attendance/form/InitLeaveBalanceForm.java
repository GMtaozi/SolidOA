package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 初始化假期余额表单
 */
@Data
public class InitLeaveBalanceForm {
    /** 用户ID列表 */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    /** 年度 */
    @NotNull(message = "年度不能为空")
    private Integer year;
}