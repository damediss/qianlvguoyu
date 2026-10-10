-- 添加身份证 OCR 相关字段，对齐 PHP 旧系统 platform_user_detail 表结构
ALTER TABLE platform_user_detail ADD COLUMN idcard_front VARCHAR(500) NULL COMMENT '身份证正面图片URL';
ALTER TABLE platform_user_detail ADD COLUMN idcard_back VARCHAR(500) NULL COMMENT '身份证反面图片URL';
ALTER TABLE platform_user_detail ADD COLUMN idcard_start_date VARCHAR(20) NULL COMMENT '身份证签发日期';
ALTER TABLE platform_user_detail ADD COLUMN idcard_end_date VARCHAR(20) NULL COMMENT '身份证失效日期';
