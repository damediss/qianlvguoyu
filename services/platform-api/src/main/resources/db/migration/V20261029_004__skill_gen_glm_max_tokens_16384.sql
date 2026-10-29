-- skill-gen-glm max_tokens 16384 修正（GLM 数组塌缩修复配套，2026-10-29）
-- 背景：5 题完整输出（三件套+评分表）约 5000~8500 token，原 8192 偏紧有截断风险；
-- 种子迁移 V20260910_013 本就写 16384，但生产配置行后被人工调成 8192（生产日志 2026-10-29 14:03 实测）。
-- 幂等：UPDATE 天然幂等；测试库已是 16384 无副作用，生产库 8192→16384。
-- 取号依据（2026-10-29 双库查询）：gjjddb2026(测试) 与 gjjddbpro(生产) MAX(version) 均为 20261029.003，
-- 本迁移取 20261029.004（共用目录，单线递进）。

UPDATE exam_faq_model_config
SET max_tokens = 16384,
    updated_at = NOW()
WHERE model_code = 'skill-gen-glm'
  AND deleted = 0;
