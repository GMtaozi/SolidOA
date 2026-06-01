package com.solidoa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.workflow.entity.ApprovalCc;
import com.solidoa.workflow.vo.ApprovalCcVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 抄送记录Mapper
 */
@Mapper
public interface ApprovalCcMapper extends BaseMapper<ApprovalCc> {

    /**
     * 根据记录ID查询抄送列表
     */
    List<ApprovalCc> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据记录ID查询抄送VO列表
     */
    List<ApprovalCcVO> selectVOByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据业务类型和ID查询抄送列表
     */
    List<ApprovalCc> selectByBusiness(@Param("businessType") String businessType,
                                      @Param("businessId") Long businessId);

    /**
     * 查询我的抄送列表
     */
    List<ApprovalCcVO> selectMyCc(@Param("userId") Long userId, @Param("isRead") Boolean isRead);

    /**
     * 分页查询我的抄送列表
     */
    List<ApprovalCcVO> selectMyCcPage(@Param("userId") Long userId, @Param("isRead") Boolean isRead,
                                       @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询我的未读抄送数量
     */
    int countUnreadByUser(@Param("userId") Long userId);

    /**
     * 查询我的抄送总数
     */
    int countMyCc(@Param("userId") Long userId, @Param("isRead") Boolean isRead);
}