package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请表单
 */
@Data
public class RepairForm {

    @NotNull(message = "补卡日期不能为空")
    private LocalDate repairDate; // 补卡日期

    @NotNull(message = "补卡类型不能为空")
    @Pattern(regexp = "^(MISSING|LATE|EARLY_LEAVE)$", message = "补卡类型只能是MISSING、LATE或EARLY_LEAVE")
    private String repairType; // 补卡类型：MISSING（缺卡）、LATE（迟到）、EARLY_LEAVE（早退）

    private LocalDateTime repairTime; // 补卡时间

    private String reason; // 补卡原因

    @Pattern(regexp = "^(MORNING|NIGHT)$", message = "时间段只能是MORNING或NIGHT")
    private String timeSlot; // 时间段：MORNING上班、NIGHT下班

    private String attachments; // 证明材料URL
}