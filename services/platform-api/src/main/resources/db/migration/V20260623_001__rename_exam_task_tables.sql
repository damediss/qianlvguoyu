-- 题库开发/审核任务表：去掉 exam_ 前缀，与 gjjd_phalcon 共用同一表名
RENAME TABLE exam_task_exam TO task_exam;
RENAME TABLE exam_task_exam_detail TO task_exam_detail;
RENAME TABLE exam_task_exam_audit_records TO task_exam_audit_records;
RENAME TABLE exam_task_examshicao TO task_examshicao;
RENAME TABLE exam_task_examshicao_detail TO task_examshicao_detail;
RENAME TABLE exam_task_examshicao_audit_records TO task_examshicao_audit_records;
RENAME TABLE exam_task_examsub TO task_examsub;
RENAME TABLE exam_task_examsub_detail TO task_examsub_detail;
RENAME TABLE exam_task_examsub_audit_records TO task_examsub_audit_records;
RENAME TABLE exam_task_ximu TO task_ximu;
RENAME TABLE exam_task_ximu_detail TO task_ximu_detail;
RENAME TABLE exam_task_ximu_audit_records TO task_ximu_audit_records;
RENAME TABLE exam_task_expert TO task_expert;

-- 已废弃的旧表（被 task_expert 替代）
DROP TABLE IF EXISTS exam_task_ximu_expert;
