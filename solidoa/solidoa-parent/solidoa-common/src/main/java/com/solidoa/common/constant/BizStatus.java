package com.solidoa.common.constant;

/**
 * 业务状态常量
 * 统一管理所有业务状态值，避免魔法字符串
 */
public class BizStatus {

    private BizStatus() {}

    // ========== 审批状态 ==========
    /** 草稿 */
    public static final String DRAFT = "DRAFT";
    /** 已提交 */
    public static final String SUBMITTED = "SUBMITTED";
    /** 审批中 */
    public static final String PENDING = "PENDING";
    /** 已通过 */
    public static final String APPROVED = "APPROVED";
    /** 已拒绝 */
    public static final String REJECTED = "REJECTED";
    /** 已撤销 */
    public static final String CANCELLED = "CANCELLED";

    // ========== 报销单状态 ==========
    /** 待审批 */
    public static final String EXPENSE_PENDING = "PENDING";
    /** 已审批 */
    public static final String EXPENSE_APPROVED = "APPROVED";
    /** 已完成 */
    public static final String COMPLETED = "COMPLETED";
    /** 已驳回 */
    public static final String EXPENSE_REJECTED = "REJECTED";
    /** 已撤回 */
    public static final String EXPENSE_CANCELLED = "CANCELLED";

    // ========== 考勤状态 ==========
    /** 正常 */
    public static final String NORMAL = "NORMAL";
    /** 迟到 */
    public static final String LATE = "LATE";
    /** 早退 */
    public static final String EARLY_LEAVE = "EARLY_LEAVE";
    /** 旷工 */
    public static final String ABSENT = "ABSENT";
    /** 请假 */
    public static final String ON_LEAVE = "ON_LEAVE";
    /** 出差 */
    public static final String BUSINESS_TRIP = "BUSINESS_TRIP";

    // ========== 消息类型 ==========
    /** 系统消息 */
    public static final String MSG_SYSTEM = "SYSTEM";
    /** 审批通知 */
    public static final String MSG_APPROVAL = "APPROVAL";
    /** 催办通知 */
    public static final String MSG_REMINDER = "REMINDER";

    // ========== 消息优先级 ==========
    /** 低优先级 */
    public static final int PRIORITY_LOW = 0;
    /** 普通优先级 */
    public static final int PRIORITY_NORMAL = 1;
    /** 高优先级 */
    public static final int PRIORITY_HIGH = 2;
    /** 紧急 */
    public static final int PRIORITY_URGENT = 3;
}