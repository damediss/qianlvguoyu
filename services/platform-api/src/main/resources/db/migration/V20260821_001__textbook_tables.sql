-- 职业标准驱动教材生成（一期 M1/M2）：textbook_project + textbook_outline 两张表
-- 方案：docs/04_requirements/职业标准驱动教材生成_一期实施设计.md v1.0 §2.2/§2.5
-- 取号（2026-08-21 双库侦察）：gjjddb2026（测试）与 gjjddbpro（生产）MAX(version) 均为 20261020.004；
-- 本迁移取 20260821.001——低于已应用最大值，依赖 Flyway out-of-order=true 生效
-- （application.yml 已配置；与 V20260920_101 等既有低版本车道同理）。
-- 幂等：CREATE TABLE IF NOT EXISTS，空库可全量跑通。

-- ============================================================
-- 1) textbook_project 教材工程（工种 × 方向 × 等级 三元组分册）
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_project (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  work_type_title VARCHAR(128) NOT NULL COMMENT '职业名称（解析回填，可手改）',
  series_no VARCHAR(64) NULL COMMENT '职业编码',
  standard_version VARCHAR(32) NULL COMMENT '标准版本（如 2020年版）',
  file_id BIGINT NOT NULL COMMENT '源 docx 文件 ID（sys_file.id，file 模块）',
  file_hash VARCHAR(64) NULL COMMENT '源文件 sha256（防重复上传提示）',
  direction VARCHAR(64) NOT NULL DEFAULT '默认' COMMENT '方向',
  level_code INT NOT NULL COMMENT '等级代码：5=五级/初级工 … 1=一级/高级技师',
  function_nos_json VARCHAR(255) NOT NULL COMMENT '方向→职业功能序号 JSON 数组，如 [1,2,12]；人工登记优先于解析',
  training_hours INT NOT NULL COMMENT '培训参考学时（解析 1.8 回填，可手改）',
  words_per_hour INT NOT NULL DEFAULT 2500 COMMENT '每学时字数系数（可配置）',
  total_words INT NOT NULL COMMENT '总字数 = training_hours × words_per_hour',
  status VARCHAR(16) NOT NULL DEFAULT 'parsed' COMMENT 'parsed已建未建骨架（兜底）/outlined骨架配比完成/reviewed评审通过；generating/done 二期预留',
  parse_warnings TEXT NULL COMMENT '解析告警 JSON 数组（学时缺失/权重表缺失/方向句未识别等）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_textbook_project (tenant_id, work_type_title, direction, level_code, deleted),
  KEY idx_textbook_project_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材工程';

-- ============================================================
-- 2) textbook_outline 大纲树（篇/章/节/点）
-- ============================================================
CREATE TABLE IF NOT EXISTS textbook_outline (
  id BIGINT NOT NULL COMMENT '主键（雪花 ID）',
  tenant_id BIGINT NOT NULL DEFAULT 10159 COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '教材工程 ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点 ID（0=根/篇）',
  node_level TINYINT NOT NULL COMMENT '1=篇 2=章 3=节 4=点',
  dimension VARCHAR(8) NULL COMMENT '点级：theory/skill；篇章节 NULL（写回 NULL 需 @TableField(updateStrategy = ALWAYS)）',
  source_no VARCHAR(32) NULL COMMENT '标准原始编号（2.2.1 / 1.1），溯源用，不随编辑变',
  title_raw VARCHAR(255) NOT NULL COMMENT '标准原文',
  title VARCHAR(255) NOT NULL COMMENT '显示标题（可编辑，初始=title_raw）',
  weight DECIMAL(5,2) NULL COMMENT '权重%（章级）',
  target_words INT NULL COMMENT '目标字数（章/节级）',
  words_manual TINYINT NOT NULL DEFAULT 0 COMMENT '字数被人工调整=1（重算配比时保留）',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '同级排序（上移/下移改它）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_textbook_outline_project (tenant_id, project_id, parent_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材大纲树';
