package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.BusinessTrip;
import com.solidoa.hr.attendance.vo.BusinessTripVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * 出差申请Mapper
 */
@Mapper
public interface BusinessTripMapper extends BaseMapper<BusinessTrip> {

    /**
     * 分页查询出差列表
     */
    List<BusinessTripVO> selectPageList(@Param("offset") int offset,
                                       @Param("limit") int limit,
                                       @Param("userId") Long userId,
                                       @Param("status") String status,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * 查询总数
     */
    long selectCount(@Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

    /**
     * 查询详情
     */
    BusinessTripVO selectDetailById(@Param("id") Long id);

    /**
     * 查询用户已通过的出差申请（用于报销关联）
     */
    List<BusinessTripVO> selectApprovedByUserId(@Param("userId") Long userId);
}