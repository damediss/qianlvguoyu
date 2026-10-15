-- AI 同步学方法论优化一期（W2）：中断清洗开关 + 中断日志表
-- 方案 docs/04_requirements/AI同步学方法论优化实施方案.md §5.1 / §6.0
--
-- 取号：2026-10-15 已查两库 MAX(version) = 测试库 20261014.003 / 生产库 20261014.003，
--       共用目录接 V20261014_003 之后取 V20261014_004（撞号会导致 jar 全环境无法启动）。

-- 1) 中断日志表（每次触发中断写一行；W3 D 案 7 天窗口与 W5 回归率指标的数据源）
CREATE TABLE IF NOT EXISTS ai_review_interrupt_log (
  id                 BIGINT      NOT NULL COMMENT '雪花 ID',
  plan_id            BIGINT      NOT NULL COMMENT '计划 id',
  platform_user_id   BIGINT      NOT NULL COMMENT '学员',
  interrupt_case     VARCHAR(2)  NOT NULL COMMENT 'A/B/C/D',
  gap_days           INT         NOT NULL COMMENT '本次间隔天数',
  stage_at_interrupt TINYINT     NOT NULL COMMENT '中断时阶段',
  tenant_id          BIGINT      NOT NULL,
  deleted            TINYINT     NOT NULL DEFAULT 0,
  created_by         BIGINT      NULL,
  updated_by         BIGINT      NULL,
  created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_review_interrupt_log_plan (plan_id),
  KEY idx_ai_review_interrupt_log_user (platform_user_id),
  KEY idx_ai_review_interrupt_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习中断日志';

-- 2) 中断清洗开关（默认 0=关：中断只重排+策略文案，不清零连对/绿牌不回黄）
INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20261015001, 10159, 'ai_review_interrupt_clean_enabled', 'AI复习：中断清洗开关(1=清洗连对/绿牌回黄)', '0', 'enabled', NOW(), NOW(), 1, 1, 0);
