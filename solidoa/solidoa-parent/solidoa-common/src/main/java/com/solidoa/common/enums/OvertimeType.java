package com.solidoa.common.enums;

/**
 * 加班类型枚举
 * 说明：加班时长以审批单填写的开始/结束时间为准
 */
public enum OvertimeType {
    WORKDAY("WORKDAY", "工作日加班"),
    REST_DAY("REST_DAY", "休息日加班"),
    HOLIDAY("HOLIDAY", "节假日加班");

    private final String code;
    private final String desc;

    OvertimeType(String code, String desc) {
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