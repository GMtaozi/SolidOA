package com.solidoa.common.enums;

/**
 * 补卡类型枚举
 */
public enum RepairType {
    MISSING("MISSING", "缺卡"),
    LATE("LATE", "迟到"),
    EARLY_LEAVE("EARLY_LEAVE", "早退");

    private final String code;
    private final String desc;

    RepairType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static RepairType fromCode(String code) {
        for (RepairType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}