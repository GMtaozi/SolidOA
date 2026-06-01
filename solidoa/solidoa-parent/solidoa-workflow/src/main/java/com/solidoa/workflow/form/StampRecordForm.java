package com.solidoa.workflow.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用印登记表单
 */
@Data
public class StampRecordForm {

    /** 用印时间 */
    @NotNull(message = "请选择用印时间")
    private LocalDateTime stampTime;

    /** 领用人 */
    @NotBlank(message = "请输入领用人")
    private String receivedBy;

    /** 领用人手机 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String receivedMobile;

    /** 实际用印份数 */

    /** 实际用印份数 */
    @NotNull(message = "请输入实际用印份数")
    @Min(value = 1, message = "实际用印份数至少为1")
    private Integer actualCount;

    /** 备注 */
    private String remark;
}