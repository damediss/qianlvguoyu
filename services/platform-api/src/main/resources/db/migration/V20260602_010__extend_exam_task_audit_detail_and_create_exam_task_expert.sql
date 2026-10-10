-- ===========================================================================
-- 题库审核指派 三子菜单 (理论/实操/主观) 数据库结构升级
-- 日期: 2026-06-02
--
-- 1. 增强 V20260528_006 已创建的 3 张任务明细表 (exam_task_exam_detail /
--    exam_task_examshicao_detail / exam_task_examsub_detail) 加上方案 §3.1
--    列出的额外字段 (xingwei / kaohe / jianding / level / nanyicd / isblod 等)
-- 2. 新建通用 `exam_task_expert` 任务专家分配表,把现有 `exam_task_ximu_expert`
--    的数据 backfill 过来,统一管理 4 类任务 (ximu/exam/examshicao/examsub) 的
--    专家分配。旧表保留只读备份,新代码只读 `exam_task_expert`。
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1) exam_task_exam_detail 补字段
-- ---------------------------------------------------------------------------
-- 提示:
-- 1) MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS,所有补字段用裸 ADD COLUMN。
-- 2) exam_task_exam_detail 已有 8 个 VARCHAR(2000) 选项列 (exam_a..h) 占 ~64K 行宽,
--    再加 VARCHAR 必爆 65535 上限,所以新加的描述类字段统一用 TEXT (off-row)。
ALTER TABLE exam_task_exam_detail
  ADD COLUMN xingwei       TEXT         NULL COMMENT '行为 (理论题用)' AFTER task_id,
  ADD COLUMN kaohe1        TEXT         NULL COMMENT '考核点1',
  ADD COLUMN kaohe2        TEXT         NULL COMMENT '考核点2',
  ADD COLUMN kaohe3        TEXT         NULL COMMENT '考核点3',
  ADD COLUMN kaohe4        TEXT         NULL COMMENT '考核点4',
  ADD COLUMN jianding      TEXT         NULL COMMENT '鉴定点',
  ADD COLUMN level         INT          NULL COMMENT '级别冗余 1-5',
  ADD COLUMN nanyicd       VARCHAR(20)  NULL COMMENT '难度档 (易/中/难)',
  ADD COLUMN isblod        INT          NOT NULL DEFAULT 0 COMMENT '重点审核 0否 1是';

-- ---------------------------------------------------------------------------
-- 2) exam_task_examshicao_detail 补字段
-- ---------------------------------------------------------------------------
ALTER TABLE exam_task_examshicao_detail
  ADD COLUMN xingwei       TEXT         NULL COMMENT '行为' AFTER task_id,
  ADD COLUMN kaohe1        TEXT         NULL,
  ADD COLUMN kaohe2        TEXT         NULL,
  ADD COLUMN kaohe3        TEXT         NULL,
  ADD COLUMN kaohe4        TEXT         NULL,
  ADD COLUMN nanyicd       VARCHAR(20)  NULL,
  ADD COLUMN isblod        INT          NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 3) exam_task_examsub_detail 补字段
-- ---------------------------------------------------------------------------
ALTER TABLE exam_task_examsub_detail
  ADD COLUMN isJisuan      INT          NOT NULL DEFAULT 0 COMMENT '是否计算题 0否 1是' AFTER answer,
  ADD COLUMN nanyicd       VARCHAR(20)  NULL,
  ADD COLUMN isblod        INT          NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 4) 新建通用 exam_task_expert 表 (替换 ximu_expert)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exam_task_expert (
  id          INT          NOT NULL AUTO_INCREMENT,
  tenant_id   INT          NOT NULL DEFAULT 1 COMMENT '租户ID',
  task_id     INT          NOT NULL COMMENT '任务ID (与 task_type 联合定位)',
  task_type   VARCHAR(20)  NOT NULL DEFAULT 'exam' COMMENT '任务类型 exam/examshicao/examsub/ximu',
  expert_id   INT          NOT NULL COMMENT '专家 sys_user.id',
  task_role   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=初审 1=终审',
  tag         VARCHAR(20)  NOT NULL DEFAULT 'exam' COMMENT '冗余 tag, 与 task_type 保持一致',
  addtime     DATETIME     NULL COMMENT '指派时间',
  endtime     DATETIME     NULL COMMENT '任务完成时间',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by  INT          NULL,
  updated_by  INT          NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exam_task_expert (tenant_id, task_id, task_type, expert_id, task_role, deleted),
  KEY idx_expert (tenant_id, expert_id, task_role, deleted),
  KEY idx_task (tenant_id, task_type, task_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库任务专家分配通用表';

-- ---------------------------------------------------------------------------
-- 5) Backfill: 把 exam_task_ximu_expert 数据复制到 exam_task_expert
--    task_type 固定 'ximu',保持幂等 (用 INSERT IGNORE 避免重复插入)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exam_task_expert
  (tenant_id, task_id, task_type, expert_id, task_role, tag, addtime, endtime,
   created_at, updated_at, created_by, updated_by, deleted)
SELECT
  tenant_id, task_id, 'ximu' AS task_type, expert_id, task_role, tag,
  addtime, endtime,
  NOW(), NOW(), created_by, updated_by, deleted
FROM exam_task_ximu_expert
WHERE deleted = 0;
