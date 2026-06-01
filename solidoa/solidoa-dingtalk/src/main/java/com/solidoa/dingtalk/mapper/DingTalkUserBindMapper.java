package com.solidoa.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.dingtalk.entity.DingTalkUserBind;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 钉钉用户绑定Mapper
 */
@Mapper
public interface DingTalkUserBindMapper extends BaseMapper<DingTalkUserBind> {

    /**
     * 根据SolidOA用户ID查询绑定
     */
    @Select("SELECT * FROM oa_dingtalk_user_bind WHERE user_id = #{userId} AND is_active = TRUE LIMIT 1")
    DingTalkUserBind selectByUserId(Long userId);

    /**
     * 根据钉钉用户ID查询绑定
     */
    @Select("SELECT * FROM oa_dingtalk_user_bind WHERE dingtalk_user_id = #{dingtalkUserId} AND is_active = TRUE LIMIT 1")
    DingTalkUserBind selectByDingtalkUserId(String dingtalkUserId);

    /**
     * 获取所有活跃绑定用户ID列表
     */
    @Select("SELECT user_id FROM oa_dingtalk_user_bind WHERE is_active = TRUE")
    List<Long> selectAllActiveUserIds();

    /**
     * 根据部门获取用户ID列表
     */
    @Select("SELECT user_id FROM oa_dingtalk_user_bind WHERE department_id = #{departmentId} AND is_active = TRUE")
    List<Long> selectByDepartment(String departmentId);
}