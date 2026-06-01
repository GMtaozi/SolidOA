package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.BusinessTripSubsidy;
import com.solidoa.hr.attendance.vo.SubsidyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 出差补贴记录Mapper
 */
@Mapper
public interface BusinessTripSubsidyMapper extends BaseMapper<BusinessTripSubsidy> {

    /**
     * 根据出差记录ID查询补贴列表
     */
    List<SubsidyVO> selectByTripId(@Param("tripId") Long tripId);

    /**
     * 批量查询多个出差记录的补贴信息（避免 N+1 查询）
     */
    List<SubsidyVO> selectByTripIds(@Param("tripIds") List<Long> tripIds);
}