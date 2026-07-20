-- 扩展字典项值字段长度，支持最长 500 字符
ALTER TABLE sys_dict_item MODIFY COLUMN item_value text NOT NULL;
