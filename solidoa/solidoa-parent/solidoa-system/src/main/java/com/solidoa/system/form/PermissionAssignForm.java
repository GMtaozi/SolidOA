package com.solidoa.system.form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PermissionAssignForm {
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
