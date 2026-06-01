package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.system.service.DictService;
import com.solidoa.system.vo.DictVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/dicts")
public class DictController {

    @Autowired
    private DictService dictService;

    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        return Result.success(dictService.getTypes());
    }

    @GetMapping
    public Result<List<DictVO>> getByType(@RequestParam(value = "type") String type) {
        return Result.success(dictService.getByType(type));
    }
}