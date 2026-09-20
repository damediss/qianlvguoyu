-- AI 小培会话表占位（本迭代仍用端侧续聊；下迭代接入服务端读写）
-- 表先建好，避免后续迁移冲突；应用层暂不强制写入。

CREATE TABLE IF NOT EXISTS train_class_assistant_session (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户',
  class_id        BIGINT       NOT NULL COMMENT '班级ID',
  platform_user_id BIGINT      NOT NULL COMMENT '学员平台用户ID',
  weknora_session_id VARCHAR(64) NULL COMMENT 'WeKnora会话ID',
  scene           VARCHAR(32)  NULL COMMENT '最近场景',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0关闭',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assistant_session_user_class (tenant_id, platform_user_id, class_id),
  KEY idx_assistant_session_weknora (weknora_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI小培会话（占位，待接入）';

CREATE TABLE IF NOT EXISTS train_class_assistant_message (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户',
  session_id      BIGINT       NOT NULL COMMENT '会话ID',
  role            VARCHAR(16)  NOT NULL COMMENT 'user/bot/system',
  content         MEDIUMTEXT   NULL COMMENT '文本内容',
  intent          VARCHAR(64)  NULL COMMENT '意图',
  payload_json    MEDIUMTEXT   NULL COMMENT '附加JSON',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_assistant_msg_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI小培消息（占位，待接入）';
