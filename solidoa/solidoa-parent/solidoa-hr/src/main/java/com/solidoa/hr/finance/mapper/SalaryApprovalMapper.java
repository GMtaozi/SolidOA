package com.solidoa.hr.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.finance.entity.SalaryApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SalaryApprovalMapper extends BaseMapper<SalaryApproval> {

    List<SalaryApproval> selectByYearMonth(@Param("yearMonth") String yearMonth);

    List<SalaryApproval> selectPendingList();
}