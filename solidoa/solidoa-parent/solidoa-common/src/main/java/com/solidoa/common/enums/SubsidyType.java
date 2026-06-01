package com.solidoa.common.enums;

/**
 * 补贴类型枚举
 */
public enum SubsidyType {
    TRANSPORT("TRANSPORT", "交通补贴"),
    ACCOMMODATION("ACCOMMODATION", "住宿补贴"),
    DAILY("DAILY", "日常补贴");

    private final String code;
    private final String desc;

    SubsidyType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SubsidyType fromCode(String code) {
        for (SubsidyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}