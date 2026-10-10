-- 申报条件字典：扩 item_label + 按租户幂等 seed（方案 A）
-- 数据来源：docs/04_requirements/phase2-training/申报条件_seed_preview.json

ALTER TABLE sys_dict_item MODIFY COLUMN item_label VARCHAR(512) NOT NULL COMMENT '字典项标签（申报条件全文等）';

-- tenant 10159
INSERT INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted)
SELECT 2026091410000000001, 10159, 'signup_declaration_condition', '报名申报条件', '', 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict
  WHERE tenant_id = 10159 AND code = 'signup_declaration_condition' AND deleted = 0
);

SET @dict_id_0 := (
  SELECT id FROM sys_dict
  WHERE tenant_id = 10159 AND code = 'signup_declaration_condition' AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000002, 10159, @dict_id_0, '0-01', '（1）年满16周岁，拟从事本职业或相关职业工作。', 1, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '0-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）年满16周岁，拟从事本职业或相关职业工作。', sort_order = 1, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '0-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000003, 10159, @dict_id_0, '0-02', '（2）年满16周岁，从事本职业或相关职业工作。', 2, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '0-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）年满16周岁，从事本职业或相关职业工作。', sort_order = 2, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '0-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000004, 10159, @dict_id_0, '1-01', '（1）累计从事本职业或相关职业工作满5年。', 3, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）累计从事本职业或相关职业工作满5年。', sort_order = 3, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000005, 10159, @dict_id_0, '1-02', '（2）取得本职业或相关职业五级/初级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满3年。', 4, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得本职业或相关职业五级/初级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满3年。', sort_order = 4, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000006, 10159, @dict_id_0, '1-03', '（3）取得本专业或相关专业的技工院校或中等及以上职业院校、专科及以上普通高等学校毕业证书(含在读应届毕业生)。', 5, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得本专业或相关专业的技工院校或中等及以上职业院校、专科及以上普通高等学校毕业证书(含在读应届毕业生)。', sort_order = 5, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000007, 10159, @dict_id_0, '1-04', '（4）取得应急管理部门颁发合法有效的低压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满3年，可申报电工四级/中级工。', 6, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得应急管理部门颁发合法有效的低压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满3年，可申报电工四级/中级工。', sort_order = 6, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000008, 10159, @dict_id_0, '1-05', '（5）取得住房城乡建设部门颁发合法有效的建筑电工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报电工四级/中级工。', 7, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得住房城乡建设部门颁发合法有效的建筑电工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报电工四级/中级工。', sort_order = 7, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000009, 10159, @dict_id_0, '1-06', '（6）取得应急管理部门颁发合法有效的熔化焊接与热切割作业特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', 8, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得应急管理部门颁发合法有效的熔化焊接与热切割作业特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', sort_order = 8, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000010, 10159, @dict_id_0, '1-07', '（7）取得住房城乡建设部门颁发合法有效的建筑焊工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', 9, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得住房城乡建设部门颁发合法有效的建筑焊工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', sort_order = 9, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000011, 10159, @dict_id_0, '1-08', '（8）取得对应目录教育部门相关初级“X证书”后，累计从事本职业或相关职业工作满3年，可申报对应职业（工种）四级/中级工。', 10, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '1-08' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（8）取得对应目录教育部门相关初级“X证书”后，累计从事本职业或相关职业工作满3年，可申报对应职业（工种）四级/中级工。', sort_order = 10, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '1-08'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000012, 10159, @dict_id_0, '2-01', '（1）累计从事本职业或相关职业工作满10年。', 11, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）累计从事本职业或相关职业工作满10年。', sort_order = 11, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000013, 10159, @dict_id_0, '2-02', '（2）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满4年。', 12, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满4年。', sort_order = 12, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000014, 10159, @dict_id_0, '2-03', '（3）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 13, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 13, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000015, 10159, @dict_id_0, '2-04', '（4）取得本专业或相关专业的技工院校高级工班及以上毕业证书（含在读应届毕业生）。', 14, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本专业或相关专业的技工院校高级工班及以上毕业证书（含在读应届毕业生）。', sort_order = 14, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000016, 10159, @dict_id_0, '2-05', '（5）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生）。', 15, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生）。', sort_order = 15, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000017, 10159, @dict_id_0, '2-06', '（6）取得经评估论证的高等职业学校、专科及以上普通高等学校本专业或相关专业的毕业证书（含在读应届毕业生）。', 16, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得经评估论证的高等职业学校、专科及以上普通高等学校本专业或相关专业的毕业证书（含在读应届毕业生）。', sort_order = 16, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000018, 10159, @dict_id_0, '2-07', '（7）取得应急管理部门颁发合法有效的高压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满 4 年，可申报电工三级/高级工。', 17, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得应急管理部门颁发合法有效的高压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满 4 年，可申报电工三级/高级工。', sort_order = 17, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000019, 10159, @dict_id_0, '2-08', '（8）取得市场监管部门颁发合法有效的特种设备焊接操作人员证书后，累计从事本职业或相关职业工作满 4 年，可申报焊工三级/高级工。', 18, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-08' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（8）取得市场监管部门颁发合法有效的特种设备焊接操作人员证书后，累计从事本职业或相关职业工作满 4 年，可申报焊工三级/高级工。', sort_order = 18, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-08'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000020, 10159, @dict_id_0, '2-09', '（9）取得对应目录教育部门相关中级“X 证书”后，累计从事本职业或相关职业工作满 4 年，可申报对应职业（工种）三级/高级工。', 19, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-09' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（9）取得对应目录教育部门相关中级“X 证书”后，累计从事本职业或相关职业工作满 4 年，可申报对应职业（工种）三级/高级工。', sort_order = 19, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-09'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000021, 10159, @dict_id_0, '2-10', '（10）.取得对应目录教育部门相关中级“X 证书”，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生），可申报对应职业（工种）三级/高级工。', 20, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '2-10' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（10）.取得对应目录教育部门相关中级“X 证书”，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生），可申报对应职业（工种）三级/高级工。', sort_order = 20, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '2-10'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000022, 10159, @dict_id_0, '3-01', '（1）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满5年。', 21, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满5年。', sort_order = 21, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000023, 10159, @dict_id_0, '3-02', '（2）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', 22, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', sort_order = 22, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000024, 10159, @dict_id_0, '3-03', '（3）取得符合专业对应关系的中级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 23, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的中级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 23, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000025, 10159, @dict_id_0, '3-04', '（4）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书的高级技工学校、技师学院毕业生，累计从事本职业或相关职业满2年。', 24, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书的高级技工学校、技师学院毕业生，累计从事本职业或相关职业满2年。', sort_order = 24, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000026, 10159, @dict_id_0, '3-05', '（5）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书满2年的技师学院预备技师班、技师班学生。取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', 25, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书满2年的技师学院预备技师班、技师班学生。取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', sort_order = 25, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000027, 10159, @dict_id_0, '3-06', '（6）取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', 26, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', sort_order = 26, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000028, 10159, @dict_id_0, '3-07', '（7）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满 5 年，并在取得对应目录教育部门相关高级“X 证书”后，从事本职业或相关职业工作满 1 年，可申报对应职业（工种）二级/技师。', 27, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '3-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满 5 年，并在取得对应目录教育部门相关高级“X 证书”后，从事本职业或相关职业工作满 1 年，可申报对应职业（工种）二级/技师。', sort_order = 27, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '3-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000029, 10159, @dict_id_0, '4-01', '（1）取得本职业或相关职业二级/技师职业资格（技能等级）证书后，累计从事本职业或相关职业工作满5年。', 28, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '4-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）取得本职业或相关职业二级/技师职业资格（技能等级）证书后，累计从事本职业或相关职业工作满5年。', sort_order = 28, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '4-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000030, 10159, @dict_id_0, '4-02', '（2）取得符合专业对应关系的中级职称后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业二级/技师职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', 29, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '4-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得符合专业对应关系的中级职称后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业二级/技师职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', sort_order = 29, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '4-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000031, 10159, @dict_id_0, '4-03', '（3）取得符合专业对应关系的高级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 30, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '4-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的高级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 30, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '4-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000032, 10159, @dict_id_0, '4-04', '（4）取得本职业或相关职业二级/技师职业资格（职业技能等级）证书满2年的高级技师班企业新型学徒制学徒。', 31, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_0 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 10159 AND dict_id = @dict_id_0
      AND item_value = '4-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本职业或相关职业二级/技师职业资格（职业技能等级）证书满2年的高级技师班企业新型学徒制学徒。', sort_order = 31, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 10159
  AND dict_id = @dict_id_0
  AND item_value = '4-04'
  AND deleted = 0;

-- tenant 2062723418189152258
INSERT INTO sys_dict (id, tenant_id, code, name, value, status, created_at, updated_at, created_by, updated_by, deleted)
SELECT 2026091410000000002, 2062723418189152258, 'signup_declaration_condition', '报名申报条件', '', 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict
  WHERE tenant_id = 2062723418189152258 AND code = 'signup_declaration_condition' AND deleted = 0
);

SET @dict_id_1 := (
  SELECT id FROM sys_dict
  WHERE tenant_id = 2062723418189152258 AND code = 'signup_declaration_condition' AND deleted = 0
  ORDER BY id LIMIT 1
);

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000102, 2062723418189152258, @dict_id_1, '0-01', '（1）年满16周岁，拟从事本职业或相关职业工作。', 1, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '0-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）年满16周岁，拟从事本职业或相关职业工作。', sort_order = 1, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '0-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000103, 2062723418189152258, @dict_id_1, '0-02', '（2）年满16周岁，从事本职业或相关职业工作。', 2, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '0-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）年满16周岁，从事本职业或相关职业工作。', sort_order = 2, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '0-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000104, 2062723418189152258, @dict_id_1, '1-01', '（1）累计从事本职业或相关职业工作满5年。', 3, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）累计从事本职业或相关职业工作满5年。', sort_order = 3, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000105, 2062723418189152258, @dict_id_1, '1-02', '（2）取得本职业或相关职业五级/初级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满3年。', 4, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得本职业或相关职业五级/初级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满3年。', sort_order = 4, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000106, 2062723418189152258, @dict_id_1, '1-03', '（3）取得本专业或相关专业的技工院校或中等及以上职业院校、专科及以上普通高等学校毕业证书(含在读应届毕业生)。', 5, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得本专业或相关专业的技工院校或中等及以上职业院校、专科及以上普通高等学校毕业证书(含在读应届毕业生)。', sort_order = 5, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000107, 2062723418189152258, @dict_id_1, '1-04', '（4）取得应急管理部门颁发合法有效的低压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满3年，可申报电工四级/中级工。', 6, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得应急管理部门颁发合法有效的低压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满3年，可申报电工四级/中级工。', sort_order = 6, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000108, 2062723418189152258, @dict_id_1, '1-05', '（5）取得住房城乡建设部门颁发合法有效的建筑电工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报电工四级/中级工。', 7, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得住房城乡建设部门颁发合法有效的建筑电工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报电工四级/中级工。', sort_order = 7, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000109, 2062723418189152258, @dict_id_1, '1-06', '（6）取得应急管理部门颁发合法有效的熔化焊接与热切割作业特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', 8, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得应急管理部门颁发合法有效的熔化焊接与热切割作业特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', sort_order = 8, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000110, 2062723418189152258, @dict_id_1, '1-07', '（7）取得住房城乡建设部门颁发合法有效的建筑焊工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', 9, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得住房城乡建设部门颁发合法有效的建筑焊工特种作业操作证书后，累计从事本职业或相关职业工作满 3年，可申报焊工四级/中级工。', sort_order = 9, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000111, 2062723418189152258, @dict_id_1, '1-08', '（8）取得对应目录教育部门相关初级“X证书”后，累计从事本职业或相关职业工作满3年，可申报对应职业（工种）四级/中级工。', 10, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '1-08' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（8）取得对应目录教育部门相关初级“X证书”后，累计从事本职业或相关职业工作满3年，可申报对应职业（工种）四级/中级工。', sort_order = 10, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '1-08'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000112, 2062723418189152258, @dict_id_1, '2-01', '（1）累计从事本职业或相关职业工作满10年。', 11, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）累计从事本职业或相关职业工作满10年。', sort_order = 11, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000113, 2062723418189152258, @dict_id_1, '2-02', '（2）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满4年。', 12, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满4年。', sort_order = 12, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000114, 2062723418189152258, @dict_id_1, '2-03', '（3）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 13, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 13, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000115, 2062723418189152258, @dict_id_1, '2-04', '（4）取得本专业或相关专业的技工院校高级工班及以上毕业证书（含在读应届毕业生）。', 14, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本专业或相关专业的技工院校高级工班及以上毕业证书（含在读应届毕业生）。', sort_order = 14, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000116, 2062723418189152258, @dict_id_1, '2-05', '（5）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生）。', 15, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得本职业或相关职业四级/中级工职业资格（职业技能等级）证书，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生）。', sort_order = 15, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000117, 2062723418189152258, @dict_id_1, '2-06', '（6）取得经评估论证的高等职业学校、专科及以上普通高等学校本专业或相关专业的毕业证书（含在读应届毕业生）。', 16, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得经评估论证的高等职业学校、专科及以上普通高等学校本专业或相关专业的毕业证书（含在读应届毕业生）。', sort_order = 16, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000118, 2062723418189152258, @dict_id_1, '2-07', '（7）取得应急管理部门颁发合法有效的高压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满 4 年，可申报电工三级/高级工。', 17, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得应急管理部门颁发合法有效的高压电工作业特种作业操作证书后，累计从事本职业或相关职业工作满 4 年，可申报电工三级/高级工。', sort_order = 17, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000119, 2062723418189152258, @dict_id_1, '2-08', '（8）取得市场监管部门颁发合法有效的特种设备焊接操作人员证书后，累计从事本职业或相关职业工作满 4 年，可申报焊工三级/高级工。', 18, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-08' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（8）取得市场监管部门颁发合法有效的特种设备焊接操作人员证书后，累计从事本职业或相关职业工作满 4 年，可申报焊工三级/高级工。', sort_order = 18, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-08'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000120, 2062723418189152258, @dict_id_1, '2-09', '（9）取得对应目录教育部门相关中级“X 证书”后，累计从事本职业或相关职业工作满 4 年，可申报对应职业（工种）三级/高级工。', 19, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-09' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（9）取得对应目录教育部门相关中级“X 证书”后，累计从事本职业或相关职业工作满 4 年，可申报对应职业（工种）三级/高级工。', sort_order = 19, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-09'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000121, 2062723418189152258, @dict_id_1, '2-10', '（10）.取得对应目录教育部门相关中级“X 证书”，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生），可申报对应职业（工种）三级/高级工。', 20, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '2-10' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（10）.取得对应目录教育部门相关中级“X 证书”，并取得高等职业学校、专科及以上普通高等学校本专业或相关专业毕业证书（含在读应届毕业生），可申报对应职业（工种）三级/高级工。', sort_order = 20, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '2-10'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000122, 2062723418189152258, @dict_id_1, '3-01', '（1）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满5年。', 21, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，累计从事本职业或相关职业工作满5年。', sort_order = 21, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000123, 2062723418189152258, @dict_id_1, '3-02', '（2）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', 22, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', sort_order = 22, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000124, 2062723418189152258, @dict_id_1, '3-03', '（3）取得符合专业对应关系的中级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 23, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的中级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 23, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000125, 2062723418189152258, @dict_id_1, '3-04', '（4）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书的高级技工学校、技师学院毕业生，累计从事本职业或相关职业满2年。', 24, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书的高级技工学校、技师学院毕业生，累计从事本职业或相关职业满2年。', sort_order = 24, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-04'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000126, 2062723418189152258, @dict_id_1, '3-05', '（5）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书满2年的技师学院预备技师班、技师班学生。取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', 25, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-05' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（5）取得本职业或相关职业三级/高级工职业资格（职业技能等级）证书满2年的技师学院预备技师班、技师班学生。取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', sort_order = 25, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-05'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000127, 2062723418189152258, @dict_id_1, '3-06', '（6）取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', 26, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-06' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（6）取得对应目录教育部门相关高级“X证书”后，累计从事本职业或相关职业工作满5年，可申报对应职业（工种二级/技师。', sort_order = 26, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-06'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000128, 2062723418189152258, @dict_id_1, '3-07', '（7）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满 5 年，并在取得对应目录教育部门相关高级“X 证书”后，从事本职业或相关职业工作满 1 年，可申报对应职业（工种）二级/技师。', 27, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '3-07' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（7）取得符合专业对应关系的初级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满 5 年，并在取得对应目录教育部门相关高级“X 证书”后，从事本职业或相关职业工作满 1 年，可申报对应职业（工种）二级/技师。', sort_order = 27, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '3-07'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000129, 2062723418189152258, @dict_id_1, '4-01', '（1）取得本职业或相关职业二级/技师职业资格（技能等级）证书后，累计从事本职业或相关职业工作满5年。', 28, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '4-01' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（1）取得本职业或相关职业二级/技师职业资格（技能等级）证书后，累计从事本职业或相关职业工作满5年。', sort_order = 28, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '4-01'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000130, 2062723418189152258, @dict_id_1, '4-02', '（2）取得符合专业对应关系的中级职称后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业二级/技师职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', 29, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '4-02' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（2）取得符合专业对应关系的中级职称后，累计从事本职业或相关职业工作满5年，并在取得本职业或相关职业二级/技师职业资格（职业技能等级）证书后，从事本职业或相关职业工作满1年。', sort_order = 29, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '4-02'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000131, 2062723418189152258, @dict_id_1, '4-03', '（3）取得符合专业对应关系的高级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', 30, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '4-03' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（3）取得符合专业对应关系的高级职称（专业技术人员职业资格）后，累计从事本职业或相关职业工作满1年。', sort_order = 30, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '4-03'
  AND deleted = 0;

INSERT INTO sys_dict_item (
  id, tenant_id, dict_id, item_value, item_label, sort_order, status,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT 2026091411000000132, 2062723418189152258, @dict_id_1, '4-04', '（4）取得本职业或相关职业二级/技师职业资格（职业技能等级）证书满2年的高级技师班企业新型学徒制学徒。', 31, 'enabled', NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE @dict_id_1 IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item
    WHERE tenant_id = 2062723418189152258 AND dict_id = @dict_id_1
      AND item_value = '4-04' AND deleted = 0
  );
UPDATE sys_dict_item
SET item_label = '（4）取得本职业或相关职业二级/技师职业资格（职业技能等级）证书满2年的高级技师班企业新型学徒制学徒。', sort_order = 31, status = 'enabled', updated_at = NOW()
WHERE tenant_id = 2062723418189152258
  AND dict_id = @dict_id_1
  AND item_value = '4-04'
  AND deleted = 0;

