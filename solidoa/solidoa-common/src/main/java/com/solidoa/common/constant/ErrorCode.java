package com.solidoa.common.constant;

public enum ErrorCode {
    // 通用错误
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户模块 1000-1099
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已禁用"),
    USER_EXISTS(1004, "用户已存在"),

    // 审批模块 2000-2099
    APPROVAL_NOT_FOUND(2001, "审批记录不存在"),
    APPROVAL_ALREADY_PROCESSED(2002, "该审批已处理"),
    APPROVAL_NO_PERMISSION(2003, "无审批权限"),

    // 考勤模块 3000-3099
    ATTENDANCE_ALREADY_CLOCKED(3001, "今日已打卡"),
    ATTENDANCE_NOT_WORKDAY(3002, "非工作日"),

    // 财务模块 4000-4099
    BUDGET_EXCEEDED(4001, "预算超支"),
    EXPENSE_NOT_FOUND(4002, "报销单不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
