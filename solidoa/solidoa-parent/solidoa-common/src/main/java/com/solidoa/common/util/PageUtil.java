package com.solidoa.common.util;

import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import java.util.List;

/**
 * 分页工具类
 */
public class PageUtil {

    /**
     * 构建分页结果
     */
    public static <T> PageVO<T> buildPage(List<T> records, long total, int pageNum, int pageSize) {
        PageVO<T> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    /**
     * 计算分页偏移量
     */
    public static int calculateOffset(int pageNum, int pageSize) {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 校验并修正分页参数（返回校验后的新对象，避免副作用）
     */
    public static PageDTO validatePageParams(PageDTO pageDTO) {
        if (pageDTO == null) {
            pageDTO = new PageDTO();
        }
        PageDTO result = new PageDTO();
        result.setPageNum(safePageNum(pageDTO.getPageNum(), 1));
        result.setPageSize(safePageSize(pageDTO.getPageSize(), 20));
        return result;
    }

    /**
     * 获取安全的分页参数
     */
    public static int safePageNum(Integer pageNum, int defaultValue) {
        return pageNum == null || pageNum < 1 ? defaultValue : pageNum;
    }

    public static int safePageSize(Integer pageSize, int defaultValue) {
        return pageSize == null || pageSize < 1 ? defaultValue : Math.min(pageSize, 100);
    }
}