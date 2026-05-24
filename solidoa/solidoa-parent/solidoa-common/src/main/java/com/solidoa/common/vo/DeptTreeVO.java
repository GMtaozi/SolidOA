package com.solidoa.common.vo;

import lombok.Data;

@Data
public class DeptTreeVO {
    private Long id;
    private String name;
    private Long parentId;
    private String leaderName;
    private Integer sort;
}