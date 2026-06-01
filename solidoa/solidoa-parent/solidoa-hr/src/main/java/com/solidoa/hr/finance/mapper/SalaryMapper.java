package com.solidoa.hr.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.finance.entity.Salary;
import com.solidoa.hr.finance.vo.SalaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SalaryMapper extends BaseMapper<Salary> {

    SalaryVO selectVOById(@Param("id") Long id);

    List<SalaryVO> selectPageList(@Param("offset") int offset,
                                    @Param("limit") int limit,
                                    @Param("yearMonth") String yearMonth,
                                    @Param("status") String status,
                                    @Param("deptId") Long deptId);

    long selectCount(@Param("yearMonth") String yearMonth,
                      @Param("status") String status,
                      @Param("deptId") Long deptId);

    List<SalaryVO> selectByUserId(@Param("userId") Long userId,
                                  @Param("startMonth") String startMonth,
                                  @Param("endMonth") String endMonth);

    List<SalaryVO> selectExportList(@Param("yearMonth") String yearMonth,
                                     @Param("deptId") Long deptId);

    int selectExists(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);

    int insertBatchSomeColumn(@Param("list") List<Salary> salaryList);

    /**
     * 使用行锁获取当天最大工资单号，用于防止并发重复
     */
    String selectMaxSalaryNoForUpdate(@Param("prefix") String prefix);
}