package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.TransferRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransferMapper extends BaseMapper<TransferRecord> {
}
