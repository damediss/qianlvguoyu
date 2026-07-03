-- 为菜单表增加默认展开字段(1=展开 0=收起)
ALTER TABLE sys_menu
  ADD COLUMN expanded TINYINT NOT NULL DEFAULT 1 COMMENT '目录默认展开(1展开 0收起)' AFTER visible;

-- 兼容旧数据：全部默认展开
UPDATE sys_menu SET expanded = 1 WHERE expanded IS NULL;
