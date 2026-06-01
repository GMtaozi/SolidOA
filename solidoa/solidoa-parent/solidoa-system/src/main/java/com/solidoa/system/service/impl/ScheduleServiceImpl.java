package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.entity.Schedule;
import com.solidoa.system.form.ScheduleForm;
import com.solidoa.system.mapper.ScheduleMapper;
import com.solidoa.system.service.ScheduleService;
import com.solidoa.system.vo.ScheduleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public Long create(ScheduleForm form, Long userId) {
        validateTimeRange(form.getStartTime(), form.getEndTime());
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(form, schedule);
        schedule.setUserId(userId);
        schedule.setStatus("NORMAL");
        scheduleMapper.insert(schedule);
        log.info("创建日程: id={}, userId={}", schedule.getId(), userId);
        return schedule.getId();
    }

    @Override
    public void update(Long id, ScheduleForm form, Long userId) {
        validateTimeRange(form.getStartTime(), form.getEndTime());
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此日程");
        }
        BeanUtils.copyProperties(form, schedule);
        scheduleMapper.updateById(schedule);
        log.info("更新日程: id={}", id);
    }

    @Override
    public void delete(Long id, Long userId) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此日程");
        }
        scheduleMapper.deleteById(id);
        log.info("删除日程: id={}", id);
    }

    @Override
    public ScheduleVO getById(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            return null;
        }
        ScheduleVO vo = new ScheduleVO();
        BeanUtils.copyProperties(schedule, vo);
        return vo;
    }

    @Override
    public List<ScheduleVO> listByDateRange(LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        List<Schedule> list = scheduleMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
        return list.stream().map(s -> {
            ScheduleVO vo = new ScheduleVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleVO> listAllByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Schedule> list = scheduleMapper.selectByDateRange(startDate, endDate);
        return list.stream().map(s -> {
            ScheduleVO vo = new ScheduleVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 校验日程时间范围：开始时间必须早于结束时间
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return; // 允许空值，由数据库约束或其他校验处理
        }
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("日程开始时间不能晚于结束时间");
        }
    }
}