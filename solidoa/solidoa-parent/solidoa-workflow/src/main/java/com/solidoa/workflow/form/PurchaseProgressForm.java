package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 采购进度表单
 */
@Data
public class PurchaseProgressForm {

    /** 进度类型: ORDERED-已下单, SHIPPING-发货中, DELIVERED-已到货, REJECTED-拒收 */
    @NotBlank(message = "请选择进度类型")
    private String progressType;

    /** 进度描述 */
    private String progressDesc;

    /** 进度时间 */
    @NotNull(message = "请选择进度时间")
    private LocalDateTime progressTime;
}