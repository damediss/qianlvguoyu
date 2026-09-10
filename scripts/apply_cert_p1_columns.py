# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host="43.138.163.7",
    port=3308,
    user="gjjd",
    password="w-#Y+=g7jdsJLjD8",
    database="gjjddb2026",
    charset="utf8mb4",
)
cur = conn.cursor()


def has_column(table: str, col: str) -> bool:
    cur.execute(
        """
        SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=%s AND COLUMN_NAME=%s
        """,
        (table, col),
    )
    return cur.fetchone()[0] > 0


def add_column(sql: str, table: str, col: str) -> None:
    if has_column(table, col):
        print(f"skip exists {table}.{col}")
        return
    print(f"add {table}.{col}")
    cur.execute(sql)


# V20260904_001
add_column(
    "ALTER TABLE train_certificate ADD COLUMN occupation VARCHAR(255) NULL COMMENT '职业名称（证面）' AFTER work_type",
    "train_certificate",
    "occupation",
)
add_column(
    "ALTER TABLE train_certificate ADD COLUMN evaluation_org VARCHAR(255) NULL COMMENT '评价机构/落款机构' AFTER issuing_authority",
    "train_certificate",
    "evaluation_org",
)
add_column(
    "ALTER TABLE train_certificate ADD COLUMN id_doc_type VARCHAR(64) NULL COMMENT '证件类型（证面，如居民身份证）' AFTER user_name",
    "train_certificate",
    "id_doc_type",
)

# V20260904_002
add_column(
    "ALTER TABLE train_certificate ADD COLUMN query_urls VARCHAR(1000) NULL COMMENT '证面查询地址原文 JSON 数组' AFTER evaluation_org",
    "train_certificate",
    "query_urls",
)
add_column(
    "ALTER TABLE train_certificate ADD COLUMN level_normalized VARCHAR(64) NULL COMMENT '等级五档归一（报表用，证面原文仍在 level）' AFTER level",
    "train_certificate",
    "level_normalized",
)
add_column(
    "ALTER TABLE train_certificate ADD COLUMN file_id BIGINT NULL COMMENT '证书文件 ID' AFTER file_path",
    "train_certificate",
    "file_id",
)

cur.execute(
    """
CREATE TABLE IF NOT EXISTS train_certificate_import_batch (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL COMMENT '租户ID',
  operator_id BIGINT NULL COMMENT '操作人',
  class_ids_json VARCHAR(1000) NULL COMMENT '勾选班级 ID JSON',
  preview_id VARCHAR(64) NULL COMMENT '预览 ID',
  total_files INT NULL DEFAULT 0 COMMENT '预览文件数',
  matched INT NULL DEFAULT 0,
  written INT NULL DEFAULT 0 COMMENT '新建证书行数',
  updated INT NULL DEFAULT 0 COMMENT '更新证书行数',
  skipped INT NULL DEFAULT 0,
  errors_json TEXT NULL COMMENT '跳过/错误摘要 JSON',
  remark VARCHAR(500) NULL,
  create_time DATETIME NULL,
  INDEX idx_cert_import_batch_tenant (tenant_id),
  INDEX idx_cert_import_batch_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='毕业典礼证书文件夹导入批次'
"""
)
print("batch table ok")

# Mark flyway history if table exists (best-effort)
cur.execute(
    """
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='flyway_schema_history'
    """
)
if cur.fetchone()[0]:
    for ver, script, checksum in [
        (
            "20260904.001",
            "V20260904_001__train_certificate_full_parse_fields.sql",
            None,
        ),
        (
            "20260904.002",
            "V20260904_002__train_certificate_import_p1.sql",
            None,
        ),
    ]:
        cur.execute(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version=%s OR script=%s",
            (ver, script),
        )
        if cur.fetchone()[0] == 0:
            cur.execute(
                """
                INSERT INTO flyway_schema_history
                  (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
                SELECT COALESCE(MAX(installed_rank),0)+1, %s, %s, 'SQL', %s, NULL, USER(), NOW(), 0, 1
                FROM flyway_schema_history
                """,
                (ver, script.replace(".sql", ""), script),
            )
            print("flyway recorded", ver)
        else:
            print("flyway already has", ver)

conn.commit()
cur.execute(
    "SHOW COLUMNS FROM train_certificate WHERE Field IN ('occupation','evaluation_org','id_doc_type','query_urls','level_normalized','file_id')"
)
print("columns now:", cur.fetchall())
conn.close()
print("done")
