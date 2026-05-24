package com.solidoa.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.attendance.entity.RepairCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RepairCardMapper extends BaseMapper<RepairCard> {
    List<RepairCard> selectPendingList(@Param("offset") int offset, @Param("limit") int limit);
}