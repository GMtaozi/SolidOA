package com.solidoa.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 催办记录VO
 */
@Data
public class ReminderVO {

    private Long id;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 催办人ID */
    private Long approverId;

    /** 催办次数 */
    private Integer reminderCount;

    /** 最近催办时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReminderTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
