package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 考勤组实体
 */
@Data
@TableName("oa_attendance_group")
public class AttendanceGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 考勤组名称
     */
    private String groupName;

    /**
     * 班次ID
     */
    private Long shiftId;

    /**
     * 适用部门(JSON数组)
     * 使用JSON存储多对多关系，避免引入中间表，适用于关联数据变更频率低、查询以整体加载为主的场景
     * 格式: [1,2,3]
     */
    private String applicableDepts;

    /**
     * 适用用户(JSON数组)
     * 使用JSON存储多对多关系，避免引入中间表，适用于关联数据变更频率低、查询以整体加载为主的场景
     * 格式: [1001,1002,1003]
     */
    private String applicableUsers;

    /**
     * 考勤地点(JSON数组)
     * 使用JSON存储一对多关系，地点信息为简单经纬度+名称，无需独立建表
     * 格式: [{"name":"公司","lng":116.4,"lat":39.9}]
     */
    private String checkLocation;

    /**
     * 考勤范围(米)
     */
    private Integer checkRange;

    /**
     * 允许远程打卡
     */
    private Boolean allowRemoteCheck;

    /**
     * 状态:0禁用,1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}