-- 修复 Flowable ACT_RE_PROCDEF 重复版本导致启动失败
-- 症状: Duplicate entry 'certificationDocumentReview-N-0-' for key 'ACT_UNIQ_PROCDEF'
--
-- 用法: 在 MySQL 客户端执行（会删除该流程所有历史部署，仅适用于开发/测试库）
-- USE gjjd_test;

-- 1) 查看当前版本
SELECT KEY_, VERSION_, ID_, DEPLOYMENT_ID_
FROM act_re_procdef
WHERE KEY_ = 'certificationDocumentReview'
ORDER BY VERSION_ DESC;

-- 2) 若存在重复或脏数据，清理后重启 platform-api（会自动按 BPMN 重新部署）
DELETE FROM act_ru_variable WHERE PROC_INST_ID_ IN (
  SELECT ID_ FROM act_ru_execution WHERE PROC_DEF_ID_ LIKE 'certificationDocumentReview:%'
);
DELETE FROM act_ru_task WHERE PROC_DEF_ID_ LIKE 'certificationDocumentReview:%';
DELETE FROM act_ru_execution WHERE PROC_DEF_ID_ LIKE 'certificationDocumentReview:%';
DELETE FROM act_ru_actinst WHERE PROC_DEF_ID_ LIKE 'certificationDocumentReview:%';

DELETE FROM act_ge_bytearray WHERE DEPLOYMENT_ID_ IN (
  SELECT ID_ FROM act_re_deployment WHERE KEY_ = 'certificationDocumentReview'
     OR NAME_ LIKE '%certificationDocumentReview%'
     OR NAME_ LIKE '%certification-document-review%'
);
DELETE FROM act_re_procdef WHERE KEY_ = 'certificationDocumentReview';
DELETE FROM act_re_deployment
WHERE KEY_ = 'certificationDocumentReview'
   OR NAME_ LIKE '%certificationDocumentReview%'
   OR NAME_ LIKE '%certification-document-review%';
