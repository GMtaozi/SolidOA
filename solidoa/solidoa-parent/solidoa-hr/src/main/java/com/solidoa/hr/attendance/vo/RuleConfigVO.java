package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 规则配置VO
 */
@Data
public class RuleConfigVO {

    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 阈值
     */
    private Integer threshold;

    /**
     * 扣除天数
     */
    private BigDecimal deductDays;

    /**
     * 扣除工资
     */
    private BigDecimal deductSalary;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}