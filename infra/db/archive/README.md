# 归档说明（2026-08-27 单库化改造）

以下脚本随「AI 同步学功能单库化改造」（docs/04_requirements/AI同步学功能单库化改造方案_gjjddbpro.md）停用归档：

- `auth-token-sync-ops.sql` — PHP→SaaS 三表镜像同步（integration/sync）的运维脚本；sync 通道代码已整包删除。
- `train-exam-list-import.sql` — train_exam_list 一次性 PHP→SaaS 导入脚本；合并库 gjjddbpro 已是同一张表，无需导入。

仅留档备查，勿在生产执行。
