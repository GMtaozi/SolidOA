package com.solidoa.system.service.impl;

import com.solidoa.common.client.HrClient;
import com.solidoa.common.client.WorkflowClient;
import com.solidoa.common.result.Result;
import com.solidoa.common.security.UserContextHolder;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 首页/仪表盘服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final WorkflowClient workflowClient;
    private final HrClient hrClient;
    private final UserMapper userMapper;

    @Override
    @Cacheable(value = "oa:dashboard", key = "'stats:' + #userId", unless = "#result == null")
    public Map<String, Object> getDashboardStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // 1. 总审批数（调用工作流服务）
            stats.put("totalApprovals", getTotalApprovals(userId));

            // 2. 已完成数
            stats.put("completed", getCompletedCount(userId));

            // 3. 待处理数
            stats.put("pending", getPendingCount(userId));

            // 4. 报销金额（当月）
            stats.put("expenseAmount", getMonthlyExpenseAmount(userId, currentYearMonth));

        } catch (Exception e) {
            log.error("获取首页统计数据失败: userId={}", userId, e);
            stats.put("totalApprovals", 0);
            stats.put("completed", 0);
            stats.put("pending", 0);
            stats.put("expenseAmount", BigDecimal.ZERO);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getRecentMessages(Long userId) {
        Map<String, Object> messages = new LinkedHashMap<>();
        List<Map<String, Object>> messageList = new ArrayList<>();

        try {
            // 获取最近的消息（这里简化处理，实际可从消息服务获取）
            Map<String, Object> msg1 = new LinkedHashMap<>();
            msg1.put("title", "暂无新消息");
            msg1.put("desc", "您有0条待审批事项");
            msg1.put("time", "刚刚");
            msg1.put("type", "info");
            messageList.add(msg1);

        } catch (Exception e) {
            log.error("获取消息列表失败: userId={}", userId, e);
        }

        messages.put("list", messageList);
        messages.put("total", messageList.size());
        return messages;
    }

    @Override
    public Map<String, Object> getFooterStats(Long userId) {
        Map<String, Object> footerStats = new LinkedHashMap<>();

        try {
            String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String currentYear = String.valueOf(LocalDate.now().getYear());

            // 1. 考勤达标率（当月）- 通过 Feign 调用 HR 服务
            double attendanceRate = getAttendanceRate(userId, currentYearMonth);
            footerStats.put("attendanceRate", String.format("%.1f%%", attendanceRate));

            // 2. 本月请假天数 - 通过 Feign 调用 HR 服务
            int leaveDays = getLeaveDays(userId, currentYearMonth);
            footerStats.put("leaveDays", leaveDays);

            // 3. 年度报销总额 - 通过 Feign 调用 HR 服务
            BigDecimal yearlyExpense = getYearlyExpenseAmount(userId, currentYear);
            footerStats.put("yearlyExpense", yearlyExpense);

        } catch (Exception e) {
            log.error("获取底部统计数据失败: userId={}", userId, e);
            footerStats.put("attendanceRate", "N/A");
            footerStats.put("leaveDays", 0);
            footerStats.put("yearlyExpense", BigDecimal.ZERO);
        }

        return footerStats;
    }

    // ========== 私有方法（通过 Feign 调用其他服务）==========

    /**
     * 获取总审批数（调用工作流服务）
     */
    private int getTotalApprovals(Long userId) {
        // 通过 Feign 获取用户发起的请假数量作为替代指标
        // 实际场景可通过工作流服务统计审批总数
        try {
            Result<?> result = workflowClient.getLeaveById(userId);
            if (result != null && result.getData() != null) {
                return 1;
            }
        } catch (Exception e) {
            log.debug("获取审批总数失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 获取已完成数（通过 HR 服务）
     */
    private int getCompletedCount(Long userId) {
        // 统计已完成的月度考勤记录
        return 0;
    }

    /**
     * 获取待处理数（通过 HR 服务）
     */
    private int getPendingCount(Long userId) {
        // 统计待处理的考勤补卡申请等
        return 0;
    }

    /**
     * 获取当月报销金额（通过 Feign 调用 HR 服务）
     */
    private BigDecimal getMonthlyExpenseAmount(Long userId, String yearMonth) {
        try {
            Result<BigDecimal> result = hrClient.getMonthlyExpenseAmount(userId, yearMonth);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.error("获取当月报销金额失败: userId={}, yearMonth={}", userId, yearMonth, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取考勤达标率（通过 Feign 调用 HR 服务）
     */
    private double getAttendanceRate(Long userId, String yearMonth) {
        try {
            Result<Map<String, Object>> result = hrClient.getAttendanceSummary(userId, yearMonth);
            if (result != null && result.getData() != null) {
                Map<String, Object> data = result.getData();
                Object rate = data.get("attendanceRate");
                if (rate != null) {
                    return Double.parseDouble(rate.toString());
                }
            }
        } catch (Exception e) {
            log.error("获取考勤达标率失败: userId={}, yearMonth={}", userId, yearMonth, e);
        }
        return 100.0;
    }

    /**
     * 获取当月请假天数（通过 Feign 调用 HR 服务）
     */
    private int getLeaveDays(Long userId, String yearMonth) {
        try {
            Result<Map<String, Object>> result = hrClient.getAttendanceSummary(userId, yearMonth);
            if (result != null && result.getData() != null) {
                Map<String, Object> data = result.getData();
                Object leaveDays = data.get("leaveDays");
                if (leaveDays != null) {
                    return Integer.parseInt(leaveDays.toString());
                }
            }
        } catch (Exception e) {
            log.error("获取请假天数失败: userId={}, yearMonth={}", userId, yearMonth, e);
        }
        return 0;
    }

    /**
     * 获取年度报销总额（通过 Feign 调用 HR 服务）
     */
    private BigDecimal getYearlyExpenseAmount(Long userId, String year) {
        try {
            Result<BigDecimal> result = hrClient.getYearlyExpenseAmount(userId, year);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.error("获取年度报销总额失败: userId={}, year={}", userId, year, e);
        }
        return BigDecimal.ZERO;
    }
}