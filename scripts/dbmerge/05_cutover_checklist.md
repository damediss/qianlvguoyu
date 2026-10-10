# 05 停机迁移窗口操作卡（gjjddb + gjjddb2026 → gjjddbpro）

> 逐条勾选。每步标注预期耗时（以预演实测为准回填 `[实测]`）。任何一步失败 → 停止并回退评估。

## 窗口前（T-24h 内完成）
- [ ] 预演报告（`outputs/dbmerge-rehearsal/`）已由 owner 签字 Go
- [ ] C 类 PHP 保障专项 6 条全部通过；MANUAL 表（cms_article*/train_dev_organization）裁决已落地
- [ ] 43 磁盘清理完毕（当前余量 12G，建议清 `/opt/guoyu-platform/deploy-backups` 旧包）`[实测]`
- [ ] 停机公告已发布；窗口时段已确定（业务低谷）
- [ ] 凭据就绪且为窗口临时密码；窗口后轮换

## 阶段 1：停机迁移（窗口内）
- [ ] 1.1 停 PHP 写流量（维护页/停写入口，与 owner 确认形式）`[实测]`
- [ ] 1.1 停 SaaS：`systemctl stop guoyu-platform-api`（43）`[实测]`
- [ ] 1.1 停 PHP CLI 常驻：`systemctl stop 'cli-*'`（114，8 个服务）`[实测]`
- [ ] 1.1 确认 sync 开关关闭（PLATFORM_SYNC_ENABLED=false / user-provisioning.php-write-enabled=false）
- [ ] 1.2 冻结确认：两库 `SHOW PROCESSLIST` 无写入；114 最终增量快照比对 `[实测]`
- [ ] 1.3 **窗口 Go 决策**（owner）——此刻原库未动一字节，可中止
- [ ] 1.4 执行 `03_migrate.sh`：`TARGET_DB=gjjddbpro GX_TARGET_DB=gxwebsoft_core bash 03_migrate.sh` `[实测]`
  - ① flyway ②SaaS ③PHP ④C 追加 ⑤B 并集 ⑥gx ⑦auto_increment ⑧tenant 回填
- [ ] 1.5 执行 `python 04_validate.py gjjddbpro`，出正式校验报告，与 owner 逐条核对 `[实测]`

## 阶段 2：切换与冒烟（窗口内）
- [ ] 2.1 PHP 改 `config.php` host→172.16.16.2（gjjddbpro + gxwebsoft_core）→ 重启 PHP-FPM → owner 冒烟（App 登录/后台登录 sys_qlgt/考试报名/导出异步任务/后台名单页）`[实测]`
- [ ] 2.2 SaaS 改 `config/platform-api.env`：`MYSQL_URL=.../gjjddbpro`、sync 双开关=false → 重启 `guoyu-platform-api`（先核对 `platform-api.jar` 软链指向目标版本）→ AI 跑 HTTP 冒烟（登录/权限/题库/考试）+ Flyway 校验 `[实测]`
- [ ] 2.3 **Go / No-Go**（owner）——No-Go 则双方配置切回原库（原库未动，秒级回滚）

## 阶段 3：观察期（1 周）
- [ ] 旧三库只读保留，不回写
- [ ] AI 每日只读对账简报（新库关键表行数增长、SaaS 日志、sync 无运行记录）
- [ ] SaaS 新 Flyway 迁移 DDL 显式 `utf8mb4_unicode_ci`
- [ ] owner 观察业务反馈，重点盯 PHP 端

## 阶段 4：归档与清理（观察期满后）
- [ ] 4.1 旧三库 mysqldump 全量归档（COS/离线介质）
- [ ] 4.2 **旧库 DROP——owner 亲自执行**
- [ ] 4.3 SaaS 移除 sync 通道代码 / PHP_MYSQL_* 配置 / user-provisioning 双写残留
- [ ] 4.4 更新 AGENTS.md / README / deploy 文档库名与配置说明

## 回滚预案
- 窗口内 No-Go：PHP/SaaS 配置切回原库即可（原库全程未动）。
- 观察期回滚：配置切回 + 窗口后新库增量补回旧库（补回脚本由 AI 编写）。
