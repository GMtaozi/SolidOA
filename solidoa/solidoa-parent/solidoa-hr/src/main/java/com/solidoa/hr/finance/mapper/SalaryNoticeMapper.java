package com.solidoa.hr.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.finance.entity.SalaryNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SalaryNoticeMapper extends BaseMapper<SalaryNotice> {

    List<SalaryNotice> selectBySalaryId(@Param("salaryId") Long salaryId);

    List<SalaryNotice> selectPendingList();
}