package com.solidoa.dingtalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.dingtalk.entity.DingtalkConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DingtalkConfigMapper extends BaseMapper<DingtalkConfig> {
    DingtalkConfig selectActiveConfig();
}
