-- ============================================================
-- 为系统管理模块的所有数据库表添加字段注释和表名称注释
-- ============================================================

-- ---- sys_tenant: 租户表 ----
ALTER TABLE sys_tenant COMMENT '租户表';
ALTER TABLE sys_tenant MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_tenant MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '租户编码，全局唯一';
ALTER TABLE sys_tenant MODIFY COLUMN name            VARCHAR(128) NOT NULL COMMENT '租户名称';
ALTER TABLE sys_tenant MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_tenant MODIFY COLUMN contact_name    VARCHAR(64)  NULL     COMMENT '联系人姓名';
ALTER TABLE sys_tenant MODIFY COLUMN contact_phone   VARCHAR(32)  NULL     COMMENT '联系人电话';
ALTER TABLE sys_tenant MODIFY COLUMN remark          VARCHAR(512) NULL     COMMENT '备注';
ALTER TABLE sys_tenant MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_tenant MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_tenant MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_org: 组织架构表 ----
ALTER TABLE sys_org COMMENT '组织架构表';
ALTER TABLE sys_org MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_org MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_org MODIFY COLUMN parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '上级组织ID，0表示顶级';
ALTER TABLE sys_org MODIFY COLUMN org_type        VARCHAR(32)  NOT NULL COMMENT '组织类型：company-公司, department-部门, team-小组';
ALTER TABLE sys_org MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '组织编码，租户内唯一';
ALTER TABLE sys_org MODIFY COLUMN name            VARCHAR(128) NOT NULL COMMENT '组织名称';
ALTER TABLE sys_org MODIFY COLUMN sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前';
ALTER TABLE sys_org MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_org MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_org MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_org MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_org MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_org MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_user: 用户表 ----
ALTER TABLE sys_user COMMENT '用户表';
ALTER TABLE sys_user MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_user MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_user MODIFY COLUMN org_id          BIGINT       NULL     COMMENT '所属组织ID';
ALTER TABLE sys_user MODIFY COLUMN post_id         BIGINT       NULL     COMMENT '所属岗位ID';
ALTER TABLE sys_user MODIFY COLUMN username        VARCHAR(64)  NOT NULL COMMENT '登录用户名，租户内唯一';
ALTER TABLE sys_user MODIFY COLUMN password_hash   VARCHAR(255) NOT NULL COMMENT '密码哈希值';
ALTER TABLE sys_user MODIFY COLUMN display_name    VARCHAR(64)  NOT NULL COMMENT '显示姓名';
ALTER TABLE sys_user MODIFY COLUMN user_type       VARCHAR(32)  NOT NULL COMMENT '用户类型：platform_admin-平台管理员, tenant_admin-租户管理员, normal-普通用户';
ALTER TABLE sys_user MODIFY COLUMN phone           VARCHAR(32)  NULL     COMMENT '手机号';
ALTER TABLE sys_user MODIFY COLUMN email           VARCHAR(128) NULL     COMMENT '邮箱';
ALTER TABLE sys_user MODIFY COLUMN avatar_file_id  BIGINT       NULL     COMMENT '头像文件ID';
ALTER TABLE sys_user MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：active-正常, locked-锁定, disabled-停用';
ALTER TABLE sys_user MODIFY COLUMN last_login_at   DATETIME     NULL     COMMENT '最后登录时间';
ALTER TABLE sys_user MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_user MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_user MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_user MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_user MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_role: 角色表 ----
ALTER TABLE sys_role COMMENT '角色表';
ALTER TABLE sys_role MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_role MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_role MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '角色编码，租户内唯一';
ALTER TABLE sys_role MODIFY COLUMN name            VARCHAR(64)  NOT NULL COMMENT '角色名称';
ALTER TABLE sys_role MODIFY COLUMN role_type       VARCHAR(32)  NOT NULL COMMENT '角色类型：platform-平台角色, tenant-租户角色, custom-自定义';
ALTER TABLE sys_role MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_role MODIFY COLUMN remark          VARCHAR(512) NULL     COMMENT '备注';
ALTER TABLE sys_role MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_role MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_role MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_role MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_role MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_menu: 菜单表 ----
ALTER TABLE sys_menu COMMENT '菜单表';
ALTER TABLE sys_menu MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_menu MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_menu MODIFY COLUMN parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '上级菜单ID，0表示顶级';
ALTER TABLE sys_menu MODIFY COLUMN menu_type       VARCHAR(32)  NOT NULL COMMENT '菜单类型：directory-目录, menu-菜单, button-按钮';
ALTER TABLE sys_menu MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '菜单编码，租户内唯一';
ALTER TABLE sys_menu MODIFY COLUMN name            VARCHAR(64)  NOT NULL COMMENT '菜单名称';
ALTER TABLE sys_menu MODIFY COLUMN path            VARCHAR(255) NULL     COMMENT '前端路由路径';
ALTER TABLE sys_menu MODIFY COLUMN component       VARCHAR(255) NULL     COMMENT '前端组件路径';
ALTER TABLE sys_menu MODIFY COLUMN permission_code VARCHAR(128) NULL     COMMENT '权限标识码，如 user:view';
ALTER TABLE sys_menu MODIFY COLUMN icon            VARCHAR(64)  NULL     COMMENT '菜单图标名称';
ALTER TABLE sys_menu MODIFY COLUMN sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前';
ALTER TABLE sys_menu MODIFY COLUMN visible         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：1-可见, 0-隐藏';
ALTER TABLE sys_menu MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_menu MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_menu MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_menu MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_menu MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_menu MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_permission: 权限表 ----
ALTER TABLE sys_permission COMMENT '权限表';
ALTER TABLE sys_permission MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_permission MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_permission MODIFY COLUMN menu_id         BIGINT       NULL     COMMENT '所属菜单ID';
ALTER TABLE sys_permission MODIFY COLUMN code            VARCHAR(128) NOT NULL COMMENT '权限编码，如 user:view';
ALTER TABLE sys_permission MODIFY COLUMN name            VARCHAR(64)  NOT NULL COMMENT '权限名称';
ALTER TABLE sys_permission MODIFY COLUMN action          VARCHAR(32)  NOT NULL COMMENT '操作类型：view-查看, create-创建, update-更新, delete-删除';
ALTER TABLE sys_permission MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_permission MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_permission MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_permission MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_permission MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_permission MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_user_role: 用户角色关联表 ----
ALTER TABLE sys_user_role COMMENT '用户角色关联表';
ALTER TABLE sys_user_role MODIFY COLUMN id              BIGINT   NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_user_role MODIFY COLUMN tenant_id       BIGINT   NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_user_role MODIFY COLUMN user_id         BIGINT   NOT NULL COMMENT '用户ID';
ALTER TABLE sys_user_role MODIFY COLUMN role_id         BIGINT   NOT NULL COMMENT '角色ID';
ALTER TABLE sys_user_role MODIFY COLUMN created_at      DATETIME NOT NULL COMMENT '创建时间';
ALTER TABLE sys_user_role MODIFY COLUMN updated_at      DATETIME NOT NULL COMMENT '更新时间';
ALTER TABLE sys_user_role MODIFY COLUMN created_by      BIGINT   NULL     COMMENT '创建人ID';
ALTER TABLE sys_user_role MODIFY COLUMN updated_by      BIGINT   NULL     COMMENT '更新人ID';
ALTER TABLE sys_user_role MODIFY COLUMN deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_role_menu: 角色菜单关联表 ----
ALTER TABLE sys_role_menu COMMENT '角色菜单关联表';
ALTER TABLE sys_role_menu MODIFY COLUMN id              BIGINT   NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_role_menu MODIFY COLUMN tenant_id       BIGINT   NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_role_menu MODIFY COLUMN role_id         BIGINT   NOT NULL COMMENT '角色ID';
ALTER TABLE sys_role_menu MODIFY COLUMN menu_id         BIGINT   NOT NULL COMMENT '菜单ID';
ALTER TABLE sys_role_menu MODIFY COLUMN created_at      DATETIME NOT NULL COMMENT '创建时间';
ALTER TABLE sys_role_menu MODIFY COLUMN updated_at      DATETIME NOT NULL COMMENT '更新时间';
ALTER TABLE sys_role_menu MODIFY COLUMN created_by      BIGINT   NULL     COMMENT '创建人ID';
ALTER TABLE sys_role_menu MODIFY COLUMN updated_by      BIGINT   NULL     COMMENT '更新人ID';
ALTER TABLE sys_role_menu MODIFY COLUMN deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_role_permission: 角色权限关联表 ----
ALTER TABLE sys_role_permission COMMENT '角色权限关联表';
ALTER TABLE sys_role_permission MODIFY COLUMN id              BIGINT   NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_role_permission MODIFY COLUMN tenant_id       BIGINT   NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_role_permission MODIFY COLUMN role_id         BIGINT   NOT NULL COMMENT '角色ID';
ALTER TABLE sys_role_permission MODIFY COLUMN permission_id   BIGINT   NOT NULL COMMENT '权限ID';
ALTER TABLE sys_role_permission MODIFY COLUMN created_at      DATETIME NOT NULL COMMENT '创建时间';
ALTER TABLE sys_role_permission MODIFY COLUMN updated_at      DATETIME NOT NULL COMMENT '更新时间';
ALTER TABLE sys_role_permission MODIFY COLUMN created_by      BIGINT   NULL     COMMENT '创建人ID';
ALTER TABLE sys_role_permission MODIFY COLUMN updated_by      BIGINT   NULL     COMMENT '更新人ID';
ALTER TABLE sys_role_permission MODIFY COLUMN deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_dict: 数据字典表 ----
ALTER TABLE sys_dict COMMENT '数据字典表';
ALTER TABLE sys_dict MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_dict MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_dict MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '字典编码，租户内唯一';
ALTER TABLE sys_dict MODIFY COLUMN name            VARCHAR(64)  NOT NULL COMMENT '字典名称';
ALTER TABLE sys_dict MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_dict MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_dict MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_dict MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_dict MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_dict MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_dict_item: 数据字典项表 ----
ALTER TABLE sys_dict_item COMMENT '数据字典项表';
ALTER TABLE sys_dict_item MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_dict_item MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_dict_item MODIFY COLUMN dict_id         BIGINT       NOT NULL COMMENT '所属字典ID';
ALTER TABLE sys_dict_item MODIFY COLUMN item_value      VARCHAR(64)  NOT NULL COMMENT '字典项值，如 male';
ALTER TABLE sys_dict_item MODIFY COLUMN item_label      VARCHAR(128) NOT NULL COMMENT '字典项标签，如 男';
ALTER TABLE sys_dict_item MODIFY COLUMN sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前';
ALTER TABLE sys_dict_item MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_dict_item MODIFY COLUMN created_at      DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_dict_item MODIFY COLUMN updated_at      DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_dict_item MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_dict_item MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_dict_item MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_post: 岗位表（修正英文注释为中文） ----
ALTER TABLE sys_post COMMENT '岗位表';
ALTER TABLE sys_post MODIFY COLUMN id              BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_post MODIFY COLUMN tenant_id       BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_post MODIFY COLUMN code            VARCHAR(64)  NOT NULL COMMENT '岗位编码，租户内唯一';
ALTER TABLE sys_post MODIFY COLUMN name            VARCHAR(128) NOT NULL COMMENT '岗位名称';
ALTER TABLE sys_post MODIFY COLUMN sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前';
ALTER TABLE sys_post MODIFY COLUMN status          VARCHAR(32)  NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled-启用, disabled-停用';
ALTER TABLE sys_post MODIFY COLUMN remark          VARCHAR(512) NULL     COMMENT '备注';
ALTER TABLE sys_post MODIFY COLUMN created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE sys_post MODIFY COLUMN updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE sys_post MODIFY COLUMN created_by      BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_post MODIFY COLUMN updated_by      BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_post MODIFY COLUMN deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_file: 文件表 ----
ALTER TABLE sys_file COMMENT '文件表';
ALTER TABLE sys_file MODIFY COLUMN id               BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_file MODIFY COLUMN tenant_id        BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_file MODIFY COLUMN storage_provider VARCHAR(32)  NOT NULL COMMENT '存储提供商：local-本地, minio, oss';
ALTER TABLE sys_file MODIFY COLUMN bucket           VARCHAR(128) NULL     COMMENT '存储桶名称';
ALTER TABLE sys_file MODIFY COLUMN object_key       VARCHAR(512) NOT NULL COMMENT '对象存储Key';
ALTER TABLE sys_file MODIFY COLUMN original_name    VARCHAR(255) NOT NULL COMMENT '原始文件名';
ALTER TABLE sys_file MODIFY COLUMN content_type     VARCHAR(128) NULL     COMMENT 'MIME类型';
ALTER TABLE sys_file MODIFY COLUMN size_bytes       BIGINT       NOT NULL COMMENT '文件大小（字节）';
ALTER TABLE sys_file MODIFY COLUMN sha256           VARCHAR(128) NULL     COMMENT '文件SHA256哈希值';
ALTER TABLE sys_file MODIFY COLUMN access_policy    VARCHAR(32)  NOT NULL DEFAULT 'private' COMMENT '访问策略：private-私有, public-公开';
ALTER TABLE sys_file MODIFY COLUMN status           VARCHAR(32)  NOT NULL DEFAULT 'uploaded' COMMENT '状态：uploaded-已上传';
ALTER TABLE sys_file MODIFY COLUMN created_at       DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_file MODIFY COLUMN updated_at       DATETIME     NOT NULL COMMENT '更新时间';
ALTER TABLE sys_file MODIFY COLUMN created_by       BIGINT       NULL     COMMENT '创建人ID';
ALTER TABLE sys_file MODIFY COLUMN updated_by       BIGINT       NULL     COMMENT '更新人ID';
ALTER TABLE sys_file MODIFY COLUMN deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';

-- ---- sys_audit_log: 审计日志表 ----
ALTER TABLE sys_audit_log COMMENT '审计日志表';
ALTER TABLE sys_audit_log MODIFY COLUMN id           BIGINT       NOT NULL COMMENT '主键ID（雪花ID）';
ALTER TABLE sys_audit_log MODIFY COLUMN tenant_id    BIGINT       NOT NULL COMMENT '所属租户ID';
ALTER TABLE sys_audit_log MODIFY COLUMN user_id      BIGINT       NULL     COMMENT '操作用户ID';
ALTER TABLE sys_audit_log MODIFY COLUMN module       VARCHAR(64)  NOT NULL COMMENT '操作模块，如 User, Role';
ALTER TABLE sys_audit_log MODIFY COLUMN action       VARCHAR(64)  NOT NULL COMMENT '操作类型：CREATE, UPDATE, DELETE, LOGIN, LOGOUT';
ALTER TABLE sys_audit_log MODIFY COLUMN target_type  VARCHAR(64)  NULL     COMMENT '目标对象类型，如 User, Role';
ALTER TABLE sys_audit_log MODIFY COLUMN target_id    BIGINT       NULL     COMMENT '目标对象ID';
ALTER TABLE sys_audit_log MODIFY COLUMN request_id   VARCHAR(128) NULL     COMMENT '请求追踪ID';
ALTER TABLE sys_audit_log MODIFY COLUMN ip           VARCHAR(64)  NULL     COMMENT '请求来源IP地址';
ALTER TABLE sys_audit_log MODIFY COLUMN user_agent   VARCHAR(512) NULL     COMMENT '请求User-Agent';
ALTER TABLE sys_audit_log MODIFY COLUMN detail_json  TEXT         NULL     COMMENT '操作详情JSON';
ALTER TABLE sys_audit_log MODIFY COLUMN created_at   DATETIME     NOT NULL COMMENT '创建时间';
ALTER TABLE sys_audit_log MODIFY COLUMN deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除';
