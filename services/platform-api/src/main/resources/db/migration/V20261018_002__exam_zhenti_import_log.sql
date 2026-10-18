-- 历年真题批量导入记录（方案 §2.9）：进度即状态，D6 轮询直接读表，不依赖进程内内存态。
-- 取号说明同 V20261018_001。
--
-- results_json 保存逐文件结果（[{fileName, ok, id, title, year, reason}]），
-- 同步模式不落库、异步模式在 Worker 每处理完一个文件后整行更新。

CREATE TABLE IF NOT EXISTS exam_zhenti_import_log (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 10159,
  exam_id BIGINT NOT NULL,
  operator_id BIGINT NULL COMMENT '发起导入的管理员 sys_user.id',
  total INT NOT NULL DEFAULT 0 COMMENT 'zip 内待处理 PDF 数',
  finished INT NOT NULL DEFAULT 0 COMMENT '已处理数（成功+失败）',
  status VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT 'running/done/failed',
  results_json MEDIUMTEXT NULL COMMENT '逐文件结果 JSON',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_zhenti_import_log (tenant_id, exam_id, created_at, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '历年真题批量导入记录';
