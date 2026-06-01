package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.Stamp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用印申请Mapper
 */
@Mapper
public interface StampMapper extends BaseMapper<Stamp> {

    /**
     * 查询最大用印单号（使用 FOR UPDATE 行锁保证并发唯一性）
     * 在高并发场景下，防止多个请求同时获取相同最大单号导致单号重复
     */
    String selectMaxStampNoForUpdate(String prefix);
}
