package com.solidoa.system.service;

import com.solidoa.system.form.DeptForm;
import com.solidoa.system.vo.DeptTreeVO;
import java.util.List;

public interface DepartmentService {
    List<DeptTreeVO> getTreeList();

    DeptTreeVO getById(Long id);

    Long create(DeptForm form);

    void update(Long id, DeptForm form);

    void delete(Long id);
}