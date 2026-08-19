-- V20260818_002: 认定考试场次增加考场场地，供信息采集座次签到表第二列/页眉使用
-- 版本号依据：仓库已有 V20260818_001；新增列 exam_venue 对应 BaseEntity 之外的业务字段

ALTER TABLE certification_exam_session
  ADD COLUMN exam_venue VARCHAR(200) NULL COMMENT '考场场地' AFTER exam_site_id;
