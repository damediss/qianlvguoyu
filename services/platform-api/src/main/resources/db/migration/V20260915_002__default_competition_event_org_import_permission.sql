-- 新增赛事参与企业默认允许名单导入；显式传入 0 时仍可关闭权限。
ALTER TABLE competition_event_org
  MODIFY COLUMN import_permission TINYINT NOT NULL DEFAULT 1;
