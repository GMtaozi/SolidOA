package com.solidoa.workflow.service;

import com.solidoa.workflow.vo.ApprovalCcVO;
import java.util.List;

/**
 * 抄送服务接口
 */
public interface ApprovalCcService {

    /**
     * 根据业务创建抄送记录
     */
    void createCcRecords(String businessType, Long businessId);

    /**
     * 获取我的抄送列表
     * @param page 页码（裸int参数，与其他Service接口分页风格保持一致）
     * @param size 每页数量
     */
    List<ApprovalCcVO> getMyCcList(Long userId, int page, int size);

    /**
     * 获取我的未读抄送数量
     */
    int getMyUnreadCount(Long userId);

    /**
     * 标记抄送已读
     */
    void markAsRead(Long ccId, Long userId);

    /**
     * 发送抄送通知
     */
    void notifyCcUsers(String businessType, Long businessId, String event);
}
