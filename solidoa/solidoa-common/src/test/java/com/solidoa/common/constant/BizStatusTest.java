package com.solidoa.common.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BizStatusTest {

    @Test
    void testApprovalStatus() {
        assertEquals("DRAFT", BizStatus.DRAFT);
        assertEquals("SUBMITTED", BizStatus.SUBMITTED);
        assertEquals("PENDING", BizStatus.PENDING);
        assertEquals("APPROVED", BizStatus.APPROVED);
        assertEquals("REJECTED", BizStatus.REJECTED);
        assertEquals("CANCELLED", BizStatus.CANCELLED);
    }

    @Test
    void testExpenseStatus() {
        assertEquals("PENDING", BizStatus.EXPENSE_PENDING);
        assertEquals("APPROVED", BizStatus.EXPENSE_APPROVED);
        assertEquals("COMPLETED", BizStatus.COMPLETED);
        assertEquals("REJECT", BizStatus.REJECT);
    }

    @Test
    void testAttendanceStatus() {
        assertEquals("NORMAL", BizStatus.NORMAL);
        assertEquals("LATE", BizStatus.LATE);
        assertEquals("EARLY_LEAVE", BizStatus.EARLY_LEAVE);
        assertEquals("ABSENT", BizStatus.ABSENT);
        assertEquals("ON_LEAVE", BizStatus.ON_LEAVE);
        assertEquals("BUSINESS_TRIP", BizStatus.BUSINESS_TRIP);
    }

    @Test
    void testMessageType() {
        assertEquals("SYSTEM", BizStatus.MSG_SYSTEM);
        assertEquals("APPROVAL", BizStatus.MSG_APPROVAL);
        assertEquals("REMINDER", BizStatus.MSG_REMINDER);
    }

    @Test
    void testPriorityLevels() {
        assertEquals(0, BizStatus.PRIORITY_LOW);
        assertEquals(1, BizStatus.PRIORITY_NORMAL);
        assertEquals(2, BizStatus.PRIORITY_HIGH);
        assertEquals(3, BizStatus.PRIORITY_URGENT);
    }

    @Test
    void testPriorityAscending() {
        assertTrue(BizStatus.PRIORITY_LOW < BizStatus.PRIORITY_NORMAL);
        assertTrue(BizStatus.PRIORITY_NORMAL < BizStatus.PRIORITY_HIGH);
        assertTrue(BizStatus.PRIORITY_HIGH < BizStatus.PRIORITY_URGENT);
    }
}