package com.solidoa.hr.attendance.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

/**
 * 规则配置表单
 */
@Data
public class RuleConfigForm {

    /**
     * 规则编码
     */
    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    /**
     * 规则配置
     */
    @NotNull(message = "规则配置不能为空")
    private Map<String, Object> config;
}