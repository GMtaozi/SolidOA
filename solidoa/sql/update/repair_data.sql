修复数据 SQL

  1. 修复部门数据（oa_system 库）

  docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_system << 'EOF'
  SET NAMES utf8mb4;

  -- 清除乱码数据
  DELETE FROM sys_department;

  -- 插入正确的部门数据
  INSERT INTO sys_department (id, name, parent_id, sort, deleted, create_time) VALUES
  (1, '江苏德信基因检测科技有限公司', 0, 1, 0, NOW()),
  (2, '总经办', 1, 1, 0, NOW()),
  (3, '人力资源部', 1, 2, 0, NOW()),
  (4, '财务部', 1, 3, 0, NOW()),
  (5, '技术部', 1, 4, 0, NOW()),
  (6, '市场部', 1, 5, 0, NOW()),
  (7, '销售部', 1, 6, 0, NOW()),
  (8, '行政部', 1, 7, 0, NOW());

  SELECT id, name FROM sys_department;
  EOF

  2. 修复用户数据（oa_system 库）

  docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_system << 'EOF'
  SET NAMES utf8mb4;

  -- 修复用户姓名
  UPDATE sys_user SET real_name = '管理员' WHERE id = 1;
  UPDATE sys_user SET real_name = '丁鑫' WHERE id = 2;
  UPDATE sys_user SET real_name = '张三' WHERE id = 4;

  SELECT id, username, real_name FROM sys_user;
  EOF

  3. 修复审批流程配置（oa_workflow 库）

  docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_workflow << 'EOF'
  SET NAMES utf8mb4;

  -- 清除乱码数据
  DELETE FROM oa_approval_flow_config;

  -- 插入正确的流程配置
  INSERT INTO oa_approval_flow_config (id, business_type, flow_name, is_default, config, create_time) VALUES
  (1, 'LEAVE', '请假审批流程', 1,
  '{"nodes":[{"order":1,"name":"直属领导审批","approverType":"DIRECT_MANAGER","mode":"ANY"}]}', NOW()),
  (2, 'EXPENSE', '报销审批流程', 1, '{"nodes":[{"order":1,"name":"直属领导审批","approverType":"DIRECT_MANAGER","mode":"
  ANY"},{"order":2,"name":"财务审核","approverType":"ROLE","roleCode":"FINANCE","mode":"ANY"}]}', NOW()),
  (3, 'PURCHASE', '采购审批流程', 1, '{"nodes":[{"order":1,"name":"部门主管审批","approverType":"DIRECT_MANAGER","mode":
  "ANY"},{"order":2,"name":"行政审核","approverType":"ROLE","roleCode":"ADMIN","mode":"ANY"}]}', NOW()),
  (4, 'STAMP', '用印审批流程', 1, '{"nodes":[{"order":1,"name":"部门主管审批","approverType":"DIRECT_MANAGER","mode":"AN
  Y"},{"order":2,"name":"行政审核","approverType":"ROLE","roleCode":"ADMIN","mode":"ANY"}]}', NOW()),
  (5, 'OVERTIME', '加班审批流程', 1,
  '{"nodes":[{"order":1,"name":"直属领导审批","approverType":"DIRECT_MANAGER","mode":"ANY"}]}', NOW()),
  (6, 'BUSINESS_TRIP', '出差审批流程', 1,
  '{"nodes":[{"order":1,"name":"直属领导审批","approverType":"DIRECT_MANAGER","mode":"ANY"}]}', NOW());

  SELECT id, business_type, flow_name FROM oa_approval_flow_config;
  EOF

  4. 修复角色数据（如果也有乱码）

  docker exec -i solidoa-mysql mysql -uroot -p749958714 --default-character-set=utf8mb4 oa_system << 'EOF'
  SET NAMES utf8mb4;

  -- 检查角色数据
  SELECT id, name, code FROM sys_role;
  EOF

  执行顺序：先执行 1、2、3，检查 4 是否需要修复。