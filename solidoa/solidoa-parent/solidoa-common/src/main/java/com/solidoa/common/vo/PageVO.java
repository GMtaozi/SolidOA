package com.solidoa.common.vo;

import lombok.Data;
import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
}