package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 抄送记录VO
 */
@Data
public class ApprovalCcVO {

    private Long id;

    /** 业务类型 */
    private String businessType;

    /** 业务类型名称 */
    private String businessTypeName;

    /** 业务ID */
    private Long businessId;

    /** 业务单号 */
    private String businessNo;

    /** 申请人名称 */
    private String applicantName;

    /** 申请标题 */
    private String title;

    /** 业务状态 */
    private String status;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
