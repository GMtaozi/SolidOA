package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物理用印记录
 */
@Data
@TableName("oa_stamp_record")
public class StampRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用印申请ID */
    private Long stampId;

    /** 用印时间 */
    private LocalDateTime stampTime;

    /** 领用人 */
    private String receivedBy;

    /** 领用人手机（加密存储） */
    private String receivedMobile;

    /** 实际用印份数 */
    private Integer actualCount;

    /** 办理人ID */
    private Long operatorId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}