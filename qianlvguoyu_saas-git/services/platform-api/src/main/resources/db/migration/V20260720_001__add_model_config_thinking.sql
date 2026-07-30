-- 模型配置表新增 thinking 字段（LongCat 思考模式）
ALTER TABLE exam_faq_model_config ADD COLUMN thinking VARCHAR(16) DEFAULT NULL COMMENT 'LongCat 思考模式：enabled/disabled，非 LongCat 模型留空';
