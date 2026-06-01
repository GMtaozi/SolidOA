package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.entity.ApprovalRecord;
import com.solidoa.workflow.entity.ApprovalNodeDetail;
import com.solidoa.workflow.mapper.ApprovalRecordMapper;
import com.solidoa.workflow.mapper.ApprovalNodeDetailMapper;
import com.solidoa.workflow.mapper.ApprovalCcMapper;
import com.solidoa.workflow.service.ApprovalRecordService;
import com.solidoa.workflow.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 审批记录服务实现
 */
@Service
@Slf4j
public class ApprovalRecordServiceImpl implements ApprovalRecordService {

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private ApprovalNodeDetailMapper nodeDetailMapper;

    @Autowired
    private ApprovalCcMapper ccMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public IPage<ApprovalRecordVO> getMyApply(int pageNum, int pageSize, String businessType,
                                               String status, String startDate, String endDate,
                                               Long userId) {
        Page<ApprovalRecordVO> page = new Page<>(pageNum, pageSize);
        return recordMapper.selectMyApply(page, userId, businessType, status, startDate, endDate);
    }

    @Override
    public IPage<ApprovalRecordVO> getMyApproved(int pageNum, int pageSize, String businessType,
                                                  String status, String startDate, String endDate,
                                                  Long userId) {
        Page<ApprovalRecordVO> page = new Page<>(pageNum, pageSize);
        return recordMapper.selectMyApproved(page, userId, businessType, status, startDate, endDate);
    }

    @Override
    public IPage<ApprovalRecordVO> getAllRecord(int pageNum, int pageSize, String businessType,
                                                 String status, String startDate, String endDate,
                                                 Long userId, Long deptId) {
        Page<ApprovalRecordVO> page = new Page<>(pageNum, pageSize);
        return recordMapper.selectAllRecord(page, businessType, status, startDate, endDate, userId, deptId);
    }

    @Override
    public ApprovalRecordDetailVO getRecordDetail(Long id) {
        ApprovalRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }

        ApprovalRecordDetailVO detail = new ApprovalRecordDetailVO();
        detail.setId(record.getId());
        detail.setRecordNo(record.getRecordNo());
        detail.setBusinessType(record.getBusinessType());
        detail.setBusinessTypeName(getBusinessTypeName(record.getBusinessType()));
        detail.setBusinessId(record.getBusinessId());
        detail.setUserId(record.getUserId());
        detail.setUserName(record.getUserName());
        detail.setDeptId(record.getDeptId());
        detail.setDeptName(record.getDeptName());
        detail.setTitle(record.getTitle());
        detail.setAmount(record.getAmount());
        detail.setStartDate(record.getStartDate());
        detail.setEndDate(record.getEndDate());
        detail.setStatus(record.getStatus());
        detail.setStatusName(getStatusName(record.getStatus()));
        detail.setCurrentNode(record.getCurrentNode());
        detail.setTotalNodes(record.getTotalNodes());
        detail.setCompletedNodes(record.getCompletedNodes());
        detail.setProgress(record.getCompletedNodes() + "/" + record.getTotalNodes());
        detail.setCreateTime(record.getCreateTime());

        // 解析JSON内容
        if (record.getContent() != null && !record.getContent().isEmpty()) {
            try {
                detail.setContent(objectMapper.readValue(record.getContent(), Object.class));
            } catch (Exception e) {
                log.warn("解析审批记录内容失败: id={}", id, e);
                detail.setContent(record.getContent());
            }
        }

        // 查询节点列表
        List<ApprovalNodeDetailVO> nodes = nodeDetailMapper.selectVOByRecordId(id);
        detail.setNodes(nodes);

        // 查询抄送列表
        List<ApprovalCcVO> ccs = ccMapper.selectVOByRecordId(id);
        detail.setCcs(ccs);

        return detail;
    }

    @Override
    public ApprovalStatisticsVO getStatistics(String startDate, String endDate, Long deptId, Long userId) {
        ApprovalStatisticsVO statistics = new ApprovalStatisticsVO();

        // 构建查询条件
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(ApprovalRecord::getCreateTime, LocalDateTime.parse(startDate + " 00:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(ApprovalRecord::getCreateTime, LocalDateTime.parse(endDate + " 23:59:59",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (deptId != null) {
            wrapper.eq(ApprovalRecord::getDeptId, deptId);
        }

        // 统计总数
        long totalCount = recordMapper.selectCount(wrapper);
        statistics.setTotalCount(totalCount);

        // 统计待审批
        LambdaQueryWrapper<ApprovalRecord> pendingWrapper = wrapper.clone();
        pendingWrapper.eq(ApprovalRecord::getStatus, "PENDING");
        statistics.setPendingCount(recordMapper.selectCount(pendingWrapper));

        // 统计已通过
        LambdaQueryWrapper<ApprovalRecord> approvedWrapper = wrapper.clone();
        approvedWrapper.eq(ApprovalRecord::getStatus, "APPROVED");
        statistics.setApprovedCount(recordMapper.selectCount(approvedWrapper));

        // 统计已拒绝
        LambdaQueryWrapper<ApprovalRecord> rejectedWrapper = wrapper.clone();
        rejectedWrapper.eq(ApprovalRecord::getStatus, "REJECTED");
        statistics.setRejectedCount(recordMapper.selectCount(rejectedWrapper));

        // 统计已撤回
        LambdaQueryWrapper<ApprovalRecord> cancelledWrapper = wrapper.clone();
        cancelledWrapper.eq(ApprovalRecord::getStatus, "CANCELLED");
        statistics.setCancelledCount(recordMapper.selectCount(cancelledWrapper));

        // 统计总金额
        BigDecimal totalAmount = recordMapper.selectList(wrapper).stream()
            .map(ApprovalRecord::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.setTotalAmount(totalAmount);

        return statistics;
    }

    @Override
    public void exportRecord(String businessType, String status, String startDate, String endDate,
                            Long deptId, jakarta.servlet.http.HttpServletResponse response) {
        // 使用SXSSFWorkbook流式写入，每次在内存中只保留100行
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("审批记录");

            // 设置表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"记录编号", "业务类型", "申请人", "部门", "标题", "金额", "状态", "当前节点", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 构建查询条件（不包含排序和limit，供分页查询使用）
            LambdaQueryWrapper<ApprovalRecord> baseWrapper = new LambdaQueryWrapper<>();
            if (businessType != null && !businessType.isEmpty()) {
                baseWrapper.eq(ApprovalRecord::getBusinessType, businessType);
            }
            if (status != null && !status.isEmpty()) {
                baseWrapper.eq(ApprovalRecord::getStatus, status);
            }
            if (startDate != null && !startDate.isEmpty()) {
                baseWrapper.ge(ApprovalRecord::getCreateTime, LocalDateTime.parse(startDate + " 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (endDate != null && !endDate.isEmpty()) {
                baseWrapper.le(ApprovalRecord::getCreateTime, LocalDateTime.parse(endDate + " 23:59:59",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (deptId != null) {
                baseWrapper.eq(ApprovalRecord::getDeptId, deptId);
            }
            baseWrapper.orderByDesc(ApprovalRecord::getCreateTime);

            // 分页导出：每次查询1000条
            final int PAGE_SIZE = 1000;
            int rowNum = 1;
            int currentPage = 1;

            while (true) {
                Page<ApprovalRecord> page = new Page<>(currentPage, PAGE_SIZE);
                IPage<ApprovalRecord> result = recordMapper.selectPage(page, baseWrapper);
                List<ApprovalRecord> records = result.getRecords();

                if (records == null || records.isEmpty()) {
                    break;
                }

                // 填充数据
                for (ApprovalRecord record : records) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(record.getRecordNo());
                    row.createCell(1).setCellValue(getBusinessTypeName(record.getBusinessType()));
                    row.createCell(2).setCellValue(record.getUserName());
                    row.createCell(3).setCellValue(record.getDeptName());
                    row.createCell(4).setCellValue(record.getTitle());
                    row.createCell(5).setCellValue(record.getAmount() != null ? record.getAmount().doubleValue() : 0);
                    row.createCell(6).setCellValue(getStatusName(record.getStatus()));
                    row.createCell(7).setCellValue(record.getCurrentNode());
                    row.createCell(8).setCellValue(record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }

                // 如果当前页记录数小于PAGE_SIZE，说明是最后一页，退出循环
                if (records.size() < PAGE_SIZE) {
                    break;
                }
                currentPage++;
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = "审批记录_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));

            workbook.write(response.getOutputStream());
            workbook.dispose();
        } catch (Exception e) {
            log.error("导出审批记录失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Long createRecord(String businessType, Long businessId, Long userId, String userName,
                             Long deptId, String deptName, String title, String content,
                             BigDecimal amount, LocalDate startDate, LocalDate endDate) {
        ApprovalRecord record = new ApprovalRecord();
        record.setRecordNo(generateRecordNo());
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setUserId(userId);
        record.setUserName(userName);
        record.setDeptId(deptId);
        record.setDeptName(deptName);
        record.setTitle(title);
        record.setContent(content);
        record.setAmount(amount);
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setStatus("PENDING");
        record.setCurrentNode("审批节点1");
        record.setTotalNodes(1);
        record.setCompletedNodes(0);
        record.setCreateTime(LocalDateTime.now());

        recordMapper.insert(record);

        log.info("创建审批记录: id={}, no={}, businessType={}, businessId={}",
            record.getId(), record.getRecordNo(), businessType, businessId);

        return record.getId();
    }

    // 使用数据库行锁保证并发唯一性（替代 synchronized 方法）
    private String generateRecordNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "AR" + date;
        // 使用 FOR UPDATE 行锁
        String maxNo = recordMapper.selectMaxRecordNoByPrefixForUpdate(prefix);
        long seq = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                seq = Long.parseLong(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private String getBusinessTypeName(String businessType) {
        if (businessType == null) return "";
        return switch (businessType) {
            case "LEAVE" -> "请假";
            case "EXPENSE" -> "报销";
            case "PURCHASE" -> "采购";
            case "SALARY" -> "工资";
            case "STAMP" -> "用印";
            case "OVERTIME" -> "加班";
            case "BUSINESS_TRIP" -> "出差";
            case "REPAIR_CARD" -> "补卡";
            case "OUTING" -> "外出";
            default -> businessType;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "审批中";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "CANCELLED" -> "已撤回";
            default -> status;
        };
    }
}