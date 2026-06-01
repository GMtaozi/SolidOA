package com.solidoa.system.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.entity.Department;
import com.solidoa.system.form.DeptForm;
import com.solidoa.system.mapper.DepartmentMapper;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.service.DepartmentService;
import com.solidoa.system.vo.DeptTreeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private static final int MAX_TREE_DEPTH = 20;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<DeptTreeVO> getTreeList() {
        List<Department> allDepts = departmentMapper.selectTreeList();
        List<DeptTreeVO> tree = new ArrayList<>();

        Map<Long, List<Department>> childrenMap = allDepts.stream()
            .filter(d -> d.getParentId() != null && d.getParentId() > 0)
            .collect(Collectors.groupingBy(Department::getParentId));

        for (Department dept : allDepts) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                DeptTreeVO vo = convertToTreeVO(dept);
                vo.setChildren(buildChildren(dept.getId(), childrenMap, 1));
                tree.add(vo);
            }
        }

        return tree;
    }

    private List<DeptTreeVO> buildChildren(Long parentId, Map<Long, List<Department>> childrenMap, int depth) {
        List<DeptTreeVO> children = new ArrayList<>();
        List<Department> depts = childrenMap.get(parentId);
        if (depts != null && depth < MAX_TREE_DEPTH) {
            for (Department dept : depts) {
                DeptTreeVO vo = convertToTreeVO(dept);
                vo.setChildren(buildChildren(dept.getId(), childrenMap, depth + 1));
                children.add(vo);
            }
        }
        return children;
    }

    private DeptTreeVO convertToTreeVO(Department dept) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setSort(dept.getSort());
        return vo;
    }

    @Override
    public DeptTreeVO getById(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return convertToTreeVO(dept);
    }

    @Override
    @Transactional
    public Long create(DeptForm form) {
        Department dept = new Department();
        BeanUtils.copyProperties(form, dept);
        departmentMapper.insert(dept);
        log.info("创建部门: {}", dept.getName());
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, DeptForm form) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        BeanUtils.copyProperties(form, dept);
        departmentMapper.updateById(dept);
        log.info("更新部门: {}", dept.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        List<Long> childIds = departmentMapper.selectChildIds(id);
        if (!childIds.isEmpty()) {
            throw new BusinessException("存在子部门，无法删除");
        }

        // 检查部门下是否有用户
        List<Long> userIds = userMapper.selectUserIdsByDeptIds(List.of(id));
        if (userIds != null && !userIds.isEmpty()) {
            throw new BusinessException("部门下存在用户，无法删除");
        }

        departmentMapper.deleteById(id);
        log.info("删除部门: {}", id);
    }
}