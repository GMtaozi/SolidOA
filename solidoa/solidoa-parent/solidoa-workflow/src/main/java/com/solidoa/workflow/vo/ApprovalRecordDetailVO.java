package com.solidoa.workflow.vo;

import lombok.Data;
import java.util.List;

/**
 * 审批记录详情VO
 */
@Data
public class ApprovalRecordDetailVO<T> extends ApprovalRecordVO {

    /** 申请内容详情 */
    private T content;

    /** 节点列表 */
    private List<ApprovalNodeDetailVO> nodes;

    /** 抄送列表 */
    private List<ApprovalCcVO> ccs;
}