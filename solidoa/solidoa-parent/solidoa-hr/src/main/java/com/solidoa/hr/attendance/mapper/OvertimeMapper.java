package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.Overtime;
import com.solidoa.hr.attendance.entity.OvertimeBreak;
import com.solidoa.hr.attendance.vo.OvertimeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 加班Mapper
 */
@Mapper
public interface OvertimeMapper extends BaseMapper<Overtime> {

    /**
     * 分页查询加班列表
     */
    List<OvertimeVO> selectPageList(@Param("offset") int offset,
                                   @Param("limit") int limit,
                                   @Param("userId") Long userId,
                                   @Param("status") String status,
                                   @Param("overtimeType") String overtimeType,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 统计加班记录数
     */
    long selectCount(@Param("userId") Long userId,
                     @Param("status") String status,
                     @Param("overtimeType") String overtimeType,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

    /**
     * 查询用户的调休记录
     */
    List<OvertimeBreak> selectBreakByUserId(@Param("userId") Long userId);

    /**
     * 查询未过期的调休记录
     */
    List<OvertimeBreak> selectValidBreakByUserId(@Param("userId") Long userId,
                                                  @Param("now") LocalDateTime now);

    /**
     * 查询用户的可用调休总时长
     */
    BigDecimal selectAvailableBreakHours(@Param("userId") Long userId,
                                        @Param("now") LocalDateTime now);

    /**
     * 查询用户的已使用调休总时长
     */
    BigDecimal selectUsedBreakHours(@Param("userId") Long userId);
}