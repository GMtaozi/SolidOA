package com.solidoa.common.enums;

/**
 * 加班补偿类型枚举
 */
public enum CompensationType {
    BREAK("BREAK", "调休"),
    SALARY("SALARY", "加班费");

    private final String code;
    private final String desc;

    CompensationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}