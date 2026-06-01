package com.solidoa.workflow.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveForm {

    @AssertTrue(message = "结束日期不能早于开始日期")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }
    @NotNull(message = "请假类型不能为空")
    private String leaveType;

    @JsonProperty("type")
    public void setLeaveTypeFromType(String type) {
        if (this.leaveType == null && type != null) {
            this.leaveType = type;
        }
    }

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "请假天数不能为空")
    @DecimalMin(value = "0.5", message = "请假天数至少为0.5天")
    private BigDecimal days;

    private BigDecimal hours;
    private String reason;
    private String attachments;
}