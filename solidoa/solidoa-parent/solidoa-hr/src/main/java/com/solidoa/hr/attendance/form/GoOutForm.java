package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 外出申请表单
 */
@Data
public class GoOutForm {

    @NotNull(message = "外出日期不能为空")
    private LocalDate outDate; // 外出日期

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime; // 开始时间

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime; // 结束时间

    @NotBlank(message = "外出地点不能为空")
    private String destination; // 外出地点

    @NotBlank(message = "外出事由不能为空")
    private String reason; // 外出事由

    private String attachments; // 附件

    /**
     * 校验outDate与startTime的日期部分一致
     */
    @AssertTrue(message = "外出日期与开始时间的日期必须一致")
    public boolean isDateConsistent() {
        if (outDate == null || startTime == null) {
            return true; // 由@NotNull处理空值
        }
        return outDate.equals(startTime.toLocalDate());
    }
}