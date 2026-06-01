package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.OperLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperLogMapper extends BaseMapper<OperLog> {
}