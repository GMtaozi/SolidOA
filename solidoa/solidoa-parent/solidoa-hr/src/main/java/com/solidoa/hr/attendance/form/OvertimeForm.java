package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加班申请表单
 */
@Data
public class OvertimeForm {

    @AssertTrue(message = "结束时间必须晚于开始时间且时长不超过24小时，且必须为0.5小时的整数倍")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        if (!endTime.isAfter(startTime)) {
            return false;
        }
        // 计算时长（秒）
        long seconds = java.time.Duration.between(startTime, endTime).getSeconds();
        // 最大24小时
        if (seconds > 24 * 3600) {
            return false;
        }
        // 必须为0.5小时（1800秒）的整数倍
        return seconds % 1800 == 0;
    }
    @NotBlank(message = "加班类型不能为空")
    private String overtimeType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String reason;

    private String compensationType;
}