-- V20260804_004: platform_user_token 去除 token 唯一约束
-- 原因：PHP 源表 platform_user_token.token 无唯一约束，现网实测存在 85 组重复 token
-- （同秒同密码 sha1 相同，最多 5 行同 token）。镜像表若保留 UNIQUE：
--   1) ON DUPLICATE KEY UPDATE 会把同 token 的多行折叠成一行，镜像行数永远少于源库（1928→1727）；
--   2) 每周期在多个源行之间反复覆盖（thrashing），checkpoint 恒见 updated=201，无法收敛。
-- 处理：唯一索引降级为普通索引，镜像按主键 id 忠实复制；鉴权查询按 id 升序取首行
-- （与 PHP Phalcon findFirst 语义一致，见 AppTokenAuthFilter）。
ALTER TABLE platform_user_token DROP INDEX uk_platform_user_token, ADD INDEX idx_platform_user_token (token);
