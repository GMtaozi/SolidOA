package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.ReminderRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;

@Mapper
public interface ReminderRecordMapper extends BaseMapper<ReminderRecord> {
    ReminderRecord selectByBusinessAndDate(
        @Param("businessType") String businessType,
        @Param("businessId") Long businessId,
        @Param("date") LocalDate date);
}