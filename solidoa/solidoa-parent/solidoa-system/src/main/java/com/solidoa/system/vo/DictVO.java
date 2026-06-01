package com.solidoa.system.vo;

import lombok.Data;

@Data
public class DictVO {
    private Long id;
    private String type;
    private String label;
    private String value;
    private Integer sort;
}