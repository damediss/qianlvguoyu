-- ===========================================================================
-- 题库审核指派 cleanup: 砍掉 3 个冗余的 expert 端菜单和权限
-- 日期: 2026-06-04
--
-- 原因: V20260602_012 加的 3 个菜单(理论题审核/实操题审核/主观题审核) +
--       6 个权限,功能跟 ExpertAuditController 的 start-tasks/end-tasks
--       完全重叠(后者只缺一个 taskType 过滤参数)。
--       PHP 老系统也压根没有这 3 个菜单,1:1 复刻不需要它们。
--
-- 备份: 21 行原始数据已经导出到 D:\tmp\cleanup_backup.sql
--       (3 sys_menu + 6 sys_permission + 6 sys_role_menu + 6 sys_role_permission)
-- ===========================================================================

-- 1) 角色授权(先删,避免外键约束)
DELETE FROM sys_role_menu
WHERE menu_id IN (4713, 4714, 4715) AND deleted = 0;

DELETE FROM sys_role_permission
WHERE permission_id IN (5430, 5431, 5440, 5441, 5450, 5451) AND deleted = 0;

-- 2) 权限本身
DELETE FROM sys_permission
WHERE id IN (5430, 5431, 5440, 5441, 5450, 5451);

-- 3) 菜单
DELETE FROM sys_menu
WHERE id IN (4713, 4714, 4715);
