package com.solidoa.dingtalk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加班记录钉钉扩展表
 */
@Data
@TableName("oa_overtime_ext")
public class OvertimeExt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 加班记录ID(关联oa_overtime或oa_attendance) */
    private Long overtimeId;

    /** 钉钉加班记录ID */
    private String dingtalkOvertimeId;

    /** 钉钉审批流程ID */
    private String dingtalkFlowId;

    /** 钉钉审批结果 */
    private String dingtalkApproveResult;

    /** 加班工作日组ID */
    private String workGroupId;

    /** 加班工作日组名称 */
    private String workGroupName;

    /** 加班时长(小时) */
    private BigDecimal durationHours;

    /** 加班类型: WD工作日/WE休息日/HOLIDAY节假日 */
    private String overtimeType;

    /** 调休单位(小时) */
    private Integer converterUnit;

    /** 是否有效 */
    private Boolean isValid;

    private LocalDateTime createTime;
}