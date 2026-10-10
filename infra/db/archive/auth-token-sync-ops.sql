-- ============================================================================
-- 手机端调用 SaaS 鉴权方案 —— 运维一次性执行脚本
-- 对应文档：gjjd_appnext/docs/手机端调用SaaS鉴权方案.md v1.3 §3.5.3④ / §5 改造清单
-- 执行顺序：先在 PHP 库执行第 1 段，再在 SaaS 库执行第 2 段
-- 注意：若《AI同步学（学员复习）实现方案》同期实施，第 1 段授权清单应按其 §3.2
--       合并为完整清单（3 表 + 12 表 SELECT + exam_result 仅 INSERT）一次授完，避免二次变更
-- ============================================================================

-- ============================================================================
-- 第 1 段：PHP 库（gjjddb @ 114.132.67.185:3308）—— 建只读同步账号
-- 执行前替换：'<SaaS服务器IP>' 为 SaaS 服务器出口 IP；'<强密码>' 为随机强密码
-- ============================================================================
CREATE USER 'saas_sync'@'<SaaS服务器IP>' IDENTIFIED BY '<强密码>' REQUIRE SSL;
GRANT SELECT ON gjjddb.platform_user        TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.platform_user_token  TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.platform_user_detail TO 'saas_sync'@'<SaaS服务器IP>';
FLUSH PRIVILEGES;

-- 防火墙（firewalld 示例，仅对 SaaS 服务器 IP 放行 3308）：
-- firewall-cmd --permanent --new-zone=saas-sync
-- firewall-cmd --permanent --zone=saas-sync --add-source=<SaaS服务器IP>/32
-- firewall-cmd --permanent --zone=saas-sync --add-port=3308/tcp
-- firewall-cmd --reload

-- 验证：
-- SELECT user, host, ssl_type FROM mysql.user WHERE user = 'saas_sync';
-- SHOW GRANTS FOR 'saas_sync'@'<SaaS服务器IP>';

-- ============================================================================
-- 第 2 段：SaaS 库（gjjddb2026 @ 43.138.163.7:3308）—— id 区间隔离
-- 一次性执行：SaaS 本地新增行走 10 亿以上区间，PHP 同步来源数据在低位区间，
-- 防止同步任务 upsert 覆盖 SaaS 本地数据（鉴权方案 §3.5.3③）
-- ============================================================================
ALTER TABLE platform_user        AUTO_INCREMENT = 1000000000;
ALTER TABLE platform_user_detail AUTO_INCREMENT = 1000000000;
-- platform_user_token 表 SaaS 侧不写（token 只能由 PHP 签发/踢人删除），无需处理

-- 验证（Auto_increment 列应显示 >= 1000000000）：
-- SHOW TABLE STATUS LIKE 'platform_user';
-- SHOW TABLE STATUS LIKE 'platform_user_detail';
