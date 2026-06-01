package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.Purchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采购申请Mapper
 */
@Mapper
public interface PurchaseMapper extends BaseMapper<Purchase> {

    /**
     * 查询最大采购单号
     * 使用 CONCAT 函数确保安全，避免 SQL 注入
     */
    @Select("SELECT purchase_no FROM oa_purchase WHERE purchase_no LIKE CONCAT(#{prefix}, '%') ORDER BY purchase_no DESC LIMIT 1")
    String selectMaxPurchaseNo(@Param("prefix") String prefix);

    /**
     * 查询最大采购单号（使用 FOR UPDATE 行锁保证并发唯一性）
     */
    @Select("SELECT purchase_no FROM oa_purchase WHERE purchase_no LIKE CONCAT(#{prefix}, '%') ORDER BY purchase_no DESC LIMIT 1 FOR UPDATE")
    String selectMaxPurchaseNoForUpdate(@Param("prefix") String prefix);
}