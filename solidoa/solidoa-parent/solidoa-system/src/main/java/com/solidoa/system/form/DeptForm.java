package com.solidoa.system.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptForm {
    private Long id;
    @NotBlank(message = "部门名称不能为空")
    private String name;
    private Long parentId;
    private Long leaderId;
    private Integer sort;
}