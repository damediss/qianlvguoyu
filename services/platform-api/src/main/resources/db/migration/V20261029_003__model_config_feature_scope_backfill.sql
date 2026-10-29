-- 模型配置适用范围回填（方案 v1.5 §12.6 补充）
-- 背景：V20261029_001 新增 feature_scope，老数据默认 common，导致理论/技能/知识修正/课件/同步学模型
--       仍可能出现在 FAQ 转换等通用模型下拉中。本条按 model_code 做幂等回填，不改历史迁移。
-- 取号：2026-10-29，测试库当前 20261029.002（002 已被并发功能占用）、生产库当前 20261027.006，
--       故本文件取 20261029.003。

UPDATE exam_faq_model_config SET feature_scope = 'theory-gen'
WHERE model_code = 'theory-gen-glm' AND feature_scope = 'common';

UPDATE exam_faq_model_config SET feature_scope = 'skill-gen'
WHERE model_code = 'skill-gen-glm' AND feature_scope = 'common';

UPDATE exam_faq_model_config SET feature_scope = 'knowledge-fix'
WHERE model_code IN ('knowledge-fix-pro', 'knowledge-fix-jev') AND feature_scope = 'common';

UPDATE exam_faq_model_config SET feature_scope = 'courseware-gen'
WHERE model_code = 'courseware-gen-glm' AND feature_scope = 'common';

UPDATE exam_faq_model_config SET feature_scope = 'review-sync'
WHERE model_code = 'deepseek-review' AND feature_scope = 'common';
