package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_transfer_record")
public class TransferRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessType;
    private Long businessId;
    private Long fromApproverId;
    private Long toApproverId;
    private String reason;
    private LocalDateTime createTime;
}