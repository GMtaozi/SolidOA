package com.solidoa.common.client;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.DeptTreeVO;
import com.solidoa.common.vo.UserSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "system-service", path = "/api/v1/system")
public interface SystemClient {

    @GetMapping("/user/{id}")
    Result<UserSimpleVO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/user/ids")
    Result<List<UserSimpleVO>> getUsersByIds(@RequestParam List<Long> ids);

    @GetMapping("/depts/tree")
    Result<List<DeptTreeVO>> getDeptTree();

    @GetMapping("/dept/{id}/leader")
    Result<UserSimpleVO> getDeptLeader(@PathVariable("id") Long deptId);

    @GetMapping("/user/{id}/permissions")
    Result<List<String>> getUserPermissions(@PathVariable("id") Long id);
}