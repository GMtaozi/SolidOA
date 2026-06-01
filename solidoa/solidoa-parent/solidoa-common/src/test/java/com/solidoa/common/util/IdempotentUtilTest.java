package com.solidoa.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdempotentUtilTest {

    @Test
    void testGenerateKey() {
        String key = IdempotentUtil.generateKey("LEAVE", 1L, "CREATE");
        assertNotNull(key);
        assertTrue(key.contains("LEAVE"));
        assertTrue(key.contains("1"));
        assertTrue(key.contains("CREATE"));
    }

    @Test
    void testGenerateKeyDifferentBusinessTypes() {
        String leaveKey = IdempotentUtil.generateKey("LEAVE", 1L, "CREATE");
        String expenseKey = IdempotentUtil.generateKey("EXPENSE", 1L, "CREATE");
        assertNotEquals(leaveKey, expenseKey);
    }

    @Test
    void testGenerateKeyDifferentBusinessIds() {
        String key1 = IdempotentUtil.generateKey("LEAVE", 1L, "CREATE");
        String key2 = IdempotentUtil.generateKey("LEAVE", 2L, "CREATE");
        assertNotEquals(key1, key2);
    }

    @Test
    void testGenerateKeyDifferentActions() {
        String key1 = IdempotentUtil.generateKey("LEAVE", 1L, "CREATE");
        String key2 = IdempotentUtil.generateKey("LEAVE", 1L, "APPROVE");
        assertNotEquals(key1, key2);
    }

    @Test
    void testGenerateMessageKey() {
        String key = IdempotentUtil.generateMessageKey("APPROVAL", "msg-123");
        assertNotNull(key);
        assertTrue(key.contains("msg:"));
        assertTrue(key.contains("APPROVAL"));
        assertTrue(key.contains("msg-123"));
    }

    @Test
    void testGenerateApiKey() {
        String key = IdempotentUtil.generateApiKey(100L, "/api/v1/leave", "req-456");
        assertNotNull(key);
        assertTrue(key.contains("api:"));
        assertTrue(key.contains("100"));
        assertTrue(key.contains("/api/v1/leave"));
        assertTrue(key.contains("req-456"));
    }
}