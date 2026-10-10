-- 修复遗留 PHP 导入表与 Flyway 新表的 collation 不一致。
-- train_people_list 已是 utf8mb4_unicode_ci，但 train_sign_up_* 可能仍为 utf8mb4_general_ci，
-- 导致跨表字符串比较（如 id_card_number = id_card）报 Error 1267。
-- 不使用 PREPARE，避免 Druid WallFilter 拦截导致 Flyway 启动失败。

ALTER TABLE train_sign_up_self CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE train_sign_up_people_self CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
