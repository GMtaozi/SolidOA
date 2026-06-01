package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.PurchaseItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购明细Mapper
 */
@Mapper
public interface PurchaseItemMapper extends BaseMapper<PurchaseItem> {
}