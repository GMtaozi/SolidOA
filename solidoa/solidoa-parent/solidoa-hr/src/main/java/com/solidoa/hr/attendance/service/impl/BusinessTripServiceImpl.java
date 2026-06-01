package com.solidoa.hr.attendance.service.impl;

import com.solidoa.hr.attendance.entity.BusinessTrip;
import com.solidoa.hr.attendance.entity.BusinessTripSubsidy;
import com.solidoa.hr.attendance.form.BusinessTripForm;
import com.solidoa.hr.attendance.mapper.BusinessTripMapper;
import com.solidoa.hr.attendance.mapper.BusinessTripSubsidyMapper;
import com.solidoa.hr.attendance.service.BusinessTripService;
import com.solidoa.hr.attendance.vo.BusinessTripVO;
import com.solidoa.hr.attendance.vo.SubsidyVO;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 出差申请服务实现
 */
@Service
@Slf4j
public class BusinessTripServiceImpl implements BusinessTripService {

    @Autowired
    private BusinessTripMapper businessTripMapper;

    @Autowired
    private BusinessTripSubsidyMapper subsidyMapper;

    @Override
    @Transactional
    public Long createBusinessTrip(BusinessTripForm form, Long userId) {
        // 校验日期
        if (form.getEndDate().isBefore(form.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        // 计算出差天数
        long daysBetween = ChronoUnit.DAYS.between(form.getStartDate(), form.getEndDate()) + 1;
        BigDecimal days = BigDecimal.valueOf(daysBetween);

        // 生成出差单号
        String tripNo = generateTripNo();

        BusinessTrip trip = new BusinessTrip();
        trip.setTripNo(tripNo);
        trip.setUserId(userId);
        trip.setDestination(form.getDestination());
        trip.setTripType(form.getTripType());
        trip.setStartDate(form.getStartDate());
        trip.setEndDate(form.getEndDate());
        trip.setDays(days);
        trip.setReason(form.getReason());
        trip.setBudgetAmount(form.getBudgetAmount());
        trip.setBudgetRemark(form.getBudgetRemark());
        trip.setAttachments(form.getAttachments());
        trip.setEmergencyContact(form.getEmergencyContact());
        trip.setEmergencyPhone(form.getEmergencyPhone());
        trip.setStatus("PENDING");
        trip.setCreateTime(LocalDateTime.now());
        trip.setUpdateTime(LocalDateTime.now());

        businessTripMapper.insert(trip);
        log.info("新建出差申请: tripNo={}, userId={}, destination={}, days={}",
                tripNo, userId, form.getDestination(), days);

        return trip.getId();
    }

    @Override
    public PageVO<BusinessTripVO> listBusinessTrip(Long userId, int pageNum, int pageSize, String status,
                                                   LocalDate startDate, LocalDate endDate) {
        int offset = (pageNum - 1) * pageSize;
        List<BusinessTripVO> records = businessTripMapper.selectPageList(
                offset, pageSize, userId, status, startDate, endDate);
        long total = businessTripMapper.selectCount(userId, status, startDate, endDate);

        PageVO<BusinessTripVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public BusinessTripVO getBusinessTripById(Long id) {
        BusinessTripVO vo = businessTripMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException("出差记录不存在");
        }

        // 查询补贴信息
        List<SubsidyVO> subsidies = subsidyMapper.selectByTripId(id);
        vo.setSubsidies(subsidies);
        vo.setTotalSubsidy(calculateTotalSubsidy(subsidies));

        // 格式化时间
        if (vo.getCreateTime() != null) {
            vo.setCreateTimeStr(vo.getCreateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        return vo;
    }

    @Override
    @Transactional
    public void approveBusinessTrip(Long id, String comment, String approveType, Long approverId) {
        BusinessTrip trip = businessTripMapper.selectById(id);
        if (trip == null) {
            throw new BusinessException("出差记录不存在");
        }

        if (!"PENDING".equals(trip.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        if ("APPROVE".equals(approveType)) {
            trip.setStatus("APPROVED");
            log.info("出差申请审批通过: tripNo={}, approverId={}", trip.getTripNo(), approverId);
        } else if ("REJECT".equals(approveType)) {
            trip.setStatus("REJECTED");
            log.info("出差申请审批拒绝: tripNo={}, approverId={}, reason={}",
                    trip.getTripNo(), approverId, comment);
        } else {
            throw new BusinessException("无效的审批类型");
        }

        trip.setCurrentApproverId(approverId);
        trip.setUpdateTime(LocalDateTime.now());
        businessTripMapper.updateById(trip);
    }

    @Override
    @Transactional
    public void cancelBusinessTrip(Long id, Long userId) {
        BusinessTrip trip = businessTripMapper.selectById(id);
        if (trip == null) {
            throw new BusinessException("出差记录不存在");
        }

        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的出差申请");
        }

        if (!"PENDING".equals(trip.getStatus())) {
            throw new BusinessException("当前状态不允许撤回");
        }

        trip.setStatus("CANCELLED");
        trip.setUpdateTime(LocalDateTime.now());
        businessTripMapper.updateById(trip);
        log.info("出差申请撤回: tripNo={}, userId={}", trip.getTripNo(), userId);
    }

    /**
     * 生成出差单号
     */
    private String generateTripNo() {
        String prefix = "BT";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + date + uuid;
    }

    /**
     * 计算总补贴金额
     */
    private BigDecimal calculateTotalSubsidy(List<SubsidyVO> subsidies) {
        if (subsidies == null || subsidies.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return subsidies.stream()
                .map(SubsidyVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<BusinessTripVO> listApprovedBusinessTrips(Long userId) {
        List<BusinessTripVO> trips = businessTripMapper.selectApprovedByUserId(userId);
        if (trips.isEmpty()) {
            return trips;
        }
        // 批量查询所有补贴，避免 N+1 查询
        List<Long> tripIds = trips.stream().map(BusinessTripVO::getId).collect(Collectors.toList());
        List<SubsidyVO> allSubsidies = subsidyMapper.selectByTripIds(tripIds);
        Map<Long, List<SubsidyVO>> subsidiesMap = allSubsidies.stream()
                .collect(Collectors.groupingBy(SubsidyVO::getTripId));

        for (BusinessTripVO vo : trips) {
            List<SubsidyVO> subsidies = subsidiesMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setSubsidies(subsidies);
            vo.setTotalSubsidy(calculateTotalSubsidy(subsidies));
        }
        return trips;
    }
}