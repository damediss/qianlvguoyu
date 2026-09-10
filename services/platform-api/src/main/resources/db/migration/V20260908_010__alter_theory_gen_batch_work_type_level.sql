-- 理论知识试题生成：批次表冗余工种与等级（导出文件命名与“等级”列用）
-- 方案：docs/04_requirements/理论知识试题生成管理方案.md（2026-09-08 需求变更）
-- 版本号依据（2026-09-08 双库查询）：gjjddb2026(测试) MAX=20260908.002；gjjddbpro(生产) MAX=20260906.003，
-- 取 20260908.010 两库均不冲突（踩坑 #7）。存量批次的工种/等级从蓝图回填。

ALTER TABLE exam_theory_gen_batch
  ADD COLUMN work_type_id INT NULL COMMENT '工种 exam_work_type.id（冗余自蓝图，导出命名用）',
  ADD COLUMN level TINYINT NULL COMMENT '等级 1~5（5=五级/初级工…1=一级/高级技师，冗余自蓝图）';

-- 存量批次回填（冗余字段与蓝图保持一致）
UPDATE exam_theory_gen_batch b
JOIN exam_theory_blueprint bp ON bp.id = b.blueprint_id
SET b.work_type_id = bp.work_type_id,
    b.level = bp.level
WHERE b.work_type_id IS NULL AND b.level IS NULL;
