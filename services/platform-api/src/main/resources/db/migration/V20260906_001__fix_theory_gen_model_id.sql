-- P0（理论知识试题生成-并发与成本优化方案 v0.3）：模型编码修正为官方全小写 glm-5.3-flash
-- 依据：智谱官方文档模型编码 glm-5.3-flash（https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5.3-flash.md）
-- 空库回放链路：V20260904_013 先种子 'GLM-5.3-Flash'（主方案 v1.2 C3 笔误），本迁移统一修正；
-- 已手工修正的库（如测试库已改为小写）不受影响，可重复执行（幂等）。
-- 版本号依据：gjjddb2026 库 MAX(version)=20260904.013（2026-09-06 实查，踩坑 #7）。

UPDATE exam_faq_model_config
SET model_id = 'glm-5.3-flash', updated_at = NOW()
WHERE model_code = 'theory-gen-glm'
  AND model_id <> 'glm-5.3-flash';
