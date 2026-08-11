-- AI 同步学灰度白名单（D8，方案 §15.2）
-- scope_type: exam=按考试/工种整批试点（scope_id = PHP 库 train_exam_list.id，复习/考试任务主键）；
--             user=按学员（scope_id = PHP 库 platform_user.id）
CREATE TABLE IF NOT EXISTS ai_review_gray_whitelist (
    id          BIGINT       NOT NULL COMMENT '雪花 ID',
    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户 ID',
    scope_type  VARCHAR(16)  NOT NULL COMMENT '范围类型：exam / user',
    scope_id    BIGINT       NOT NULL COMMENT '范围 ID：examId 或 platform_user.id',
    remark      VARCHAR(255) NULL COMMENT '备注（试点说明）',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_by  BIGINT       NULL COMMENT '创建人',
    updated_by  BIGINT       NULL COMMENT '更新人',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gray_scope (tenant_id, scope_type, scope_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 同步学灰度白名单（D8）';

-- 灰度总开关（D8 拍板：上线即灰度态，仅白名单可用；试点达标后改 'false' 全量）
INSERT INTO sys_dict (id, tenant_id, code, name, status, value, deleted, created_at, updated_at)
SELECT 20260810041, 1, 'ai_review_gray_enabled', 'AI同步学：灰度开关（true=仅白名单可用，false=全员开放）', 'enabled', 'true', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE tenant_id = 1 AND code = 'ai_review_gray_enabled' AND deleted = 0);

-- 超前学习每日上限（D7：每日最多提前解锁 1 天）
INSERT INTO sys_dict (id, tenant_id, code, name, status, value, deleted, created_at, updated_at)
SELECT 20260810042, 1, 'ai_review_advance_max_per_day', 'AI同步学：每日最多提前解锁天数', 'enabled', '1', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE tenant_id = 1 AND code = 'ai_review_advance_max_per_day' AND deleted = 0);
