-- 移除「学员管理」占位菜单 sup_student_mgmt 及其权限/角色关联

DELETE FROM sys_role_permission
WHERE tenant_id = 1
  AND permission_id IN (
    SELECT id FROM (
      SELECT id FROM sys_permission
      WHERE tenant_id = 1 AND code = 'support.student_mgmt:view'
    ) t
  );

DELETE FROM sys_role_menu
WHERE tenant_id = 1
  AND menu_id IN (
    SELECT id FROM (
      SELECT id FROM sys_menu
      WHERE tenant_id = 1 AND code = 'sup_student_mgmt'
    ) t
  );

DELETE FROM sys_permission
WHERE tenant_id = 1
  AND code = 'support.student_mgmt:view';

DELETE FROM sys_menu
WHERE tenant_id = 1
  AND code = 'sup_student_mgmt';
