package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考勤规则配置实体
 */
@Data
@TableName("oa_attendance_rule")
public class AttendanceRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型:OVERTIME,LATE,EARLY_LEAVE,ABSENT,REPAIR
     */
    private String ruleType;

    /**
     * 阈值(用于OVERTIME等)
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
     * 状态:0禁用,1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}