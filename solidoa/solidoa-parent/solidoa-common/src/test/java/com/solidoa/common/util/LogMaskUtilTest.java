package com.solidoa.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogMaskUtilTest {

    @Test
    void testMaskPassword() {
        String json = "{\"password\":\"secret123\"}";
        String masked = LogMaskUtil.mask(json);
        assertFalse(masked.contains("secret123"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void testMaskMobile() {
        String json = "{\"mobile\":\"13800138000\"}";
        String masked = LogMaskUtil.mask(json);
        assertFalse(masked.contains("13800138000"));
        assertTrue(masked.contains("****"));
    }

    @Test
    void testMaskEmail() {
        String json = "{\"email\":\"test@example.com\"}";
        String masked = LogMaskUtil.mask(json);
        assertFalse(masked.contains("test@example.com"));
        assertTrue(masked.contains("****"));
    }

    @Test
    void testMaskSensitiveFields() {
        String json = "{\"password\":\"pass123\",\"token\":\"abc123\",\"secret\":\"xyz789\"}";
        String masked = LogMaskUtil.mask(json);
        assertFalse(masked.contains("pass123"));
        assertFalse(masked.contains("abc123"));
        assertFalse(masked.contains("xyz789"));
    }

    @Test
    void testNullOrEmptyInput() {
        assertNull(LogMaskUtil.mask(null));
        assertEquals("", LogMaskUtil.mask(""));
    }
}