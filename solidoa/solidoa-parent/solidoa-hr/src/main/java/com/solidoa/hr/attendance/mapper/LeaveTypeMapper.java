package com.solidoa.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.hr.attendance.entity.LeaveType;
import com.solidoa.hr.attendance.vo.LeaveTypeVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 假期类型Mapper
 */
@Mapper
public interface LeaveTypeMapper extends BaseMapper<LeaveType> {

    /**
     * 查询所有启用的假期类型
     */
    List<LeaveTypeVO> selectActiveList();
}