package com.solidoa.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.file.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<File> {
    List<File> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);

    Page<File> selectByBusinessPage(Page<File> page, @Param("businessType") String businessType, @Param("businessId") Long businessId);

    long countByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);
}