package com.solidoa.hr.finance.service.impl;

import com.solidoa.hr.finance.mapper.ExpenseMapper;
import com.solidoa.hr.finance.service.ReportService;
import com.solidoa.hr.finance.vo.DeptExpenseReportVO;
import com.solidoa.hr.finance.vo.ExpenseVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    // 使用系统临时目录，兼容 Windows 和 Unix
    private static final String EXPORT_DIR = System.getProperty("java.io.tmpdir") + File.separator + "finance-exports" + File.separator;
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ExportProgress> exportTasks = new ConcurrentHashMap<>();

    @Override
    public List<DeptExpenseReportVO> getDeptExpenseReport(Integer year, Integer month) {
        // 查询部门费用汇总
        List<DeptExpenseReportVO> reports = expenseMapper.selectDeptExpenseReport(year, month);
        log.info("生成部门费用报表: year={}, month={}, count={}", year, month, reports.size());
        return reports;
    }

    @Override
    public String exportExpenseReportAsync(Integer year, Integer month, Long userId) {
        String taskId = "export_" + System.currentTimeMillis() + "_" + userId;

        // 记录导出任务
        ExportProgress progress = new ExportProgress();
        progress.setTaskId(taskId);
        progress.setStatus("PROCESSING");
        progress.setProgress(0);
        progress.setCreateTime(LocalDateTime.now());
        exportTasks.put(taskId, progress);

        // 异步执行导出（通过代理调用以确保 @Async 生效）
        ReportService proxy = applicationContext.getBean(ReportService.class);
        proxy.exportExpenseReport(taskId, year, month, userId);

        return taskId;
    }

    @Override
    public Map<String, Object> getExportProgress(String taskId) {
        ExportProgress progress = exportTasks.get(taskId);
        if (progress == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", progress.getStatus());
        result.put("progress", progress.getProgress());
        result.put("downloadUrl", progress.getFilePath());
        result.put("message", progress.getMessage());
        return result;
    }

    @Async
    public void exportExpenseReport(String taskId, Integer year, Integer month, Long userId) {
        ExportProgress progress = exportTasks.get(taskId);
        FileOutputStream fos = null;
        SXSSFWorkbook workbook = null;

        try {
            // 确保导出目录存在，并检查创建是否成功
            File exportDir = new File(EXPORT_DIR);
            if (!exportDir.exists()) {
                boolean created = exportDir.mkdirs();
                if (!created) {
                    log.error("创建导出目录失败: {}", EXPORT_DIR);
                    progress.setStatus("FAILED");
                    progress.setMessage("系统错误：无法创建导出目录");
                    return;
                }
            }

            // 生成文件名
            String fileName = "expense_report_" + year + "_" + month + "_" + System.currentTimeMillis() + ".xlsx";
            String filePath = EXPORT_DIR + fileName;

            // 使用 SXSSFWorkbook 流式写入，内存中保留 100 行
            workbook = new SXSSFWorkbook(100);
            Sheet sheet = workbook.createSheet("报销报表");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"报销单号", "申请人", "部门", "报销类型", "金额", "状态", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据（分页查询防止 OOM）
            int rowNum = 1;
            int offset = 0;
            int totalProcessed = 0;
            long totalCount = expenseMapper.selectExportCount(year, month);

            while (true) {
                List<ExpenseVO> pageData = expenseMapper.selectExportPage(offset, BATCH_SIZE, year, month);
                if (pageData.isEmpty()) {
                    break;
                }

                for (ExpenseVO expense : pageData) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(expense.getExpenseNo());
                    row.createCell(1).setCellValue(expense.getUserName());
                    row.createCell(2).setCellValue(expense.getDeptName());
                    row.createCell(3).setCellValue(expense.getExpenseType());
                    row.createCell(4).setCellValue(expense.getAmount() != null ? expense.getAmount().setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                    row.createCell(5).setCellValue(expense.getStatus());
                    row.createCell(6).setCellValue(expense.getCreateTime() != null ?
                        expense.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                }

                totalProcessed += pageData.size();
                offset += BATCH_SIZE;

                // 更新进度
                int progressPercent = totalCount > 0 ? (int) (totalProcessed * 100 / totalCount) : 100;
                progress.setProgress(Math.min(progressPercent, 99));
            }

            // 写入文件
            fos = new FileOutputStream(filePath);
            workbook.write(fos);

            // 完成
            progress.setStatus("COMPLETED");
            progress.setProgress(100);
            progress.setFilePath("/api/v1/finance/report/download/" + taskId);
            progress.setMessage("导出完成");

            log.info("导出报表完成: taskId={}, filePath={}", taskId, filePath);

        } catch (Exception e) {
            log.error("导出报表失败: taskId={}", taskId, e);
            progress.setStatus("FAILED");
            progress.setMessage("导出失败: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                    workbook.dispose(); // 删除临时文件
                } catch (Exception ignored) {}
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private static class ExportProgress {
        private String taskId;
        private String status;
        private int progress;
        private String filePath;
        private String message;
        private LocalDateTime createTime;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }
}