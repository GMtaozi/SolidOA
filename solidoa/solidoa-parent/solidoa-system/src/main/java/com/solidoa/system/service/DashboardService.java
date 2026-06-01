package com.solidoa.system.service;

import java.util.Map;

/**
 * 首页/仪表盘服务接口
 */
public interface DashboardService {

    /**
     * 获取首页统计数据
     */
    Map<String, Object> getDashboardStats(Long userId);

    /**
     * 获取最新消息通知
     */
    Map<String, Object> getRecentMessages(Long userId);

    /**
     * 获取底部统计数据
     */
    Map<String, Object> getFooterStats(Long userId);
}
