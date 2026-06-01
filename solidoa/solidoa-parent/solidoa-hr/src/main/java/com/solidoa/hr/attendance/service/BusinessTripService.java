package com.solidoa.hr.attendance.service;

import com.solidoa.hr.attendance.form.BusinessTripForm;
import com.solidoa.hr.attendance.vo.BusinessTripVO;
import com.solidoa.common.vo.PageVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 出差申请服务接口
 */
public interface BusinessTripService {

    /**
     * 新建出差申请
     */
    Long createBusinessTrip(BusinessTripForm form, Long userId);

    /**
     * 出差列表（带用户身份验证）
     */
    PageVO<BusinessTripVO> listBusinessTrip(Long userId, int pageNum, int pageSize, String status,
                                           LocalDate startDate, LocalDate endDate);

    /**
     * 出差详情
     */
    BusinessTripVO getBusinessTripById(Long id);

    /**
     * 审批出差
     */
    void approveBusinessTrip(Long id, String comment, String approveType, Long approverId);

    /**
     * 撤回出差申请
     */
    void cancelBusinessTrip(Long id, Long userId);

    /**
     * 获取当前用户已通过的出差申请列表（用于报销关联）
     */
    List<BusinessTripVO> listApprovedBusinessTrips(Long userId);
}