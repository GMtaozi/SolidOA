package com.solidoa.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.dingtalk.entity.OvertimeExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 加班记录扩展Mapper
 */
@Mapper
public interface OvertimeExtMapper extends BaseMapper<OvertimeExt> {

    /**
     * 根据钉钉加班记录ID查询
     */
    @Select("SELECT * FROM oa_overtime_ext WHERE dingtalk_overtime_id = #{dingtalkOvertimeId} LIMIT 1")
    OvertimeExt selectByDingtalkOvertimeId(String dingtalkOvertimeId);
}