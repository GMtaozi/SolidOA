package com.solidoa.system.vo;

import lombok.Data;
import java.util.List;

@Data
public class DeptTreeVO {
    private Long id;
    private String name;
    private Long parentId;
    private String leaderName;
    private Integer sort;
    private List<DeptTreeVO> children;
}