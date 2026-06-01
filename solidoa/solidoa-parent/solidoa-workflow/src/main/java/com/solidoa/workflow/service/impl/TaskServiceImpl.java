package com.solidoa.workflow.service.impl;

import com.solidoa.workflow.mapper.LeaveMapper;
import com.solidoa.workflow.mapper.ExpenseMapper;
import com.solidoa.workflow.mapper.ApprovalRecordMapper;
import com.solidoa.workflow.service.TaskService;
import com.solidoa.workflow.vo.TaskVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    /**
     * 单表查询最大数据量限制，防止内存溢出
     * 当数据量超过此限制时，使用近似分页策略
     */
    private static final int MAX_FETCH_SIZE = 5000;

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    public PageVO<TaskVO> getMyTasks(Long userId, PageDTO dto) {
        int pageSize = dto.getPageSize();
        int pageNum = dto.getPageNum();

        // 首先查询总数量
        long total = leaveMapper.countMyTaskList(userId, "LEAVE") + expenseMapper.countMyTaskList(userId, "EXPENSE");

        // 全局 offset
        int offset = (pageNum - 1) * pageSize;

        // 查询所有数据后再进行分页截取（避免跨表分页导致数据丢失）
        List<TaskVO> allTasks = new ArrayList<>();
        allTasks.addAll(leaveMapper.selectMyTaskListPage(userId, "LEAVE", 0, MAX_FETCH_SIZE));
        allTasks.addAll(expenseMapper.selectMyTaskListPage(userId, "EXPENSE", 0, MAX_FETCH_SIZE));

        // 按申请时间倒序排序
        allTasks.sort((a, b) -> {
            if (a.getApplyTime() == null) return 1;
            if (b.getApplyTime() == null) return -1;
            return b.getApplyTime().compareTo(a.getApplyTime());
        });

        // 全局分页截取
        int fromIndex = Math.min(offset, allTasks.size());
        int toIndex = Math.min(offset + pageSize, allTasks.size());
        List<TaskVO> pagedList = allTasks.subList(fromIndex, toIndex);

        PageVO<TaskVO> page = new PageVO<>();
        page.setRecords(pagedList);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public PageVO<TaskVO> getPendingTasks(Long userId, PageDTO dto) {
        int pageSize = dto.getPageSize();
        int pageNum = dto.getPageNum();

        // 首先查询总数量
        long total = leaveMapper.countPendingTaskList(userId) + expenseMapper.countPendingTaskList(userId);

        // 全局 offset
        int offset = (pageNum - 1) * pageSize;

        // 查询所有数据后再进行分页截取
        List<TaskVO> allTasks = new ArrayList<>();
        allTasks.addAll(leaveMapper.selectPendingTaskListPage(userId, 0, MAX_FETCH_SIZE));
        allTasks.addAll(expenseMapper.selectPendingTaskListPage(userId, 0, MAX_FETCH_SIZE));

        // 按申请时间倒序排序
        allTasks.sort((a, b) -> {
            if (a.getApplyTime() == null) return 1;
            if (b.getApplyTime() == null) return -1;
            return b.getApplyTime().compareTo(a.getApplyTime());
        });

        // 全局分页截取
        int fromIndex = Math.min(offset, allTasks.size());
        int toIndex = Math.min(offset + pageSize, allTasks.size());
        List<TaskVO> pagedList = allTasks.subList(fromIndex, toIndex);

        PageVO<TaskVO> page = new PageVO<>();
        page.setRecords(pagedList);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public PageVO<TaskVO> getProcessedTasks(Long userId, PageDTO dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();

        long total = approvalRecordMapper.countProcessedTaskList(userId);
        List<TaskVO> pageList = approvalRecordMapper.selectProcessedTaskListPage(userId, offset, dto.getPageSize());

        PageVO<TaskVO> page = new PageVO<>();
        page.setRecords(pageList);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }
}