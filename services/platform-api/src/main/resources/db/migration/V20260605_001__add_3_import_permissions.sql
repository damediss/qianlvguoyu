-- ===========================================================================
-- 题库审核指派 补导入权限
-- 日期: 2026-06-05
--
-- 背景: V20260602_012 复刻 /yuntiku/task-exam, /yuntiku/task-examsub, /yuntiku/task-examshicao
--       等 PHP 接口时,漏了导入 (import) 端点。本迁移补 3 个 import 权限 + admin 角色授权。
--
-- 老 PHP 逻辑在 D:\workspace\gjjd_phalcon\app\controllers\api\YuntikuController.php 第 1804 行 uploadAction:
--   type=exam|examshicao|examsub → 创建任务 + 批量插入 detail
-- 老 PHP 前端在 D:\workspace\yun_shang\src\views\tiku\taskExam.vue 第 4 行 + 91-135 行的"导入"按钮。
--
-- Java 已有参考实现: ExamTaskXimuController.java @PostMapping("/task-ximus/import")
--   + ExamTaskXimuApplicationService.importExcel(MultipartFile, workTypeId, level, types)
-- ===========================================================================

-- 1) 3 个新权限
INSERT INTO sys_permission
  (id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (7060, 1, 4710, 'task:exam-audit:import',        '导入理论审核任务', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7061, 1, 4711, 'task:examshicao-audit:import',  '导入实操审核任务', 'import', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7062, 1, 4712, 'task:examsub-audit:import',     '导入主观审核任务', 'import', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 2) admin 角色 (PLATFORM_ADMIN id=2000) 授权
INSERT INTO sys_role_permission
  (id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (910120, 1, 2000, 7060, NOW(), NOW(), 1, 1, 0),
  (910121, 1, 2000, 7061, NOW(), NOW(), 1, 1, 0),
  (910122, 1, 2000, 7062, NOW(), NOW(), 1, 1, 0);
