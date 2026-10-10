-- V20260804_003: PHP→SaaS 三表同步检查点表 + 同步状态监控权限种子
-- 对应《手机端调用SaaS鉴权方案》v1.3 §3.5.3（sync_checkpoint）/ §3.5.5（监控只读接口）

-- 1. 同步检查点表（幂等）
CREATE TABLE IF NOT EXISTS sync_checkpoint (
  table_name      VARCHAR(64)  NOT NULL COMMENT '同步表名：platform_user / platform_user_token / platform_user_detail',
  last_max_id     BIGINT       NOT NULL DEFAULT 0 COMMENT 'id 水位线（新增快路径）',
  last_run_at     DATETIME     NULL COMMENT '最近执行时间',
  last_success_at DATETIME     NULL COMMENT '最近成功时间',
  fail_count      INT          NOT NULL DEFAULT 0 COMMENT '连续失败次数（成功即清零）',
  last_error      VARCHAR(500) NULL COMMENT '最近一次错误信息',
  stats           VARCHAR(500) NULL COMMENT '最近一周期统计：new=x,updated=y,deleted=z',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PHP→SaaS 三表同步检查点';

-- 2. 同步状态监控权限（挂在"系统管理"目录 4100 下，纯 API 权限不建菜单页）
INSERT IGNORE INTO sys_permission (
  id, tenant_id, menu_id, code, name, action, status,
  created_at, updated_at, created_by, updated_by, deleted
) VALUES
(10001, 1, 4100, 'system:sync:view', '查看同步状态', 'view', 'enabled', NOW(), NOW(), 1, 1, 0);

-- 3. 授予平台管理员（role_id=2000）
INSERT IGNORE INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, created_at, updated_at, created_by, updated_by, deleted
) VALUES
(10001, 1, 2000, 10001, NOW(), NOW(), 1, 1, 0);
