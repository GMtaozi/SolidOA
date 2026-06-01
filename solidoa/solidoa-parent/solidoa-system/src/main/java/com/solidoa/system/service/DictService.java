package com.solidoa.system.service;

import com.solidoa.system.vo.DictVO;
import java.util.List;

public interface DictService {
    List<DictVO> getByType(String type);

    List<String> getTypes();
}