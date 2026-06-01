package com.solidoa.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdempotentUtilTest {

    @Test
    void testGenerateKey() {
        String key = IdempotentUtil.generateKey("POST", "/api/v1/expense");
        assertNotNull(key);
        assertTrue(key.contains("POST"));
        assertTrue(key.contains("expense"));
    }

    @Test
    void testGenerateKeyDifferentMethods() {
        String getKey = IdempotentUtil.generateKey("GET", "/api/v1/test");
        String postKey = IdempotentUtil.generateKey("POST", "/api/v1/test");
        assertNotEquals(getKey, postKey);
    }

    @Test
    void testGenerateKeyDifferentPaths() {
        String key1 = IdempotentUtil.generateKey("POST", "/api/v1/expense");
        String key2 = IdempotentUtil.generateKey("POST", "/api/v1/leave");
        assertNotEquals(key1, key2);
    }
}