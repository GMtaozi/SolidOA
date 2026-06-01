package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.ApprovalNodeDetail;
import com.solidoa.workflow.vo.ApprovalNodeDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 审批节点明细Mapper
 */
@Mapper
public interface ApprovalNodeDetailMapper extends BaseMapper<ApprovalNodeDetail> {

    /**
     * 根据记录ID查询节点列表
     */
    List<ApprovalNodeDetail> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据记录ID查询节点详情VO列表
     */
    List<ApprovalNodeDetailVO> selectVOByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询用户审批过的记录ID列表
     */
    List<Long> selectApprovedRecordIdsByUser(@Param("userId") Long userId);
}