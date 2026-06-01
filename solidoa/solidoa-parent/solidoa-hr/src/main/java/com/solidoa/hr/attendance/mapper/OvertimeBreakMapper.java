package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.OvertimeBreak;
import org.apache.ibatis.annotations.Mapper;

/**
 * 加班调休记录Mapper
 */
@Mapper
public interface OvertimeBreakMapper extends BaseMapper<OvertimeBreak> {
}