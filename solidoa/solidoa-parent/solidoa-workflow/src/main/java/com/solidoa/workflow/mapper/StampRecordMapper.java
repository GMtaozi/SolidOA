package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.StampRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物理用印记录Mapper
 */
@Mapper
public interface StampRecordMapper extends BaseMapper<StampRecord> {
}