/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.1.200_3308
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : 192.168.1.200:3308
 Source Schema         : gjjd_test

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 19/05/2026 15:13:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_generation_draft
-- ----------------------------
DROP TABLE IF EXISTS `ai_generation_draft`;
CREATE TABLE `ai_generation_draft`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `draft_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_id` bigint NULL DEFAULT NULL,
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `result_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'generated',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_generation_draft_source`(`tenant_id` ASC, `source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_ai_generation_draft_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_approval_record
-- ----------------------------
DROP TABLE IF EXISTS `biz_approval_record`;
CREATE TABLE `biz_approval_record`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` bigint NOT NULL,
  `process_instance_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `task_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `operated_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz_approval_business`(`tenant_id` ASC, `business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_biz_approval_process`(`tenant_id` ASC, `process_instance_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `work_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `level_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cover_file_id` bigint NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `outline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `owner_user_id` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_course_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_course_owner`(`tenant_id` ASC, `owner_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_chapter
-- ----------------------------
DROP TABLE IF EXISTS `course_chapter`;
CREATE TABLE `course_chapter`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'text',
  `content_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `file_id` bigint NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_chapter_course`(`tenant_id` ASC, `course_id` ASC) USING BTREE,
  INDEX `idx_course_chapter_parent`(`tenant_id` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for course_kejian
-- ----------------------------
DROP TABLE IF EXISTS `course_kejian`;
CREATE TABLE `course_kejian`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sno` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `keshi` int NULL DEFAULT NULL COMMENT '课时',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面',
  `type` int NULL DEFAULT NULL COMMENT '类型',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  `desction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件路径',
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `tag` int NULL DEFAULT NULL COMMENT '标签',
  `jigou_id` bigint NULL DEFAULT NULL COMMENT '机构ID',
  `owner_id` bigint NULL DEFAULT NULL COMMENT '所有者ID',
  `addtime` bigint NULL DEFAULT NULL COMMENT '添加时间(时间戳)',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_kejian_work_type`(`work_type_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_course
-- ----------------------------
DROP TABLE IF EXISTS `exam_course`;
CREATE TABLE `exam_course`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '科目名称',
  `level` tinyint(1) NULL DEFAULT 0 COMMENT '0初级1中级2高级3技师4高级技师',
  `series_no_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '科目编码(编组)',
  `series_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '科目编码',
  `user_in_charge` int NULL DEFAULT NULL COMMENT '拥有者id',
  `user_in_charge_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拥有者名称',
  `last_level_get_year` int NULL DEFAULT 0 COMMENT '上级获得年份要求',
  `work_year_limit` int NULL DEFAULT NULL COMMENT '累计从事本职业或相关职业工作年',
  `min_level` int NULL DEFAULT NULL COMMENT '最低证书要求',
  `min_level_year` int NULL DEFAULT NULL COMMENT '最低证书获得年',
  `start_age` int NULL DEFAULT 0 COMMENT '开始年龄',
  `is_general` tinyint(1) NULL DEFAULT 0 COMMENT '是否通用资源',
  `exam_condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '版本',
  `comments` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` bigint NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `work_type_id`(`work_type_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 268 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '科目' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for exam_paper
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper`;
CREATE TABLE `exam_paper`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_score` int NOT NULL DEFAULT 100,
  `pass_score` int NOT NULL DEFAULT 60,
  `duration_minutes` int NOT NULL DEFAULT 60,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exam_paper_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_paper_question
-- ----------------------------
DROP TABLE IF EXISTS `exam_paper_question`;
CREATE TABLE `exam_paper_question`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `score` int NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_paper_question`(`tenant_id` ASC, `paper_id` ASC, `question_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_exam_paper_question_paper`(`tenant_id` ASC, `paper_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_result
-- ----------------------------
DROP TABLE IF EXISTS `exam_result`;
CREATE TABLE `exam_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型',
  `exam_plan_id` bigint NULL DEFAULT NULL COMMENT '考试计划ID',
  `paper_id` bigint NULL DEFAULT NULL COMMENT '试卷ID',
  `detail_id` bigint NULL DEFAULT NULL COMMENT '详情ID',
  `get_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '得分',
  `exam_type` int NULL DEFAULT NULL COMMENT '考试类型',
  `exam_id` bigint NULL DEFAULT NULL COMMENT '考试ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户答案',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exam_result_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_exam_result_exam`(`exam_id` ASC) USING BTREE,
  INDEX `idx_exam_result_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_session
-- ----------------------------
DROP TABLE IF EXISTS `exam_session`;
CREATE TABLE `exam_session`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `paper_id` bigint NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_at` datetime NOT NULL,
  `end_at` datetime NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `allow_retake` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exam_session_time`(`tenant_id` ASC, `start_at` ASC, `end_at` ASC) USING BTREE,
  INDEX `idx_exam_session_status`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_work_type
-- ----------------------------
DROP TABLE IF EXISTS `exam_work_type`;
CREATE TABLE `exam_work_type`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `series_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职业编码',
  `levels_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '级别',
  `parent_id` int NULL DEFAULT NULL COMMENT '上级ID',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NULL DEFAULT 0 COMMENT '1已删除，0正常',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号，降序排序',
  `tenant_id` bigint NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 137 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工种' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_chunk
-- ----------------------------
DROP TABLE IF EXISTS `kb_chunk`;
CREATE TABLE `kb_chunk`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `space_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `chunk_no` int NOT NULL,
  `heading` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `page_no` int NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_count` int NOT NULL DEFAULT 0,
  `embedding_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `vector_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `vector_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `metadata_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_chunk_no`(`tenant_id` ASC, `document_id` ASC, `chunk_no` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_kb_chunk_space`(`tenant_id` ASC, `space_id` ASC) USING BTREE,
  INDEX `idx_kb_chunk_document`(`tenant_id` ASC, `document_id` ASC) USING BTREE,
  FULLTEXT INDEX `ft_kb_chunk_content`(`content`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_document
-- ----------------------------
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `space_id` bigint NOT NULL,
  `file_id` bigint NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'file',
  `access_scope` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'inherit',
  `sensitive_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal',
  `parser_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `index_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `chunk_count` int NOT NULL DEFAULT 0,
  `text_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'indexing',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_document_space`(`tenant_id` ASC, `space_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_kb_document_file`(`tenant_id` ASC, `file_id` ASC) USING BTREE,
  INDEX `idx_kb_document_category`(`tenant_id` ASC, `category_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_document_acl
-- ----------------------------
DROP TABLE IF EXISTS `kb_document_acl`;
CREATE TABLE `kb_document_acl`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `principal_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `principal_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_document_acl`(`tenant_id` ASC, `document_id` ASC, `principal_type` ASC, `principal_value` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_kb_document_acl_document`(`tenant_id` ASC, `document_id` ASC) USING BTREE,
  INDEX `idx_kb_document_acl_principal`(`tenant_id` ASC, `principal_type` ASC, `principal_value` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_index_task
-- ----------------------------
DROP TABLE IF EXISTS `kb_index_task`;
CREATE TABLE `kb_index_task`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `space_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ingest',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `retry_count` int NOT NULL DEFAULT 0,
  `error_message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_index_task_document`(`tenant_id` ASC, `document_id` ASC) USING BTREE,
  INDEX `idx_kb_index_task_status`(`tenant_id` ASC, `status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_space
-- ----------------------------
DROP TABLE IF EXISTS `kb_space`;
CREATE TABLE `kb_space`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `scope_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_user_id` bigint NULL DEFAULT NULL,
  `category_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_space_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_kb_space_scope_status`(`tenant_id` ASC, `scope_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_kb_space_owner`(`tenant_id` ASC, `owner_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_space_role
-- ----------------------------
DROP TABLE IF EXISTS `kb_space_role`;
CREATE TABLE `kb_space_role`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `space_id` bigint NOT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_space_role`(`tenant_id` ASC, `space_id` ASC, `role_code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_kb_space_role_space`(`tenant_id` ASC, `space_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for platform_user
-- ----------------------------
DROP TABLE IF EXISTS `platform_user`;
CREATE TABLE `platform_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `realname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `idcard` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stat` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `addtime` int NOT NULL DEFAULT 0,
  `logintime` int NOT NULL DEFAULT 0,
  `org_id` int NOT NULL DEFAULT 0 COMMENT '所属组织机构id',
  `company_id` int NOT NULL DEFAULT 0 COMMENT '所属企业id',
  `wxopenid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
  `expert_id` int NOT NULL DEFAULT 0 COMMENT '大于0表示platfrom_account表的专家id',
  `task` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4080 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '平台用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `question_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `difficulty` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `work_type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `level_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `knowledge_point_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_filter`(`tenant_id` ASC, `question_type` ASC, `work_type_code` ASC, `level_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_option
-- ----------------------------
DROP TABLE IF EXISTS `question_option`;
CREATE TABLE `question_option`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `option_label` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_correct` tinyint NOT NULL DEFAULT 0,
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_option_question`(`tenant_id` ASC, `question_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_answer
-- ----------------------------
DROP TABLE IF EXISTS `student_answer`;
CREATE TABLE `student_answer`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `attempt_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `answer_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `is_correct` tinyint NULL DEFAULT NULL,
  `score` int NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_answer_question`(`tenant_id` ASC, `attempt_id` ASC, `question_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_student_answer_attempt`(`tenant_id` ASC, `attempt_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_exam_attempt
-- ----------------------------
DROP TABLE IF EXISTS `student_exam_attempt`;
CREATE TABLE `student_exam_attempt`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `exam_session_id` bigint NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'started',
  `started_at` datetime NOT NULL,
  `submitted_at` datetime NULL DEFAULT NULL,
  `score` int NULL DEFAULT NULL,
  `passed` tinyint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_exam_attempt_user`(`tenant_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_student_exam_attempt_session`(`tenant_id` ASC, `exam_session_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_learning_record
-- ----------------------------
DROP TABLE IF EXISTS `student_learning_record`;
CREATE TABLE `student_learning_record`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `progress_percent` int NOT NULL DEFAULT 0,
  `completed` tinyint NOT NULL DEFAULT 0,
  `completed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_learning_record`(`tenant_id` ASC, `user_id` ASC, `chapter_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_student_learning_course`(`tenant_id` ASC, `user_id` ASC, `course_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_wrong_question
-- ----------------------------
DROP TABLE IF EXISTS `student_wrong_question`;
CREATE TABLE `student_wrong_question`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `source_attempt_id` bigint NULL DEFAULT NULL,
  `mastered` tinyint NOT NULL DEFAULT 0,
  `last_wrong_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_wrong_question`(`tenant_id` ASC, `user_id` ASC, `question_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_student_wrong_user`(`tenant_id` ASC, `user_id` ASC, `mastered` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_id` bigint NULL DEFAULT NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `detail_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_audit_log_tenant_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_sys_audit_log_target`(`tenant_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_dict_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `dict_id` bigint NOT NULL,
  `item_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_label` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_dict_item_value`(`tenant_id` ASC, `dict_id` ASC, `item_value` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_dict_item_dict`(`tenant_id` ASC, `dict_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `storage_provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `bucket` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `object_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `size_bytes` bigint NOT NULL,
  `sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `access_policy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'private',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'uploaded',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_file_tenant_status`(`tenant_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_sys_file_created_at`(`tenant_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `menu_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `permission_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `visible` tinyint NOT NULL DEFAULT 1,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_menu_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_menu_parent`(`tenant_id` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_org
-- ----------------------------
DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT 0,
  `org_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_org_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_org_parent`(`tenant_id` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `menu_id` bigint NULL DEFAULT NULL,
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_permission_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_permission_menu`(`tenant_id` ASC, `menu_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_tenant_code`(`tenant_id` ASC, `code` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_menu`(`tenant_id` ASC, `role_id` ASC, `menu_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_role_menu_role`(`tenant_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_permission`(`tenant_id` ASC, `role_id` ASC, `permission_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_role_permission_role`(`tenant_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_tenant_code`(`code` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `org_id` bigint NULL DEFAULT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar_file_id` bigint NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled',
  `last_login_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_tenant_username`(`tenant_id` ASC, `username` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_user_org`(`tenant_id` ASC, `org_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `updated_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_role`(`tenant_id` ASC, `user_id` ASC, `role_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_sys_user_role_user`(`tenant_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_sys_user_role_role`(`tenant_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_ability_comment_config
-- ----------------------------
DROP TABLE IF EXISTS `train_ability_comment_config`;
CREATE TABLE `train_ability_comment_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `score` tinyint NULL DEFAULT NULL COMMENT '分数',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '能力项评分配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_ability_config
-- ----------------------------
DROP TABLE IF EXISTS `train_ability_config`;
CREATE TABLE `train_ability_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NOT NULL,
  `parent_id` int NULL DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `standard` tinyint NULL DEFAULT NULL COMMENT '标准',
  `user_id` int NULL DEFAULT NULL,
  `level` int NOT NULL DEFAULT 0 COMMENT '状态',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`, `train_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '能力项配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_archive
-- ----------------------------
DROP TABLE IF EXISTS `train_archive`;
CREATE TABLE `train_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `archive_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '档案类型',
  `file_count` int NULL DEFAULT 0 COMMENT '文件数',
  `archive_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归档时间',
  `file_paths` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件路径JSON',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批时间',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_archive_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_archive_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_attendance
-- ----------------------------
DROP TABLE IF EXISTS `train_attendance`;
CREATE TABLE `train_attendance`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `attendance_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考勤日期',
  `status` int NULL DEFAULT 0 COMMENT '状态 0-缺勤 1-出勤 2-迟到 3-早退',
  `check_in_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签到时间',
  `check_out_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签退时间',
  `photo_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '照片路径',
  `video_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '视频路径',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_attendance_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_attendance_class`(`class_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_base_data
-- ----------------------------
DROP TABLE IF EXISTS `train_base_data`;
CREATE TABLE `train_base_data`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `tag` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签',
  `sno` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 118 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训导入表基础数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_brushing
-- ----------------------------
DROP TABLE IF EXISTS `train_brushing`;
CREATE TABLE `train_brushing`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL,
  `knowledge_id` int NULL DEFAULT NULL,
  `score` decimal(10, 0) NULL DEFAULT 0,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '刷题配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_budget
-- ----------------------------
DROP TABLE IF EXISTS `train_budget`;
CREATE TABLE `train_budget`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '市',
  `region` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '区',
  `org_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业名称',
  `train_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训时间',
  `train_work_type_id` bigint NULL DEFAULT NULL COMMENT '培训工种ID',
  `train_work_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训工种',
  `train_level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训级别',
  `bonus_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '补贴标准',
  `expect_income` decimal(16, 2) NULL DEFAULT NULL COMMENT '预计收入',
  `actual_income` decimal(16, 2) NULL DEFAULT NULL COMMENT '实际收入',
  `school_manage_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '学校管理费',
  `manage_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '公司运营管理费',
  `research_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '研发费用',
  `company_require_commission` decimal(16, 2) NULL DEFAULT NULL COMMENT '企业要求分成',
  `agent_require_commission` decimal(16, 2) NULL DEFAULT NULL COMMENT '中介要求分成',
  `teacher_course_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '培训讲师课酬',
  `teacher_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '培训讲师食宿差旅费',
  `staff_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '工作人员劳务费用',
  `staff_travel_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '工作人员食宿差旅费',
  `train_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '培训耗材费',
  `point_rewards` decimal(16, 2) NULL DEFAULT NULL COMMENT '积分奖励',
  `business_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '业务招待费',
  `people_num` int NULL DEFAULT NULL COMMENT '认定人数',
  `charge_fee` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收费标准',
  `service_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '行业服务费',
  `review_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '评审费用',
  `maintain_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '认定金额',
  `makeup_people_num` int NULL DEFAULT NULL COMMENT '补考人数',
  `theory_makeup_people_num` int NULL DEFAULT NULL COMMENT '理论补考',
  `operation_makeup_people_num` int NULL DEFAULT NULL COMMENT '实操补考',
  `staff_makeup_train_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '工作人员差旅费',
  `review_makeup_train_fee` decimal(16, 2) NULL DEFAULT NULL COMMENT '考评员差旅费',
  `profit` decimal(16, 2) NULL DEFAULT NULL COMMENT '项目利润',
  `comments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `status` int NULL DEFAULT 0 COMMENT '状态 0-正常 1-冻结',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_budget_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_budget_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_certificate
-- ----------------------------
DROP TABLE IF EXISTS `train_certificate`;
CREATE TABLE `train_certificate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `certificate_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证书编号',
  `certificate_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证书类型',
  `work_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工种',
  `level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级',
  `issue_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发证日期',
  `expire_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '过期日期',
  `issuing_authority` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发证机构',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件路径',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_certificate_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_certificate_project`(`project_id` ASC) USING BTREE,
  INDEX `idx_train_certificate_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_class_list
-- ----------------------------
DROP TABLE IF EXISTS `train_class_list`;
CREATE TABLE `train_class_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `is_leader` int NULL DEFAULT 0 COMMENT '是否组长 0-否 1-是',
  `is_committee` int NULL DEFAULT 0 COMMENT '是否班委 0-否 1-是',
  `class_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班级名称',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `status` int NULL DEFAULT 0 COMMENT '状态 0-未开班 1-开班 2-结课',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_class_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_class_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_class_rule
-- ----------------------------
DROP TABLE IF EXISTS `train_class_rule`;
CREATE TABLE `train_class_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `rule_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则类型',
  `rule_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '规则配置JSON',
  `max_students` int NULL DEFAULT NULL COMMENT '最大学生数',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_class_rule_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_course
-- ----------------------------
DROP TABLE IF EXISTS `train_course`;
CREATE TABLE `train_course`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `teacher_id_card` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教师身份证',
  `teacher_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教师姓名',
  `teacher_user_id` bigint NULL DEFAULT NULL COMMENT '教师用户ID',
  `type` int NULL DEFAULT NULL COMMENT '类型',
  `course_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程日期',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程标题',
  `start_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结束时间',
  `theory_time` int NULL DEFAULT NULL COMMENT '理论学时',
  `operation_time` int NULL DEFAULT NULL COMMENT '实操学时',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_course_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_course_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_declare
-- ----------------------------
DROP TABLE IF EXISTS `train_declare`;
CREATE TABLE `train_declare`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `city_id` bigint NULL DEFAULT NULL COMMENT '城市ID',
  `city_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '城市名称',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_declare_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_declare_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_declare_fields
-- ----------------------------
DROP TABLE IF EXISTS `train_declare_fields`;
CREATE TABLE `train_declare_fields`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `declare_id` int NULL DEFAULT NULL,
  `field_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 146 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_declare_user_data
-- ----------------------------
DROP TABLE IF EXISTS `train_declare_user_data`;
CREATE TABLE `train_declare_user_data`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `declare_id` int NULL DEFAULT NULL,
  `field_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `field_data` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户申报数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_department_confirm
-- ----------------------------
DROP TABLE IF EXISTS `train_department_confirm`;
CREATE TABLE `train_department_confirm`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_info_id` int NULL DEFAULT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `organization_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门',
  `user_id` int NULL DEFAULT NULL,
  `feedback` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '反馈',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 163 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_dev_organization
-- ----------------------------
DROP TABLE IF EXISTS `train_dev_organization`;
CREATE TABLE `train_dev_organization`  (
  `organization_id` int NOT NULL AUTO_INCREMENT COMMENT '机构id',
  `parent_id` int NOT NULL DEFAULT 0 COMMENT '上级id, 0是顶级',
  `organization_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '机构名称',
  `organization_full_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '机构全称',
  `organization_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '机构代码',
  `organization_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '机构类型',
  `province` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '所在省份',
  `city` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '所在城市',
  `region` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '所在辖区',
  `zip_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮政编码',
  `park` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '所属园区',
  `phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '企业邮箱',
  `establish_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成立日期',
  `registered_capital` decimal(10, 2) UNSIGNED NULL DEFAULT 0.00 COMMENT '注册资本',
  `business` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '经营范围',
  `business_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '经营状态',
  `insureds` int NULL DEFAULT NULL COMMENT '参保人数',
  `industry` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '所属行业',
  `estate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属产业',
  `image` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '园区图片',
  `about` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '园区介绍',
  `is_coop` tinyint(1) NULL DEFAULT 0 COMMENT '是否合作单位',
  `leader_id` int NULL DEFAULT NULL COMMENT '负责人id',
  `legal_person` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '法定代表人',
  `set_up_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '成立日期',
  `tag` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业标签',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `comments` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`organization_id`) USING BTREE,
  INDEX `leader_id`(`leader_id` ASC) USING BTREE,
  INDEX `tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 211 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '开发单位' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_list
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_list`;
CREATE TABLE `train_exam_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级',
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `org_id` bigint NULL DEFAULT NULL COMMENT '组织ID',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组织名称',
  `type` int NULL DEFAULT NULL COMMENT '类型',
  `source` int NULL DEFAULT NULL COMMENT '来源',
  `invigilate_user_id` bigint NULL DEFAULT NULL COMMENT '监考人ID',
  `invigilate_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '监考人姓名',
  `evaluate_user_id` bigint NULL DEFAULT NULL COMMENT '评价人ID',
  `evaluate_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '评价人姓名',
  `watcher_user_id` bigint NULL DEFAULT NULL COMMENT '观察员ID',
  `watcher_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观察员姓名',
  `staff_user_id` bigint NULL DEFAULT NULL COMMENT '工作人员ID',
  `staff_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作人员姓名',
  `proposition_user_id` bigint NULL DEFAULT NULL COMMENT '命题人ID',
  `proposition_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '命题人姓名',
  `marking_user_id` bigint NULL DEFAULT NULL COMMENT '阅卷人ID',
  `marking_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '阅卷人姓名',
  `exam_id` bigint NULL DEFAULT NULL COMMENT '考试ID',
  `exam_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考试日期',
  `exam_date_mode` int NULL DEFAULT NULL COMMENT '考试日期模式',
  `exam_date_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '考试日期列表',
  `exam_time` int NULL DEFAULT NULL COMMENT '考试时长(分钟)',
  `need_finish_adventure` int NULL DEFAULT 0 COMMENT '需完成冒险',
  `need_finish_normal` int NULL DEFAULT 0 COMMENT '需完成普通',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `jigou_id` bigint NULL DEFAULT NULL COMMENT '机构ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_exam_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_exam_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_exam_paper
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_paper`;
CREATE TABLE `train_exam_paper`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `exam_room_id` int NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL,
  `paper_id` int NULL DEFAULT NULL,
  `operate_paper_id` int NULL DEFAULT NULL COMMENT '实操试卷',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '试卷派发' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_people_in_room
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_people_in_room`;
CREATE TABLE `train_exam_people_in_room`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL COMMENT '工种id',
  `session_id` int NULL DEFAULT NULL COMMENT '场次id',
  `room_id` int NULL DEFAULT NULL COMMENT '考场id',
  `exam_room_id` int NULL DEFAULT NULL COMMENT '考试考场id',
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `realname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `id_card_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考场人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_people_list
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_people_list`;
CREATE TABLE `train_exam_people_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL COMMENT '考试计划id',
  `train_id` int NULL DEFAULT NULL,
  `work_type_id` int NULL DEFAULT NULL COMMENT '考试工种',
  `work_type_level` tinyint(1) NULL DEFAULT 0 COMMENT '等级',
  `user_id` int NULL DEFAULT NULL,
  `id_card_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别',
  `realname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文化程度',
  `result_file_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '考卷',
  `org_id` int NULL DEFAULT NULL,
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL COMMENT '考场id',
  `score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '分数',
  `normal_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '平时分',
  `origin_normal_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '原平时分',
  `operate_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实操分数',
  `real_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '实操分数',
  `total_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '总分',
  `has_set_real_score` tinyint(1) NULL DEFAULT 0 COMMENT '是否已手动设置真实分数',
  `high_level_status` tinyint(1) NULL DEFAULT 0 COMMENT '技师、高级技师论文答辩,专家评审结果',
  `result_paper` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作结果证明',
  `in_white_list` tinyint(1) NULL DEFAULT 0 COMMENT '白名单内，不用先学游戏学',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nation_paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `learn_time` int NULL DEFAULT 0 COMMENT '学习次数',
  `learn_use_time` int NULL DEFAULT 0 COMMENT '学习时间',
  `platform_user_id` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `exam_id`(`exam_id` ASC) USING BTREE,
  INDEX `train_id`(`train_id` ASC) USING BTREE,
  INDEX `work_type_id`(`work_type_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `id_card_num`(`id_card_num` ASC) USING BTREE,
  INDEX `phone`(`phone` ASC) USING BTREE,
  INDEX `room_id`(`room_id` ASC) USING BTREE,
  INDEX `deleted`(`deleted` ASC) USING BTREE,
  INDEX `sort_number`(`sort_number` ASC) USING BTREE,
  INDEX `tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15301 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_review
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_review`;
CREATE TABLE `train_exam_review`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `paper_id` int NULL DEFAULT NULL,
  `num` int NULL DEFAULT NULL,
  `comments` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `jigou_id` int NOT NULL DEFAULT 0 COMMENT '机构id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 224 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试复习' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_room_list
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_room_list`;
CREATE TABLE `train_exam_room_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL COMMENT '工种id',
  `session_id` int NULL DEFAULT NULL COMMENT '场次id',
  `room_id` int NULL DEFAULT NULL COMMENT '考场id',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_session
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_session`;
CREATE TABLE `train_exam_session`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL,
  `start_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `end_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `course_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '考试科目',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '场次' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_shijuan
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_shijuan`;
CREATE TABLE `train_exam_shijuan`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NOT NULL DEFAULT 0 COMMENT 'train_exam_list表id',
  `paper_id` int NOT NULL DEFAULT 0 COMMENT 'exam_paper表id',
  `plan_type` int NOT NULL DEFAULT 0 COMMENT '计划类型:0考试,1复习',
  `addtime` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试计划、复习计划绑定试卷表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_staff
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_staff`;
CREATE TABLE `train_exam_staff`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `exam_room_id` int NULL DEFAULT NULL,
  `room_id` int NULL DEFAULT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人员类型',
  `realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 127 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考务人员' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_staff_config
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_staff_config`;
CREATE TABLE `train_exam_staff_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人员类型',
  `realname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考务人员' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_exam_work_type_list
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_work_type_list`;
CREATE TABLE `train_exam_work_type_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `work_type_id` int NULL DEFAULT NULL,
  `level` tinyint(1) NULL DEFAULT 0,
  `course_id` int NULL DEFAULT NULL COMMENT '科目',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 946 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试工种' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_file_detail
-- ----------------------------
DROP TABLE IF EXISTS `train_file_detail`;
CREATE TABLE `train_file_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_id` bigint NULL DEFAULT NULL COMMENT '文件主表ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_file_detail_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_file_detail_file`(`file_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_file_list
-- ----------------------------
DROP TABLE IF EXISTS `train_file_list`;
CREATE TABLE `train_file_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联表名',
  `pk` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联主键',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_file_list_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_group_ability
-- ----------------------------
DROP TABLE IF EXISTS `train_group_ability`;
CREATE TABLE `train_group_ability`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL COMMENT '科目',
  `knowledge_id` int NULL DEFAULT NULL,
  `result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `config_id` int NULL DEFAULT NULL COMMENT '标题',
  `score` tinyint NULL DEFAULT NULL COMMENT '得分',
  `current_score` tinyint NULL DEFAULT NULL COMMENT '当前',
  `user_id` int NULL DEFAULT NULL,
  `comments` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 110 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能矩阵' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_group_ability_task
-- ----------------------------
DROP TABLE IF EXISTS `train_group_ability_task`;
CREATE TABLE `train_group_ability_task`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `ability_id` int NULL DEFAULT NULL,
  `task_num` int NULL DEFAULT 0,
  `done_num` int NULL DEFAULT 0,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '科目',
  `point` decimal(10, 3) NULL DEFAULT 0.000,
  `get_point` decimal(10, 3) NULL DEFAULT 0.000,
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能矩阵' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_group_album
-- ----------------------------
DROP TABLE IF EXISTS `train_group_album`;
CREATE TABLE `train_group_album`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `comments` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态, 0正常, 1冻结',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_group_knowledge
-- ----------------------------
DROP TABLE IF EXISTS `train_group_knowledge`;
CREATE TABLE `train_group_knowledge`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `intro` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `get_point` int NULL DEFAULT 0,
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `comments` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态,',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `check_status` tinyint(1) NULL DEFAULT 0 COMMENT '0待审核1通过2不通过',
  `check_user_id` int NULL DEFAULT NULL COMMENT '审题用户',
  `check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审题用户',
  `check_time` timestamp NULL DEFAULT NULL COMMENT '审题时间',
  `final_check_status` tinyint(1) NULL DEFAULT 0 COMMENT '终审 0待审核1通过2不通过',
  `final_check_time` timestamp NULL DEFAULT NULL COMMENT '终审时间',
  `final_check_user_id` int NULL DEFAULT NULL COMMENT '终审用户',
  `final_check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审题用户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_group_point_log
-- ----------------------------
DROP TABLE IF EXISTS `train_group_point_log`;
CREATE TABLE `train_group_point_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `point` int NULL DEFAULT NULL,
  `scene` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `comments` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态,',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_group_sign
-- ----------------------------
DROP TABLE IF EXISTS `train_group_sign`;
CREATE TABLE `train_group_sign`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sign_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '团队签到' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_info_class
-- ----------------------------
DROP TABLE IF EXISTS `train_info_class`;
CREATE TABLE `train_info_class`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_info_id` bigint NULL DEFAULT NULL COMMENT '培训信息ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `nums` int NULL DEFAULT NULL COMMENT '人数',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `date_start` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开始日期',
  `date_end` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结束日期',
  `addtime` int NULL DEFAULT NULL COMMENT '添加时间(时间戳)',
  `file_temps` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件模板JSON',
  `kcbiao_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程表文件',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_info_class_train`(`train_info_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_info_list
-- ----------------------------
DROP TABLE IF EXISTS `train_info_list`;
CREATE TABLE `train_info_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '简介',
  `group_type` int NULL DEFAULT NULL COMMENT '组类型',
  `master_type` int NULL DEFAULT NULL COMMENT '主类型',
  `type` int NULL DEFAULT NULL COMMENT '类型',
  `cate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业名称',
  `company_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业地址',
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `work_type_level` int NULL DEFAULT NULL COMMENT '工种等级',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `num` int NULL DEFAULT NULL COMMENT '人数',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '岗位',
  `course_num` int NULL DEFAULT NULL COMMENT '课程数',
  `head_confirm` int NULL DEFAULT NULL COMMENT '负责人确认',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `company_has_confirm` int NULL DEFAULT NULL COMMENT '企业已确认',
  `company_confirm_time` datetime NULL DEFAULT NULL COMMENT '企业确认时间',
  `company_confirm_user_id` bigint NULL DEFAULT NULL COMMENT '企业确认人ID',
  `company_confirm_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业确认人姓名',
  `pass_status` int NULL DEFAULT NULL COMMENT '通过状态',
  `check_user_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核人姓名',
  `is_sorted_learn` int NULL DEFAULT NULL COMMENT '是否排序学习',
  `max_normal_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '最高平时成绩',
  `course_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程文件',
  `start_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结束日期',
  `train_company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训企业名称',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `point` int NULL DEFAULT NULL COMMENT '积分',
  `album` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相册',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '总结',
  `province_id` bigint NULL DEFAULT NULL COMMENT '省ID',
  `province_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '省名称',
  `city_id` bigint NULL DEFAULT NULL COMMENT '市ID',
  `city_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '市名称',
  `ability_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '能力分',
  `region_id` bigint NULL DEFAULT NULL COMMENT '区ID',
  `region_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '区名称',
  `in_charge_user_id` bigint NULL DEFAULT NULL COMMENT '负责人ID',
  `in_charge_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '负责人姓名',
  `files` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件列表JSON',
  `traindemand_id` bigint NULL DEFAULT NULL COMMENT '培训需求ID',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_info_list_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_info_list_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_need
-- ----------------------------
DROP TABLE IF EXISTS `train_need`;
CREATE TABLE `train_need`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NULL DEFAULT NULL COMMENT '组织ID',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组织名称',
  `train_object` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训对象',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '岗位',
  `num` int NULL DEFAULT NULL COMMENT '人数',
  `work_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工种',
  `level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级',
  `target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训目标',
  `method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训方式',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训地点',
  `train_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训日期',
  `budget` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预算',
  `teacher_require` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '师资要求',
  `need_exam` int NULL DEFAULT 0 COMMENT '是否需要考试 0-否 1-是',
  `need_match` int NULL DEFAULT 0 COMMENT '是否需要匹配 0-否 1-是',
  `need_paper` int NULL DEFAULT 0 COMMENT '是否需要试卷 0-否 1-是',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `json_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展JSON数据',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_need_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_need_org`(`org_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_need_chat
-- ----------------------------
DROP TABLE IF EXISTS `train_need_chat`;
CREATE TABLE `train_need_chat`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `need_id` int NULL DEFAULT NULL,
  `from_user_id` int NULL DEFAULT NULL,
  `to_user_id` int NULL DEFAULT NULL,
  `sort_number` int UNSIGNED NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `deleted` int NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `from_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_normal_score_log
-- ----------------------------
DROP TABLE IF EXISTS `train_normal_score_log`;
CREATE TABLE `train_normal_score_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NULL DEFAULT NULL,
  `knowledge_id` int NULL DEFAULT NULL,
  `train_id` int NULL DEFAULT NULL,
  `train_task_list_id` int NULL DEFAULT NULL,
  `score` decimal(10, 2) NULL DEFAULT 0.00,
  `scene` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '平时分记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_offline_homework
-- ----------------------------
DROP TABLE IF EXISTS `train_offline_homework`;
CREATE TABLE `train_offline_homework`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训线下作业' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_offline_homework_report
-- ----------------------------
DROP TABLE IF EXISTS `train_offline_homework_report`;
CREATE TABLE `train_offline_homework_report`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `offline_id` int NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训线下作业详情' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_paper_config
-- ----------------------------
DROP TABLE IF EXISTS `train_paper_config`;
CREATE TABLE `train_paper_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `template_id` int NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `x` int NULL DEFAULT NULL,
  `y` int NULL DEFAULT NULL,
  `font_size` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_paper_template
-- ----------------------------
DROP TABLE IF EXISTS `train_paper_template`;
CREATE TABLE `train_paper_template`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stamp_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `width` int NULL DEFAULT NULL,
  `height` int NULL DEFAULT NULL,
  `stamp_width` int NULL DEFAULT NULL,
  `stamp_height` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '证书模板' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_people_list
-- ----------------------------
DROP TABLE IF EXISTS `train_people_list`;
CREATE TABLE `train_people_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `train_class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `realname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `is_leader` int NULL DEFAULT 0 COMMENT '是否组长 0-否 1-是',
  `is_committee` int NULL DEFAULT 0 COMMENT '是否班委 0-否 1-是',
  `exam_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '考试成绩',
  `real_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '实操成绩',
  `paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证书',
  `normal_score` decimal(10, 2) NULL DEFAULT NULL COMMENT '平时成绩',
  `national_paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '国家证书',
  `current_level` int NULL DEFAULT NULL COMMENT '当前等级',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `id_card` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_people_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_people_train`(`train_id` ASC) USING BTREE,
  INDEX `idx_train_people_class`(`train_class_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_point_config
-- ----------------------------
DROP TABLE IF EXISTS `train_point_config`;
CREATE TABLE `train_point_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `value` int NULL DEFAULT 0,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_position_to_work_type
-- ----------------------------
DROP TABLE IF EXISTS `train_position_to_work_type`;
CREATE TABLE `train_position_to_work_type`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位',
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工种—岗位对应表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_project
-- ----------------------------
DROP TABLE IF EXISTS `train_project`;
CREATE TABLE `train_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项目名称',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业名称',
  `manager_id` bigint NULL DEFAULT NULL COMMENT '负责人ID',
  `manager_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '负责人姓名',
  `plan_start` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计划开始',
  `plan_end` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计划结束',
  `current_stage` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前阶段',
  `progress` int NULL DEFAULT 0 COMMENT '进度',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_project_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_project_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_project_table
-- ----------------------------
DROP TABLE IF EXISTS `train_project_table`;
CREATE TABLE `train_project_table`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `table_type` int NULL DEFAULT NULL COMMENT '表类型',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '表名称',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `version` int NULL DEFAULT 1 COMMENT '版本',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `file_id` bigint NULL DEFAULT NULL COMMENT '文件ID',
  `owner_id` bigint NULL DEFAULT NULL COMMENT '负责人ID',
  `owner_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '负责人姓名',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_project_table_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_project_table_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_protocol
-- ----------------------------
DROP TABLE IF EXISTS `train_protocol`;
CREATE TABLE `train_protocol`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `version` int NULL DEFAULT 1 COMMENT '版本',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `signed_at` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '签署时间',
  `signer_id` bigint NULL DEFAULT NULL COMMENT '签署人ID',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_protocol_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_protocol_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_recruit
-- ----------------------------
DROP TABLE IF EXISTS `train_recruit`;
CREATE TABLE `train_recruit`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `company_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位',
  `num` int NULL DEFAULT NULL COMMENT '招聘人数',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '招聘要求',
  `expired_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '过期日期',
  `post_nature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位性质',
  `trial_offer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '试用期待遇',
  `official_offer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '正式员工待遇',
  `offer_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '待遇说明',
  `other_benefits` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '其他福利',
  `work_address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作地点',
  `contact` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `recruit_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '招聘方式',
  `people_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人才类型',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '招聘需求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_request
-- ----------------------------
DROP TABLE IF EXISTS `train_request`;
CREATE TABLE `train_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业名称',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '岗位',
  `num` int NULL DEFAULT NULL COMMENT '人数',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `train_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '培训日期',
  `contact` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `is_on_position` int NULL DEFAULT 0 COMMENT '是否在岗 0-否 1-是',
  `student_form` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学员形式',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_request_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_research
-- ----------------------------
DROP TABLE IF EXISTS `train_research`;
CREATE TABLE `train_research`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `research_org_id` int NULL DEFAULT NULL COMMENT '调研机构',
  `research_site_id` int NULL DEFAULT NULL COMMENT '调研站点',
  `org_id` int NULL DEFAULT NULL COMMENT '企业id',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业名',
  `year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调研年份',
  `org_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业性质',
  `org_province_id` int NULL DEFAULT NULL COMMENT '所在省',
  `org_province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在省',
  `org_city_id` int NULL DEFAULT NULL COMMENT '所在市',
  `org_city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在市',
  `org_area_id` int NULL DEFAULT NULL COMMENT '所在区',
  `org_area_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在区',
  `org_industry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行业属性',
  `research_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调研类型',
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_research_field
-- ----------------------------
DROP TABLE IF EXISTS `train_research_field`;
CREATE TABLE `train_research_field`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `research_id` int NULL DEFAULT NULL,
  `template_id` int NULL DEFAULT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `image_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研导出字段' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_research_org
-- ----------------------------
DROP TABLE IF EXISTS `train_research_org`;
CREATE TABLE `train_research_org`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `province_id` int NULL DEFAULT NULL COMMENT '所在省',
  `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在省',
  `city_id` int NULL DEFAULT NULL COMMENT '所在市',
  `city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在市',
  `area_id` int NULL DEFAULT NULL COMMENT '所在区',
  `area_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在区',
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研机构' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_research_people_list
-- ----------------------------
DROP TABLE IF EXISTS `train_research_people_list`;
CREATE TABLE `train_research_people_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `research_id` int NULL DEFAULT NULL COMMENT '调研id',
  `research_org_id` int NULL DEFAULT NULL COMMENT '调研机构',
  `research_org_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调研机构类型',
  `research_site_id` int NULL DEFAULT NULL COMMENT '调研站点',
  `year` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '采集年份',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别(0男  1女)',
  `birthday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '出生日期',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位',
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `qualification_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '资格证书级别（无证书、学徒工、初级工、中级工、高级工、技师、高级技师、特级技师、首席技师）',
  `cert_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职称证书级别（无证书、助理工程师、中级工程师、高级工程师、教授级高级工程师）',
  `inducation_cert` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上岗证书',
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学历',
  `people_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人员类型（管理人员、技术人员、技能人员）',
  `talent_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人才类型（政府授予的荣誉称号）',
  `sign_start_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同开始时间',
  `sign_end_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同结束时间',
  `start_work_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '开始该工作的时间',
  `get_cert_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '获得职称证书时间',
  `get_qualification_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '获得资格证书时间',
  `get_inducation_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '获得上岗证书时间',
  `is_unemployment` tinyint(1) NULL DEFAULT 0 COMMENT '是否城镇登记失业人员',
  `is_from_countryside` tinyint(1) NULL DEFAULT 0 COMMENT '是否农村转移就业劳动者',
  `graduate_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '大学以上的毕业年份',
  `is_middle_school_urban` tinyint(1) NULL DEFAULT 0 COMMENT '是否城镇户籍未继续升学的初高中毕业',
  `is_middle_school_countryside` tinyint(1) NULL DEFAULT 0 COMMENT '是否乡村户籍未继续升学的初高中毕业',
  `is_poor_urban` tinyint(1) NULL DEFAULT NULL COMMENT '是否城镇户籍贫困劳动力',
  `is_poor_countryside` tinyint(1) NULL DEFAULT NULL COMMENT '是否乡村户籍贫困劳动力',
  `is_soldier` tinyint(1) NULL DEFAULT 0 COMMENT '是否退役军人',
  `is_soldier_family` tinyint(1) NULL DEFAULT 0 COMMENT '是否随军家属',
  `is_farmer` tinyint(1) NULL DEFAULT 0 COMMENT '是否返乡农民工',
  `is_staff` tinyint(1) NULL DEFAULT NULL COMMENT '是否在职职工',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源',
  `province_id` int NULL DEFAULT NULL,
  `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 122 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研企业人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_research_site
-- ----------------------------
DROP TABLE IF EXISTS `train_research_site`;
CREATE TABLE `train_research_site`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `province_id` int NULL DEFAULT NULL COMMENT '所在省',
  `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在省',
  `city_id` int NULL DEFAULT NULL COMMENT '所在市',
  `city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在市',
  `area_id` int NULL DEFAULT NULL COMMENT '所在区',
  `area_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所在区',
  `user_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID',
  `sort_number` int UNSIGNED NOT NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研站点' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_research_template
-- ----------------------------
DROP TABLE IF EXISTS `train_research_template`;
CREATE TABLE `train_research_template`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `research_id` int NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '调研导出模板' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_resource
-- ----------------------------
DROP TABLE IF EXISTS `train_resource`;
CREATE TABLE `train_resource`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_resource_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_schedule
-- ----------------------------
DROP TABLE IF EXISTS `train_schedule`;
CREATE TABLE `train_schedule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `teacher_id` bigint NULL DEFAULT NULL COMMENT '教师ID',
  `teacher_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教师姓名',
  `course_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '课程名称',
  `room_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教室名称',
  `day_of_week` int NULL DEFAULT NULL COMMENT '星期几',
  `start_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结束时间',
  `schedule_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日期',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_schedule_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_schedule_class`(`class_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_settlement
-- ----------------------------
DROP TABLE IF EXISTS `train_settlement`;
CREATE TABLE `train_settlement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `settlement_amount` decimal(16, 2) NULL DEFAULT NULL COMMENT '结算金额',
  `institution_share` decimal(16, 2) NULL DEFAULT NULL COMMENT '机构分成',
  `enterprise_share` decimal(16, 2) NULL DEFAULT NULL COMMENT '企业分成',
  `third_party_share` decimal(16, 2) NULL DEFAULT NULL COMMENT '第三方分成',
  `share_ratio` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分成比例',
  `approval_status` int NULL DEFAULT 0 COMMENT '审批状态',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批时间',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_settlement_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_settlement_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_sign_up
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up`;
CREATE TABLE `train_sign_up`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `work_type_id` int NULL DEFAULT NULL,
  `is_new` tinyint(1) NULL DEFAULT 0 COMMENT '是否新型学徒制',
  `course_id` int NULL DEFAULT NULL,
  `level` tinyint(1) NULL DEFAULT 0 COMMENT '申报等级',
  `cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '封面',
  `job` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职业',
  `train_time` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '培训时间',
  `exam_time` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '考试时间',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '介绍',
  `user_id` int NULL DEFAULT NULL,
  `has_open` tinyint(1) NULL DEFAULT 0,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `need_contact` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要合同',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约报名' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sign_up_orc_self
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up_orc_self`;
CREATE TABLE `train_sign_up_orc_self`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `sign_up_id` int NOT NULL DEFAULT 0,
  `id_card_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `realname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `orc_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '识别类型',
  `pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `post` json NULL,
  `fail_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `fail_msg` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `orc_result` json NOT NULL,
  `addtime` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '身份证识别失败记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sign_up_people
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up_people`;
CREATE TABLE `train_sign_up_people`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `sign_up_id` int NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `work_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工种',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0',
  `company_id` int NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `zip_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `industrial_park` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工业园',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `user_id` int NULL DEFAULT NULL,
  `comments` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `id_card_front` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card_back` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card_header` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '一寸照',
  `id_card_header2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '两寸照',
  `id_card_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `work_type_paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工种证书',
  `work_date_start` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `work_date_end` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title_paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `has_pay` tinyint(1) NULL DEFAULT 0,
  `school` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `in_school_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `graduate_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `school_paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `education` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `condition` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '申报条件',
  `school_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '毕业证编号',
  `paper_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证书编号',
  `work_exp` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作经历',
  `contact_first_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同第一页',
  `contact_expire_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同期限页',
  `contact_sign_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同盖章页三页',
  `social_security` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社保',
  `house_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户口性质',
  `political` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '政治面貌',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 591 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约报名人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sign_up_people_self
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up_people_self`;
CREATE TABLE `train_sign_up_people_self`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `sign_up_id` int NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `req_work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `req_work_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工种',
  `req_level` int NULL DEFAULT 0,
  `job` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职业',
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `work_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工种',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0',
  `company_id` int NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `zip_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `industrial_park` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工业园',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `comments` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `company_status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `id_card_front` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card_back` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card_header` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '一寸照',
  `id_card_header2` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '两寸照',
  `id_card_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `work_type_paper_has` int NOT NULL DEFAULT 0 COMMENT '是否有上一等级证书:0否,1是',
  `work_type_paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上一等级证书图片',
  `work_type_paper_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上一等级证书编号',
  `work_type_paper_level` int NOT NULL DEFAULT 0 COMMENT '上一等级证书等级',
  `work_type_paper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上一等级证书工种',
  `work_date_start` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '本公司入职日期',
  `work_date_end` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '本公司在职结束日期',
  `work_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '从业日期',
  `title_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职称',
  `title_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title_paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职称证',
  `has_pay` tinyint(1) NULL DEFAULT 0,
  `school` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `in_school_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `graduate_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `school_paper_has` int NOT NULL DEFAULT 0 COMMENT '是否有毕业证:0否,1有',
  `school_paper` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `education` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `condition` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '申报条件',
  `school_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '毕业证编号',
  `paper_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证书编号',
  `work_exp` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工作经历',
  `contact_first_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同第一页',
  `contact_expire_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同期限页',
  `contact_sign_page` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '劳动合同盖章页三页',
  `social_security` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社保',
  `house_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户口性质',
  `political` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '政治面貌',
  `task` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `sign_up_id`(`sign_up_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3018 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约报名人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sign_up_self
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up_self`;
CREATE TABLE `train_sign_up_self`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '封面',
  `train_time` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '培训时间',
  `exam_time` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '考试时间',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '介绍',
  `user_id` int NULL DEFAULT NULL,
  `has_open` tinyint(1) NULL DEFAULT 0,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `need_contact` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要合同',
  `company_id` int NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `org_id` int NOT NULL DEFAULT 0 COMMENT '组织id',
  `train_info_id` int NOT NULL DEFAULT 0,
  `jigou_id` int NOT NULL DEFAULT 1 COMMENT '机构id',
  `files` json NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 122 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约报名' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sign_up_selftask
-- ----------------------------
DROP TABLE IF EXISTS `train_sign_up_selftask`;
CREATE TABLE `train_sign_up_selftask`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `sign_up_id` int NOT NULL DEFAULT 0,
  `addtime` int NOT NULL DEFAULT 0,
  `endtime` int NOT NULL DEFAULT 0,
  `ids_in` json NULL,
  `ids_out` json NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '信息收集材料导出任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_sim_exam_list
-- ----------------------------
DROP TABLE IF EXISTS `train_sim_exam_list`;
CREATE TABLE `train_sim_exam_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `proposition_user_id` int NULL DEFAULT NULL COMMENT '命题人员',
  `proposition_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '命题人员',
  `marking_user_id` int NULL DEFAULT NULL COMMENT '阅卷人员',
  `marking_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '阅卷人员',
  `exam_id` int NULL DEFAULT NULL,
  `exam_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '考试日期',
  `exam_time` int NULL DEFAULT NULL COMMENT '考试时间(分钟)',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模拟考试' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_solution
-- ----------------------------
DROP TABLE IF EXISTS `train_solution`;
CREATE TABLE `train_solution`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_info_id` bigint NULL DEFAULT NULL COMMENT '培训信息ID',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企业名称',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `nation_policy` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '国家政策',
  `industry_situation_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '行业形势分析',
  `company_development_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '企业发展分析',
  `research_data_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '调研数据分析',
  `project_development_ideas` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目开发思路',
  `main_objectives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主要目标',
  `main_initiatives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主要举措',
  `development_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '发展计划',
  `division` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分工',
  `risk` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '风险',
  `communication` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '沟通',
  `checkpoint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '检查点',
  `results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '成果',
  `teacher_intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '师资介绍',
  `request_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '需求分析',
  `train_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '培训内容',
  `train_method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '培训方式',
  `course` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '课程',
  `status` int NULL DEFAULT 0 COMMENT '状态 0-草稿 1-待审批 2-已通过 3-已驳回',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件路径',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_solution_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_solution_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_solution_badge
-- ----------------------------
DROP TABLE IF EXISTS `train_solution_badge`;
CREATE TABLE `train_solution_badge`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `solution_id` int NULL DEFAULT NULL COMMENT '培训方案',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '费用名',
  `badge` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算',
  `expend` decimal(10, 2) NULL DEFAULT NULL COMMENT '支出',
  `status` int NOT NULL DEFAULT 0 COMMENT '任务状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `user_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 112 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训方案预算管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_student_chat
-- ----------------------------
DROP TABLE IF EXISTS `train_student_chat`;
CREATE TABLE `train_student_chat`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `need_id` int NULL DEFAULT NULL,
  `from_user_id` int NULL DEFAULT NULL,
  `to_user_id` int NULL DEFAULT NULL,
  `sort_number` int UNSIGNED NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `deleted` int NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `from_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_student_list
-- ----------------------------
DROP TABLE IF EXISTS `train_student_list`;
CREATE TABLE `train_student_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `org_id` int NULL DEFAULT NULL,
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` int NULL DEFAULT 0,
  `edu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学历',
  `major` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专业',
  `graduate_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '毕业时间',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `birthday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `nation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `in_school_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `school_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '毕业证编号',
  `political` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '政治面貌',
  `json_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '生源' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_subsidy
-- ----------------------------
DROP TABLE IF EXISTS `train_subsidy`;
CREATE TABLE `train_subsidy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `policy_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '政策模板',
  `subsidy_amount` decimal(16, 2) NULL DEFAULT NULL COMMENT '补贴金额',
  `student_count` int NULL DEFAULT NULL COMMENT '学员人数',
  `material_package_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '材料包路径',
  `material_status` int NULL DEFAULT 0 COMMENT '材料状态',
  `rssb_status` int NULL DEFAULT 0 COMMENT '人社局状态',
  `rssb_submit_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '人社局提交时间',
  `rssb_receipt_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '人社局回执号',
  `rssb_reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '人社局驳回原因',
  `granted_amount` decimal(16, 2) NULL DEFAULT NULL COMMENT '已拨付金额',
  `grant_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拨付时间',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_subsidy_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_subsidy_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_summary
-- ----------------------------
DROP TABLE IF EXISTS `train_summary`;
CREATE TABLE `train_summary`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NULL DEFAULT NULL COMMENT '项目ID',
  `certification_rate` decimal(10, 2) NULL DEFAULT NULL COMMENT '认定通过率',
  `subsidy_success_rate` decimal(10, 2) NULL DEFAULT NULL COMMENT '补贴成功率',
  `achievements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '成果',
  `lessons_learned` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '经验教训',
  `improvement_suggestions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '改进建议',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件路径',
  `approval_status` int NULL DEFAULT 0 COMMENT '审批状态',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批时间',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_summary_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_summary_project`(`project_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_talent_need_chat
-- ----------------------------
DROP TABLE IF EXISTS `train_talent_need_chat`;
CREATE TABLE `train_talent_need_chat`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `need_id` int NULL DEFAULT NULL,
  `from_user_id` int NULL DEFAULT NULL,
  `to_user_id` int NULL DEFAULT NULL,
  `sort_number` int UNSIGNED NULL DEFAULT 100 COMMENT '排序(数字越小越靠前)',
  `deleted` int NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NULL DEFAULT 1 COMMENT '租户id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `from_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for train_talent_needs
-- ----------------------------
DROP TABLE IF EXISTS `train_talent_needs`;
CREATE TABLE `train_talent_needs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `org_id` bigint NULL DEFAULT NULL COMMENT '组织ID',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组织名称',
  `job` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '岗位',
  `work_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工种',
  `major` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业',
  `edu` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `gender` int NULL DEFAULT NULL COMMENT '性别 0-不限 1-男 2-女',
  `num` int NULL DEFAULT NULL COMMENT '人数',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `work_year` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工作年限',
  `in_position_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '到岗日期',
  `paper_require` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证书要求',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `json_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展JSON数据',
  `duty` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '职责',
  `school_need` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学校需求',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_talent_needs_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_talent_needs_org`(`org_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_task_info
-- ----------------------------
DROP TABLE IF EXISTS `train_task_info`;
CREATE TABLE `train_task_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `train_task_id` bigint NULL DEFAULT NULL COMMENT '培训任务ID',
  `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识ID',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '简介',
  `is_required` int NULL DEFAULT 0 COMMENT '是否必修',
  `score` int NULL DEFAULT NULL COMMENT '分数',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_task_info_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_task_info_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_task_info_detail
-- ----------------------------
DROP TABLE IF EXISTS `train_task_info_detail`;
CREATE TABLE `train_task_info_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `task_list_id` bigint NULL DEFAULT NULL COMMENT '任务ID',
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `get_learn_time` decimal(10, 2) NULL DEFAULT NULL COMMENT '获取学习时间',
  `min_minute` int NULL DEFAULT NULL COMMENT '最少分钟',
  `is_required` int NULL DEFAULT 0 COMMENT '是否必修',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `score` int NULL DEFAULT NULL COMMENT '分数',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_task_info_detail_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_task_info_detail_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_task_list
-- ----------------------------
DROP TABLE IF EXISTS `train_task_list`;
CREATE TABLE `train_task_list`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `get_learn_time` decimal(10, 2) NULL DEFAULT NULL COMMENT '获取学习时间',
  `min_minute` int NULL DEFAULT NULL COMMENT '最少分钟',
  `is_required` int NULL DEFAULT 0 COMMENT '是否必修 0-否 1-是',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `score` int NULL DEFAULT NULL COMMENT '分数',
  `get_point` decimal(10, 2) NULL DEFAULT NULL COMMENT '获取积分',
  `check_status` int NULL DEFAULT NULL COMMENT '审核状态',
  `final_check_status` int NULL DEFAULT NULL COMMENT '终审状态',
  `check_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `check_user_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核人姓名',
  `final_check_time` datetime NULL DEFAULT NULL COMMENT '终审时间',
  `final_check_user_id` bigint NULL DEFAULT NULL COMMENT '终审人ID',
  `final_check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '终审人姓名',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_task_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_task_work_type`(`work_type_id` ASC) USING BTREE,
  INDEX `idx_train_task_course`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_task_log
-- ----------------------------
DROP TABLE IF EXISTS `train_task_log`;
CREATE TABLE `train_task_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_class_id` bigint NULL DEFAULT NULL COMMENT '班级ID',
  `ck_id` bigint NULL DEFAULT NULL COMMENT '签到ID',
  `date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日期',
  `addtime` int NULL DEFAULT NULL COMMENT '添加时间(时间戳)',
  `status` int NULL DEFAULT NULL COMMENT '状态',
  `times` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_task_log_class`(`train_class_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_teacher_bank
-- ----------------------------
DROP TABLE IF EXISTS `train_teacher_bank`;
CREATE TABLE `train_teacher_bank`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `id_card` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证',
  `teacher_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '教师资格证',
  `engineer_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工程师证',
  `level_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '等级证',
  `edu_license` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历证',
  `graduate_paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '毕业证',
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公司',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `id_card_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证号',
  `start_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结束日期',
  `birthday` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '生日',
  `gender` int NULL DEFAULT NULL COMMENT '性别',
  `nation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '民族',
  `education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `major` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '专业',
  `status` int NULL DEFAULT 1 COMMENT '状态',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_teacher_bank_tenant`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_ability
-- ----------------------------
DROP TABLE IF EXISTS `train_user_ability`;
CREATE TABLE `train_user_ability`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识ID',
  `result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '结果',
  `config_id` bigint NULL DEFAULT NULL COMMENT '配置ID',
  `score` int NULL DEFAULT NULL COMMENT '分数',
  `current_score` int NULL DEFAULT NULL COMMENT '当前分数',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `comments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_ability_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_ability_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_course_paper
-- ----------------------------
DROP TABLE IF EXISTS `train_user_course_paper`;
CREATE TABLE `train_user_course_paper`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `work_type_id` bigint NULL DEFAULT NULL COMMENT '工种ID',
  `get_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '获取年份',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_course_paper_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_course_paper_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_course_plan
-- ----------------------------
DROP TABLE IF EXISTS `train_user_course_plan`;
CREATE TABLE `train_user_course_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `realname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `add_from` int NULL DEFAULT NULL COMMENT '添加来源',
  `course_id` bigint NULL DEFAULT NULL COMMENT '课程ID',
  `date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日期',
  `type` int NULL DEFAULT NULL COMMENT '类型',
  `status` int NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_course_plan_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_course_plan_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_exam_log
-- ----------------------------
DROP TABLE IF EXISTS `train_user_exam_log`;
CREATE TABLE `train_user_exam_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_exam_log_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_exam_log_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_info
-- ----------------------------
DROP TABLE IF EXISTS `train_user_info`;
CREATE TABLE `train_user_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `company_id` bigint NULL DEFAULT NULL COMMENT '企业ID',
  `person_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '人员类型',
  `person_supplementary` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '人员补充',
  `house_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '户籍类型',
  `nationality` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '国籍',
  `education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历',
  `job_intention` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '求职意向',
  `is_party_member` int NULL DEFAULT 0 COMMENT '是否党员',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `id_card_front` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证正面',
  `id_card_back` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '身份证背面',
  `photo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '照片',
  `edu_photo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学历照片',
  `work_contract` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '劳动合同',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_info_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_info_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_learn_log
-- ----------------------------
DROP TABLE IF EXISTS `train_user_learn_log`;
CREATE TABLE `train_user_learn_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `train_id` bigint NULL DEFAULT NULL COMMENT '培训ID',
  `match_id` bigint NULL DEFAULT NULL COMMENT '匹配ID',
  `train_task_id` bigint NULL DEFAULT NULL COMMENT '培训任务ID',
  `learn_time` int NULL DEFAULT NULL COMMENT '学习时间(分钟)',
  `score` int NULL DEFAULT NULL COMMENT '分数',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_learn_log_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_learn_log_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_train_user_learn_log_train`(`train_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_paper
-- ----------------------------
DROP TABLE IF EXISTS `train_user_paper`;
CREATE TABLE `train_user_paper`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'URL',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `platform_user_id` bigint NULL DEFAULT NULL COMMENT '平台用户ID',
  `sort_number` int NULL DEFAULT 0 COMMENT '排序号',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `pk` bigint NULL DEFAULT NULL COMMENT '关联主键',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_paper_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_paper_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for train_user_patent
-- ----------------------------
DROP TABLE IF EXISTS `train_user_patent`;
CREATE TABLE `train_user_patent`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '证书',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_train_user_patent_tenant`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_train_user_patent_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
