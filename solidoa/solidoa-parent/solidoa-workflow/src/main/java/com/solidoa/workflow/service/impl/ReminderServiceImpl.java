package com.solidoa.workflow.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.entity.ReminderRecord;
import com.solidoa.workflow.mapper.ReminderRecordMapper;
import com.solidoa.workflow.service.ReminderService;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.workflow.vo.ReminderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;

@Service
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private static final int MAX_REMINDER_PER_DAY = 3;
    private static final String REMINDER_KEY_PREFIX = "reminder:count:";

    @Autowired
    private ReminderRecordMapper reminderRecordMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void sendReminder(String businessType, Long businessId, Long requestUserId) {
        String today = LocalDate.now().toString();
        String key = REMINDER_KEY_PREFIX + businessType + ":" + businessId + ":" + today;

        // 使用 Lua 脚本确保 increment 和 expire 的原子性
        // 如果 key 不存在，先设置为 1 并设置过期时间；否则只 increment
        String luaScript =
            "local count = redis.call('GET', KEYS[1]) " +
            "if count == false then " +
            "    redis.call('SET', KEYS[1], 1) " +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "    return 1 " +
            "else " +
            "    return redis.call('INCR', KEYS[1]) " +
            "end";
        Long count = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(Duration.ofDays(1).toSeconds())
        );

        if (count != null && count > MAX_REMINDER_PER_DAY) {
            throw new BusinessException("今日催办次数已达上限（" + MAX_REMINDER_PER_DAY + "次），请明天再试");
        }

        // 查询当前催办记录
        ReminderRecord record = reminderRecordMapper.selectByBusinessAndDate(
            businessType, businessId, LocalDate.now());

        if (record == null) {
            record = new ReminderRecord();
            record.setBusinessType(businessType);
            record.setBusinessId(businessId);
            record.setApproverId(requestUserId);
            record.setReminderCount(1);
            record.setLastReminderTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            reminderRecordMapper.insert(record);
        } else {
            record.setReminderCount(record.getReminderCount() + 1);
            record.setLastReminderTime(LocalDateTime.now());
            reminderRecordMapper.updateById(record);
        }

        // B1 催办: 3 通道通知 (Sprint 4.6 集成钉钉)
        log.info("[B1] 催办: type={}, id={}, userId={}, count={}",
            businessType, businessId, requestUserId, count);
    }

    @Override
    public PageVO<ReminderVO> getReminderHistory(String businessType, Long businessId, PageDTO dto) {
        return null;
    }
}