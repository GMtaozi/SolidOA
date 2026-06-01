package com.solidoa.system.service.impl;

import com.solidoa.system.entity.Dict;
import com.solidoa.system.mapper.DictMapper;
import com.solidoa.system.service.DictService;
import com.solidoa.system.vo.DictVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictServiceImpl implements DictService {

    @Autowired
    private DictMapper dictMapper;

    @Override
    public List<DictVO> getByType(String type) {
        List<Dict> dicts = dictMapper.selectByType(type);
        return dicts.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getTypes() {
        return dictMapper.selectDistinctTypes();
    }

    private DictVO convertToVO(Dict dict) {
        DictVO vo = new DictVO();
        vo.setId(dict.getId());
        vo.setType(dict.getType());
        vo.setLabel(dict.getLabel());
        vo.setValue(dict.getValue());
        vo.setSort(dict.getSort());
        return vo;
    }
}