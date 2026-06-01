package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.workflow.entity.ApprovalNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 审批节点Mapper
 */
@Mapper
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {

    /**
     * 查询业务的审批节点列表
     */
    @Select("SELECT * FROM oa_approval_node WHERE business_type = #{businessType} AND business_id = #{businessId} ORDER BY node_order")
    List<ApprovalNode> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 查询当前待审批的节点
     */
    @Select("SELECT * FROM oa_approval_node WHERE business_type = #{businessType} AND business_id = #{businessId} AND status = 'PENDING' ORDER BY node_order LIMIT 1")
    ApprovalNode selectCurrentNode(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    /**
     * 查询待某用户审批的节点数量
     */
    @Select("SELECT COUNT(*) FROM oa_approval_node WHERE approver_id = #{userId} AND status = 'PENDING'")
    int countPendingByApprover(@Param("userId") Long userId);

    /**
     * 查询某审批节点的会签待审批数量
     */
    @Select("SELECT COUNT(*) FROM oa_approval_node WHERE business_type = #{businessType} AND business_id = #{businessId} AND node_order = #{nodeOrder} AND status = 'PENDING'")
    int countPendingInNode(@Param("businessType") String businessType, @Param("businessId") Long businessId, @Param("nodeOrder") Integer nodeOrder);
}
