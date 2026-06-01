package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DictMapper extends BaseMapper<Dict> {
    List<Dict> selectByType(@Param("type") String type);

    List<String> selectDistinctTypes();
}