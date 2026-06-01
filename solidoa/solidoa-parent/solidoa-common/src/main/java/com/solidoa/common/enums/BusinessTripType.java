package com.solidoa.common.enums;

import java.math.BigDecimal;

/**
 * 出差类型枚举
 */
public enum BusinessTripType {
    LOCAL("LOCAL", "本地出差", new BigDecimal("50")),
    SHORT("SHORT", "短途出差", new BigDecimal("150")),
    LONG("LONG", "长途出差", new BigDecimal("200"));

    private final String code;
    private final String desc;
    private final BigDecimal dailySubsidy;

    BusinessTripType(String code, String desc, BigDecimal dailySubsidy) {
        this.code = code;
        this.desc = desc;
        this.dailySubsidy = dailySubsidy;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public BigDecimal getDailySubsidy() {
        return dailySubsidy;
    }

    public static BusinessTripType fromCode(String code) {
        for (BusinessTripType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据天数自动计算出差类型
     */
    public static BusinessTripType calculateType(int days) {
        if (days <= 1) return LOCAL;
        if (days <= 3) return SHORT;
        return LONG;
    }
}