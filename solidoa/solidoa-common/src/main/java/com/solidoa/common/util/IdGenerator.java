package com.solidoa.common.util;

import cn.hutool.core.util.IdUtil;

public class IdGenerator {

    public static String generateId() {
        return IdUtil.fastSimpleUUID();
    }

    public static long generateLongId() {
        return IdUtil.getSnowflakeNextId();
    }
}