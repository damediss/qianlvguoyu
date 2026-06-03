-- ===========================================================================
-- 题库审核指派 三子菜单 - 菜单 / 权限 / 角色授权
-- 日期: 2026-06-02
--
-- 6 个新菜单:
--   管理端 (parent=4705 开发任务):
--     4710 题库理论审核指派
--     4711 题库实操审核指派
--     4712 题库主观审核指派
--   专家端 (parent=4707 审核任务):
--     4713 理论题审核
--     4714 实操题审核
--     4715 主观题审核
--
-- 18 个新权限 (菜单 4710-4715):
--   管理端 4 权限/菜单 × 3 = 12: view / assign-expert / delete / export
--   专家端 2 权限/菜单 × 3 =  6: view / submit
--
-- 角色授权:
--   PLATFORM_ADMIN (2000) 拿到管理端 3 菜单 + 12 权限
--   EXPERT (2004)        拿到专家端 3 菜单 + 6 权限
--
-- !!! 重要:sys_permission / sys_role_permission 的 ID 段必须远离 5400-5451,
-- !!! 那段早被其他同学(培训模块)用 train:* 占了。我们的 18 个权限用 7000-7017。
-- !!! sys_role_menu 的 ID 段 604930-604935 也没撞上,但保险起见用 604940-604945。
-- !!! sys_role_permission 用 910000-910011(admin) / 910100-910105(expert)。
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1) 菜单
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_menu (
  id, tenant_id, parent_id, menu_type, code, name, path, component, permission_code,
  icon, sort_order, visible, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  -- 管理端 (parent=4705)
  (4710, 1, 4705, 'page', 'task_exam_audit_assign',  '题库理论审核指派',
    '/task-management/dev-task/task-exam',  'exam/TaskExamAuditAssignList',
    'task:exam-audit:view',  'FileTextOutlined',  2, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4711, 1, 4705, 'page', 'task_examshicao_audit_assign', '题库实操审核指派',
    '/task-management/dev-task/task-examshicao', 'exam/TaskExamshicaoAuditAssignList',
    'task:examshicao-audit:view', 'ExperimentOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4712, 1, 4705, 'page', 'task_examsub_audit_assign',  '题库主观审核指派',
    '/task-management/dev-task/task-examsub',  'exam/TaskExamsubAuditAssignList',
    'task:examsub-audit:view',   'EditOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 专家端 (parent=4707)
  (4713, 1, 4707, 'page', 'expert_task_exam',  '理论题审核',
    '/task-management/audit-task/exam',  'expert/ExpertTaskExam',
    'expert:task-exam:view',     'FileTextOutlined', 3, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4714, 1, 4707, 'page', 'expert_task_examshicao', '实操题审核',
    '/task-management/audit-task/examshicao', 'expert/ExpertTaskExamshicao',
    'expert:task-examshicao:view', 'ExperimentOutlined', 4, 1, 'enabled', NOW(), NOW(), 1, 1, 0),
  (4715, 1, 4707, 'page', 'expert_task_examsub',  '主观题审核',
    '/task-management/audit-task/examsub', 'expert/ExpertTaskExamsub',
    'expert:task-examsub:view',   'EditOutlined', 5, 1, 'enabled', NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 2) 权限 (新 ID 段 7000-7017,避开 5400-5451 撞车)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  -- 管理端 4710 (理论) - 4 权限
  (7000, 1, 4710, 'task:exam-audit:view',          '查看理论审核指派', 'view',          'enabled', NOW(), NOW(), 1, 1, 0),
  (7001, 1, 4710, 'task:exam-audit:assign-expert', '指派理论任务专家', 'assign',        'enabled', NOW(), NOW(), 1, 1, 0),
  (7002, 1, 4710, 'task:exam-audit:delete',        '删除理论任务',     'delete',        'enabled', NOW(), NOW(), 1, 1, 0),
  (7003, 1, 4710, 'task:exam-audit:export',        '导出理论任务',     'export',        'enabled', NOW(), NOW(), 1, 1, 0),
  -- 管理端 4711 (实操)
  (7004, 1, 4711, 'task:examshicao-audit:view',          '查看实操审核指派', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (7005, 1, 4711, 'task:examshicao-audit:assign-expert', '指派实操任务专家', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7006, 1, 4711, 'task:examshicao-audit:delete',        '删除实操任务',     'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7007, 1, 4711, 'task:examshicao-audit:export',        '导出实操任务',     'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 管理端 4712 (主观)
  (7008, 1, 4712, 'task:examsub-audit:view',          '查看主观审核指派', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (7009, 1, 4712, 'task:examsub-audit:assign-expert', '指派主观任务专家', 'assign', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7010, 1, 4712, 'task:examsub-audit:delete',        '删除主观任务',     'delete', 'enabled', NOW(), NOW(), 1, 1, 0),
  (7011, 1, 4712, 'task:examsub-audit:export',        '导出主观任务',     'export', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 专家端 4713 (理论) - 2 权限
  (7012, 1, 4713, 'expert:task-exam:view',    '查看我的理论任务', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (7013, 1, 4713, 'expert:task-exam:submit',  '提交理论审核',     'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 专家端 4714 (实操)
  (7014, 1, 4714, 'expert:task-examshicao:view',   '查看我的实操任务', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (7015, 1, 4714, 'expert:task-examshicao:submit', '提交实操审核',     'submit', 'enabled', NOW(), NOW(), 1, 1, 0),
  -- 专家端 4715 (主观)
  (7016, 1, 4715, 'expert:task-examsub:view',   '查看我的主观任务', 'view',   'enabled', NOW(), NOW(), 1, 1, 0),
  (7017, 1, 4715, 'expert:task-examsub:submit', '提交主观审核',     'submit', 'enabled', NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 3) PLATFORM_ADMIN (2000) 拿到管理端 3 菜单 + 12 权限
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604940, 1, 2000, 4710, NOW(), NOW(), 1, 1, 0),
  (604941, 1, 2000, 4711, NOW(), NOW(), 1, 1, 0),
  (604942, 1, 2000, 4712, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (910000, 1, 2000, 7000, NOW(), NOW(), 1, 1, 0),
  (910001, 1, 2000, 7001, NOW(), NOW(), 1, 1, 0),
  (910002, 1, 2000, 7002, NOW(), NOW(), 1, 1, 0),
  (910003, 1, 2000, 7003, NOW(), NOW(), 1, 1, 0),
  (910004, 1, 2000, 7004, NOW(), NOW(), 1, 1, 0),
  (910005, 1, 2000, 7005, NOW(), NOW(), 1, 1, 0),
  (910006, 1, 2000, 7006, NOW(), NOW(), 1, 1, 0),
  (910007, 1, 2000, 7007, NOW(), NOW(), 1, 1, 0),
  (910008, 1, 2000, 7008, NOW(), NOW(), 1, 1, 0),
  (910009, 1, 2000, 7009, NOW(), NOW(), 1, 1, 0),
  (910010, 1, 2000, 7010, NOW(), NOW(), 1, 1, 0),
  (910011, 1, 2000, 7011, NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 4) EXPERT (2004) 拿到专家端 3 菜单 + 6 权限
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (
  id, tenant_id, role_id, menu_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (604943, 1, 2004, 4713, NOW(), NOW(), 1, 1, 0),
  (604944, 1, 2004, 4714, NOW(), NOW(), 1, 1, 0),
  (604945, 1, 2004, 4715, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
  (910100, 1, 2004, 7012, NOW(), NOW(), 1, 1, 0),
  (910101, 1, 2004, 7013, NOW(), NOW(), 1, 1, 0),
  (910102, 1, 2004, 7014, NOW(), NOW(), 1, 1, 0),
  (910103, 1, 2004, 7015, NOW(), NOW(), 1, 1, 0),
  (910104, 1, 2004, 7016, NOW(), NOW(), 1, 1, 0),
  (910105, 1, 2004, 7017, NOW(), NOW(), 1, 1, 0);
