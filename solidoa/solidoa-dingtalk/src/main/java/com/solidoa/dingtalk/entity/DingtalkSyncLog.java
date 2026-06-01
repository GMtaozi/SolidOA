package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_dingtalk_sync_log")
public class DingtalkSyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String syncType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer successCount;
    private Integer failCount;
    private String errorMsg;
    private String status;
    private LocalDateTime createTime;
}
