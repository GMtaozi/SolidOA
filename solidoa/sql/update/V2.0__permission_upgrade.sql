-- ============================================================================
-- 第 5 章 系统管理权限升级 (V2.0)
-- 现状: sys_permission 已有 parentId/sort/icon/type/url/method (V1.x 已建)
--       sys_role 缺 data_scope
--       sys_permission 缺 menuId/path/component/hidden
-- 目标: 完整菜单/按钮/API 三态权限模型 + 角色数据权限范围
-- ============================================================================

-- 1. sys_role 增加数据权限范围
ALTER TABLE sys_role
  ADD COLUMN data_scope TINYINT DEFAULT 1
    COMMENT '1全部 2本部门 3本部门及下级 4本人 5自定义' AFTER status;

-- 2. sys_permission 增加菜单/前端路由相关字段
ALTER TABLE sys_permission
  ADD COLUMN menu_id BIGINT COMMENT '关联菜单ID' AFTER parent_id,
  ADD COLUMN path VARCHAR(200) COMMENT '前端路由路径' AFTER url,
  ADD COLUMN component VARCHAR(200) COMMENT '前端组件路径' AFTER path,
  ADD COLUMN hidden TINYINT DEFAULT 0 COMMENT '是否隐藏菜单 0否 1是' AFTER component;

-- 3. sys_permission 新增索引
CREATE INDEX idx_menu_id ON sys_permission (menu_id);
CREATE INDEX idx_parent_sort ON sys_permission (parent_id, sort);

-- 4. 数据回填：给现有 4 个核心 Controller 端点配置按钮权限
-- (供管理员在角色管理里"分配权限"用)
-- 注意: V1.x 的 sys_permission 用 type 字段 (menu/button/api), 无 perm_type
INSERT INTO sys_permission (name, code, type, url, method, parent_id, sort, icon, path, component, hidden, create_time, update_time) VALUES
  ('用户管理-查看',  'USER_VIEW',          'BUTTON', '/api/v1/system/users/{id}', 'GET',    0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('用户管理-列表',  'USER_LIST',          'BUTTON', '/api/v1/system/users',        'GET',    0, 2, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('用户管理-创建',  'USER_CREATE',        'BUTTON', '/api/v1/system/users',        'POST',   0, 3, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('用户管理-更新',  'USER_UPDATE',        'BUTTON', '/api/v1/system/users/{id}', 'PUT',    0, 4, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('用户管理-删除',  'USER_DELETE',        'BUTTON', '/api/v1/system/users/{id}', 'DELETE', 0, 5, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('用户管理-重置密码','USER_RESET_PASSWORD','BUTTON','/api/v1/system/users/{id}/reset-password','POST',0,6,NULL,NULL,NULL,1,NOW(),NOW()),

  ('角色管理-查看',  'ROLE_VIEW',          'BUTTON', '/api/v1/system/roles/{id}', 'GET',    0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('角色管理-创建',  'ROLE_CREATE',        'BUTTON', '/api/v1/system/roles',       'POST',   0, 2, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('角色管理-更新',  'ROLE_UPDATE',        'BUTTON', '/api/v1/system/roles/{id}', 'PUT',    0, 3, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('角色管理-删除',  'ROLE_DELETE',        'BUTTON', '/api/v1/system/roles/{id}', 'DELETE', 0, 4, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('角色管理-分配权限','ROLE_ASSIGN_PERMISSIONS','BUTTON','/api/v1/system/roles/{id}/permissions','PUT',0,5,NULL,NULL,NULL,1,NOW(),NOW()),

  ('部门管理-创建',  'DEPT_CREATE',        'BUTTON', '/api/v1/system/depts',       'POST',   0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('部门管理-更新',  'DEPT_UPDATE',        'BUTTON', '/api/v1/system/depts/{id}', 'PUT',    0, 2, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('部门管理-删除',  'DEPT_DELETE',        'BUTTON', '/api/v1/system/depts/{id}', 'DELETE', 0, 3, NULL, NULL, NULL, 1, NOW(), NOW()),

  ('印章管理-创建',  'STAMP_CREATE',       'BUTTON', '/api/v1/workflow/stamp',      'POST',   0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('印章管理-审批',  'STAMP_APPROVE',      'BUTTON', '/api/v1/workflow/stamp/{id}/approve','POST',0,2,NULL,NULL,NULL,1,NOW(),NOW()),

  ('采购管理-创建',  'PURCHASE_CREATE',    'BUTTON', '/api/v1/workflow/purchase',   'POST',   0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('采购管理-审批',  'PURCHASE_APPROVE',   'BUTTON', '/api/v1/workflow/purchase/{id}/approve','POST',0,2,NULL,NULL,NULL,1,NOW(),NOW()),

  ('报销管理-创建',  'EXPENSE_CREATE',     'BUTTON', '/api/v1/workflow/expense',    'POST',   0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('报销管理-审批',  'EXPENSE_APPROVE',    'BUTTON', '/api/v1/workflow/expense/{id}/approve','POST',0,2,NULL,NULL,NULL,1,NOW(),NOW()),

  ('请假管理-创建',  'LEAVE_CREATE',       'BUTTON', '/api/v1/workflow/leave',      'POST',   0, 1, NULL, NULL, NULL, 1, NOW(), NOW()),
  ('请假管理-审批',  'LEAVE_APPROVE',      'BUTTON', '/api/v1/workflow/leave/{id}/approve','POST',0,2,NULL,NULL,NULL,1,NOW(),NOW());

-- 5. 角色数据权限种子：默认管理员=全部(1), 普通用户=本人(4)
UPDATE sys_role SET data_scope = 1 WHERE code = 'ADMIN';
UPDATE sys_role SET data_scope = 4 WHERE code = 'USER';
