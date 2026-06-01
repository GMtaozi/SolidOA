package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出差申请表单
 */
@Data
public class BusinessTripForm {

    @AssertTrue(message = "结束日期不能早于开始日期")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
    @NotBlank(message = "出差目的地不能为空")
    private String destination;

    @NotBlank(message = "出差类型不能为空")
    private String tripType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotBlank(message = "出差事由不能为空")
    private String reason;

    /** 预算金额 */
    @DecimalMin(value = "0", message = "预算金额不能为负数")
    @DecimalMax(value = "999999999.99", message = "预算金额超出允许范围")
    private BigDecimal budgetAmount;

    /** 预算说明 */
    private String budgetRemark;

    /** 随行人员 */
    private String companions;

    /** 附件 */
    private String attachments;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;
}