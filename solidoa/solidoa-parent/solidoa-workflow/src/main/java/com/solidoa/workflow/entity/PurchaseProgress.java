package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 采购进度记录
 */
@Data
@TableName("oa_purchase_progress")
public class PurchaseProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购单ID */
    private Long purchaseId;

    /** 进度类型: ORDERED-已下单, SHIPPING-发货中, DELIVERED-已到货, REJECTED-拒收 */
    private String progressType;

    /** 进度描述 */
    private String progressDesc;

    /** 进度时间 */
    private LocalDateTime progressTime;

    /** 操作人ID */
    private Long operatorId;

    /** 乐观锁版本号 */
    @Version
    private Long version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}