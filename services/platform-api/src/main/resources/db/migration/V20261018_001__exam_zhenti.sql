-- 历年真题主表（方案 docs/04_requirements/历年真题-实施计划与测试验收.md §2.2）
-- 取号：2026-10-18 已查两库 MAX(version) = 测试库 20261014.004 / 生产库 20261014.004，
--       共用目录取 V20261018_001（撞号会导致 jar 全环境无法启动）。
--
-- 说明：
--   file_key 为存储对象键（local/COS 同口径），任何接口出参不得返回；
--   year 用 VARCHAR(10) 保留历史数据的“2024上”等半年份写法；
--   deleted 由 MyBatis-Plus @TableLogic 接管，软删 90 天后由定时任务物理清理。

CREATE TABLE IF NOT EXISTS exam_zhenti (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  exam_id BIGINT NOT NULL COMMENT '考试项目 id（train_exam_list.id）',
  title VARCHAR(200) NOT NULL COMMENT '真题标题',
  year VARCHAR(10) NULL COMMENT '年份，允许“2024上”等写法',
  file_key VARCHAR(512) NOT NULL COMMENT '存储对象键，禁止出参',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '字节数',
  file_md5 CHAR(32) NOT NULL COMMENT '文件 MD5，去重键',
  sort INT NOT NULL DEFAULT 0 COMMENT '同 exam 下排序，升序',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
  meta_pending TINYINT NOT NULL DEFAULT 0 COMMENT '1 待完善（缺年份/工种等级）',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_zhenti_list (tenant_id, exam_id, status, deleted),
  KEY idx_zhenti_md5 (exam_id, file_md5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '历年真题';
