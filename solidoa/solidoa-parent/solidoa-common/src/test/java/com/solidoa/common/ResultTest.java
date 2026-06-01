package com.solidoa.common.result;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("data");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("data", result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSuccessNullData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void testFail() {
        Result<Void> result = Result.fail("错误信息");
        assertEquals(500, result.getCode());
        assertEquals("错误信息", result.getMessage());
    }

    @Test
    void testFailWithCode() {
        Result<Void> result = Result.fail(400, "自定义错误");
        assertEquals(400, result.getCode());
        assertEquals("自定义错误", result.getMessage());
    }

    @Test
    void testTimestampNotZero() {
        Result<String> result = Result.success("test");
        assertTrue(result.getTimestamp() > 0);
    }
}