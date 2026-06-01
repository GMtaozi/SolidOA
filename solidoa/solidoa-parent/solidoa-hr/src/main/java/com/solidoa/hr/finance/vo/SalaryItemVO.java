package com.solidoa.hr.finance.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalaryItemVO {
    private Long id;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String itemTypeName;
    private String category;
    private String categoryName;
    private BigDecimal defaultValue;
    private Integer isTaxable;
    private Integer sort;
    private Integer status;
}