-- V20260804_006: AI 同步学（学员复习）一期建表 + 参数字典 + 模型配置种子
-- 对应《AI同步学（学员复习）实现方案》v1.2 §4（数据库设计）/ §4.6（sys_dict 参数）/ §6（模型配置）
-- 全部新表含完整 BaseEntity 列：tenant_id / deleted / created_by / updated_by / created_at / updated_at

-- ============================================================
-- 4.1 AI 复习计划主表（学员 × 考试计划 唯一）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_review_plan (
  id                BIGINT      NOT NULL COMMENT '雪花 ID',
  platform_user_id  BIGINT      NOT NULL COMMENT '学员（PHP platform_user.id）',
  exam_id           BIGINT      NOT NULL COMMENT '培训考试计划 train_exam_list.id',
  work_type_id      BIGINT      NULL COMMENT '工种（题目范围快照）',
  level             VARCHAR(32) NULL COMMENT '等级快照',
  course_id         BIGINT      NULL COMMENT '科目快照',
  source            INT         NULL COMMENT '题库来源快照（train_exam_list.source）',
  weight_snapshot   JSON        NULL COMMENT '权重快照（§5.7：计划生成时固化，[{knowledgeId,title,rate}]）',
  weight_source     VARCHAR(16) NULL COMMENT '权重来源：standard(type=3)/custom(type=0)/count(题量兜底)',
  exam_date         DATE        NULL COMMENT '考试日期（最近未来日）',
  start_date        DATE        NOT NULL COMMENT '复习开始日（计划生成日）',
  total_days        INT         NOT NULL COMMENT '总天数',
  stage1_days       INT         NOT NULL COMMENT '阶段1天数（50%）',
  stage2_days       INT         NOT NULL COMMENT '阶段2天数（30%）',
  stage3_days       INT         NOT NULL COMMENT '阶段3天数（20%，尾差归此）',
  total_questions   INT         NOT NULL DEFAULT 0 COMMENT '题目全集题数',
  target_questions  INT         NOT NULL DEFAULT 0 COMMENT '冗余裁剪后必做题数',
  must_do_ids       JSON        NULL COMMENT '必做池题 id 列表（排题的输入快照，裁剪/中断删尾部后同步更新）',
  dropped_question_ids JSON     NULL COMMENT '被裁剪/中断补救放弃的题 id（扩展题，不进任何每日任务）',
  trimmed           TINYINT     NOT NULL DEFAULT 0 COMMENT '是否触发裁剪',
  daily_new_cap     INT         NOT NULL DEFAULT 120 COMMENT '每日新题上限快照',
  daily_total_cap   INT         NOT NULL DEFAULT 200 COMMENT '每日总量上限快照',
  current_stage     TINYINT     NOT NULL DEFAULT 1 COMMENT '当前阶段 1/2/3',
  current_day_index INT         NOT NULL DEFAULT 1 COMMENT '当前第几天（从 1 起）',
  last_active_date  DATE        NULL COMMENT '最后活跃日（有作答即更新）',
  interrupt_count   INT         NOT NULL DEFAULT 0 COMMENT '中断次数（零星中断判定用）',
  status            VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/completed/abandoned',
  tenant_id         BIGINT      NOT NULL,
  deleted           TINYINT     NOT NULL DEFAULT 0,
  created_by        BIGINT      NULL,
  updated_by        BIGINT      NULL,
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_review_plan_user_exam (platform_user_id, exam_id),
  KEY idx_ai_review_plan_status (status, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习计划主表';

-- ============================================================
-- 4.2 每日任务
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_review_plan_day (
  id                  BIGINT      NOT NULL COMMENT '雪花 ID',
  plan_id             BIGINT      NOT NULL COMMENT '计划 id',
  day_index           INT         NOT NULL COMMENT '第几天',
  plan_date           DATE        NOT NULL COMMENT '计划日期',
  stage               TINYINT     NOT NULL COMMENT '1/2/3',
  new_question_ids    JSON        NULL COMMENT '今日新题 id 有序列表',
  review_question_ids JSON        NULL COMMENT '今日复盘题 id 有序列表（红牌置顶、加星优先）',
  mock_paper_id       BIGINT      NULL COMMENT '阶段3 模考卷 id',
  new_target          INT         NOT NULL DEFAULT 0 COMMENT '新题目标量',
  review_target       INT         NOT NULL DEFAULT 0 COMMENT '复盘目标量',
  new_done            INT         NOT NULL DEFAULT 0 COMMENT '新题完成数',
  review_done         INT         NOT NULL DEFAULT 0 COMMENT '复盘完成数',
  correct_count       INT         NOT NULL DEFAULT 0 COMMENT '当日答对数',
  answer_count        INT         NOT NULL DEFAULT 0 COMMENT '当日作答数',
  status              VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/in_progress/done/skipped/abandoned',
  strategy_note       VARCHAR(500) NULL COMMENT '当日策略说明（中断补救文案等，展示给学员）',
  tenant_id           BIGINT      NOT NULL,
  deleted             TINYINT     NOT NULL DEFAULT 0,
  created_by          BIGINT      NULL,
  updated_by          BIGINT      NULL,
  created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_review_day_plan_day (plan_id, day_index),
  KEY idx_ai_review_day_date (plan_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习每日任务';

-- ============================================================
-- 4.3 作答记录（主记录，异步回写 PHP exam_result）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_review_answer (
  id               BIGINT      NOT NULL COMMENT '雪花 ID',
  plan_id          BIGINT      NOT NULL COMMENT '计划 id',
  day_id           BIGINT      NULL COMMENT '每日任务 id（模考答完可空）',
  platform_user_id BIGINT      NOT NULL COMMENT '学员',
  exam_plan_id     BIGINT      NULL COMMENT '冗余：train_exam_list.id（回写 PHP exam_result.exam_plan_id 用）',
  question_id      BIGINT      NOT NULL COMMENT 'exam_exam_detail.id',
  question_score   DECIMAL(10,1) NULL COMMENT '冗余：题目分值（回写 get_score 用，对=题分/错=0）',
  knowledge_id     BIGINT      NULL COMMENT '冗余便于统计',
  question_type    VARCHAR(16) NULL COMMENT 'single/multi/judge/...',
  round_type       VARCHAR(8)  NOT NULL COMMENT 'new/review/mock',
  user_answer      TEXT        NULL COMMENT '作答内容（单/判：选项内容；多选：内容数组 JSON）',
  is_correct       TINYINT     NOT NULL DEFAULT 0 COMMENT '0/1',
  use_time_sec     INT         NOT NULL DEFAULT 0 COMMENT '用时（秒）',
  attempt_seq      INT         NOT NULL DEFAULT 1 COMMENT '该题第几次作答',
  php_synced       TINYINT     NOT NULL DEFAULT 0 COMMENT '回写 PHP exam_result：0 未回写 1 已回写 2 失败待补偿',
  php_sync_time    DATETIME    NULL COMMENT '回写成功时间',
  tenant_id        BIGINT      NOT NULL,
  deleted          TINYINT     NOT NULL DEFAULT 0,
  created_by       BIGINT      NULL,
  updated_by       BIGINT      NULL,
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_review_answer_once (plan_id, day_id, question_id, attempt_seq),
  KEY idx_ai_review_answer_sync (php_synced, id),
  KEY idx_ai_review_answer_user (platform_user_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习作答记录';

-- ============================================================
-- 4.4 错题本（三级淘汰）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_review_wrong_item (
  id                  BIGINT      NOT NULL COMMENT '雪花 ID',
  plan_id             BIGINT      NOT NULL COMMENT '计划 id',
  platform_user_id    BIGINT      NOT NULL COMMENT '学员',
  question_id         BIGINT      NOT NULL COMMENT 'exam_exam_detail.id',
  knowledge_id        BIGINT      NULL COMMENT '知识点（L3）',
  card_level          VARCHAR(8)  NOT NULL DEFAULT 'red' COMMENT 'red/yellow/green',
  star                TINYINT     NOT NULL DEFAULT 0 COMMENT '二次错误加星（顽固盲区）',
  consecutive_correct INT         NOT NULL DEFAULT 0 COMMENT '连续答对计数',
  last_correct_date   DATE        NULL COMMENT '最近一次答对日期（黄牌间隔>3天判定）',
  wrong_count         INT         NOT NULL DEFAULT 0 COMMENT '累计答错',
  correct_count       INT         NOT NULL DEFAULT 0 COMMENT '累计答对',
  first_wrong_time    DATETIME    NULL COMMENT '首次答错时间',
  last_wrong_time     DATETIME    NULL COMMENT '最近答错时间',
  removed             TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已移出（绿牌=1）',
  remove_time         DATETIME    NULL COMMENT '移出时间',
  tenant_id           BIGINT      NOT NULL,
  deleted             TINYINT     NOT NULL DEFAULT 0,
  created_by          BIGINT      NULL,
  updated_by          BIGINT      NULL,
  created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_review_wrong_plan_q (plan_id, question_id),
  KEY idx_ai_review_wrong_level (plan_id, card_level, removed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习错题本（三级淘汰）';

-- ============================================================
-- 4.5 自组模考卷 + 卷题明细
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_review_mock_paper (
  id             BIGINT      NOT NULL COMMENT '雪花 ID',
  plan_id        BIGINT      NOT NULL COMMENT '计划 id',
  paper_no       INT         NOT NULL COMMENT '第几套',
  question_count INT         NOT NULL DEFAULT 0 COMMENT '题量',
  total_score    DECIMAL(10,1) NOT NULL DEFAULT 0 COMMENT '总分',
  pass_score     DECIMAL(10,1) NOT NULL DEFAULT 0 COMMENT '及格分（total×0.6）',
  time_limit_min INT         NOT NULL DEFAULT 60 COMMENT '时长（分钟）',
  status         VARCHAR(16) NOT NULL DEFAULT 'ready' COMMENT 'ready/done',
  score          DECIMAL(10,1) NULL COMMENT '得分',
  correct_count  INT         NULL COMMENT '答对数',
  tenant_id      BIGINT      NOT NULL,
  deleted        TINYINT     NOT NULL DEFAULT 0,
  created_by     BIGINT      NULL,
  updated_by     BIGINT      NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_review_mock_plan (plan_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习自组模考卷';

CREATE TABLE IF NOT EXISTS ai_review_mock_paper_item (
  id            BIGINT      NOT NULL COMMENT '雪花 ID',
  paper_id      BIGINT      NOT NULL COMMENT '模考卷 id',
  question_id   BIGINT      NOT NULL COMMENT 'exam_exam_detail.id',
  question_type VARCHAR(16) NULL,
  sort_number   INT         NOT NULL DEFAULT 1 COMMENT '题序（随机，脱离题库顺序）',
  score         DECIMAL(10,1) NOT NULL DEFAULT 0 COMMENT '分值',
  tenant_id     BIGINT      NOT NULL,
  deleted       TINYINT     NOT NULL DEFAULT 0,
  created_by    BIGINT      NULL,
  updated_by    BIGINT      NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_review_mock_item (paper_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 复习模考卷题明细';

-- ============================================================
-- 4.6 sys_dict 参数（幂等）
-- ============================================================
INSERT IGNORE INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20260804061, 1, 'ai_review_daily_new_cap',  'AI同步学：每日新题上限',        '120',            'enabled', NOW(), NOW(), 1, 1, 0),
(20260804062, 1, 'ai_review_daily_total_cap','AI同步学：每日总题量上限',      '200',            'enabled', NOW(), NOW(), 1, 1, 0),
(20260804063, 1, 'ai_review_mock_size',      'AI同步学：模考卷题量',          '100',            'enabled', NOW(), NOW(), 1, 1, 0),
(20260804064, 1, 'ai_review_default_days',   'AI同步学：默认复习天数',        '20',             'enabled', NOW(), NOW(), 1, 1, 0),
(20260804065, 1, 'ai_review_interrupt_gap',  'AI同步学：中断判定间隔天数',    '2',              'enabled', NOW(), NOW(), 1, 1, 0),
(20260804066, 1, 'ai_review_model_code',     'AI同步学：LLM 模型配置 code',   'deepseek-review','enabled', NOW(), NOW(), 1, 1, 0);

-- ============================================================
-- §6 模型配置种子（api_key 留空，由运维在管理端填写，AGENTS.md 不落真实密钥）
-- ============================================================
INSERT IGNORE INTO exam_faq_model_config
(id, tenant_id, model_code, model_name, base_url, api_key, api_format, model_id, temperature, max_tokens, thinking, extra_config, status, created_at, updated_at, created_by, updated_by, deleted) VALUES
(20260804067, 1, 'deepseek-review', 'DeepSeek（AI同步学）', 'https://api.deepseek.com', '', 'openai', 'deepseek-v4-flash', 0.7, 6000, '', NULL, 'enabled', NOW(), NOW(), 1, 1, 0);
