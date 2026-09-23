-- AI 课件生成(courseware):知识点清单表 + 大纲页挂接知识点
-- 背景(方案 v1.4):基本要求/工作要求章从"概括页"改为"逐最小知识点一页",
-- 解析层确定性枚举 x.y.z 知识点落库,大纲组装按此替换。
-- 版本号依据(2026-09-21 双库查询):gjjddb2026(测试) MAX=20260921.020;gjjddbpro(生产) MAX=20260921.020,
-- 本迁移取 20260921.101,在两库最大值之上并留足同日期间隔(踩坑 #7)。
-- MySQL DDL 自动提交：ADD COLUMN 做 information_schema 守卫，支持失败后重跑。

CREATE TABLE IF NOT EXISTS cw_knowledge_point (
  id BIGINT NOT NULL COMMENT '主键(雪花 ID)',
  tenant_id BIGINT NOT NULL COMMENT '租户 ID',
  project_id BIGINT NOT NULL COMMENT '课件项目 ID',
  category VARCHAR(16) NOT NULL COMMENT 'basic基本要求 / work工作要求',
  level_name VARCHAR(64) NULL COMMENT '等级(如 五级/初级工;工作要求才有)',
  job_function VARCHAR(120) NULL COMMENT '职业功能(工作要求表第 1 列 / 基本要求所属节)',
  job_content VARCHAR(200) NULL COMMENT '工作内容(x.y 组名)',
  code VARCHAR(16) NOT NULL COMMENT '知识点编号(x.y.z;基本要求为 2.1.1 型)',
  title VARCHAR(300) NOT NULL COMMENT '知识点标题',
  skill_text TEXT NULL COMMENT '技能要求原文(工作要求技能要求列)',
  knowledge_text TEXT NULL COMMENT '相关知识要求原文(工作要求知识列 / 基本要求小节正文)',
  source_ref VARCHAR(32) NULL COMMENT '来源小节 ref(S1:N)',
  sort_no INT NOT NULL COMMENT '文档顺序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cw_kp_project (project_id, sort_no),
  KEY idx_cw_kp_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 课件知识点清单';

-- 大纲页挂接知识点(仅知识点页非空)
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE cw_outline_page ADD COLUMN knowledge_point_id BIGINT NULL COMMENT ''知识点 ID(cw_knowledge_point.id)'' AFTER source_refs',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'cw_outline_page'
    AND column_name = 'knowledge_point_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
