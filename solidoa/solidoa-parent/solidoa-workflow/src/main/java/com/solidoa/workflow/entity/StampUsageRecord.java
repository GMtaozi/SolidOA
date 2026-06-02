package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用印记录实体
 */
@Data
@TableName("biz_stamp_usage_record")
public class StampUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String usageNo;
    private Long stampId;
    private String stampName;
    private Long applicantId;
    private String applicantName;
    private String deptName;
    private String fileName;
    private Integer fileCount;
    private String reason;
    private LocalDateTime usageTime;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
