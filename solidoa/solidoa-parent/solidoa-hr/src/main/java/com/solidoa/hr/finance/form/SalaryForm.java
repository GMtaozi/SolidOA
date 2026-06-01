package com.solidoa.hr.finance.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * 工资审批表单
 */
@Data
public class SalaryForm {

    /** 申请日期 */
    @NotBlank(message = "申请日期不能为空")
    private String applyDate;

    /** 发薪日期 */
    @NotBlank(message = "发薪日期不能为空")
    private String payDate;

    /** 薪资期间 */
    @NotBlank(message = "薪资期间不能为空")
    private String salaryMonth;

    /** 发薪类型：MONTHLY月薪, BONUS奖金, ANNUAL年终奖, OTHER其他 */
    @NotBlank(message = "发薪类型不能为空")
    private String payType;

    /** 工资发放人数 */
    @NotNull(message = "发放人数不能为空")
    @Min(value = 1, message = "发放人数至少为1")
    private Integer employeeCount;

    /** 应发工资合计 */
    @NotNull(message = "应发工资合计不能为空")
    @Min(value = 0, message = "应发工资不能为负数")
    private BigDecimal totalGrossSalary;

    /** 代扣代缴合计 - 已添加@NotNull @Min(0)验证，防止负数或null */
    @NotNull(message = "代扣代缴合计不能为空")
    @Min(value = 0, message = "代扣代缴不能为负数")
    private BigDecimal totalDeduction;

    /** 实发工资合计（服务端自动计算，不允许前端传入） */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BigDecimal totalNetSalary;

    /** 附件 */
    private String attachments;

    /**
     * 获取实发工资（强制服务端计算：应发工资 - 代扣代缴）
     * @return 实发工资
     */
    public BigDecimal getTotalNetSalary() {
        if (totalGrossSalary == null || totalDeduction == null) {
            return null;
        }
        return totalGrossSalary.subtract(totalDeduction);
    }
}