/*
 Navicat Premium Dump SQL

 Source Server         : 200本机
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3308
 Source Schema         : gjjd_test

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 15/05/2026 18:44:34
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
-- Records of ai_generation_draft
-- ----------------------------

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
-- Records of biz_approval_record
-- ----------------------------

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
-- Records of course
-- ----------------------------

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
-- Records of course_chapter
-- ----------------------------

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
-- Records of exam_paper
-- ----------------------------

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
-- Records of exam_paper_question
-- ----------------------------

-- ----------------------------
-- Table structure for exam_result
-- ----------------------------
DROP TABLE IF EXISTS `exam_result`;
CREATE TABLE `exam_result`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '题型',
  `exam_plan_id` int NOT NULL DEFAULT 0 COMMENT 'train_exam_list表id',
  `paper_id` int NULL DEFAULT NULL COMMENT '试卷id',
  `detail_id` int NULL DEFAULT NULL COMMENT '题目id',
  `get_score` decimal(5, 1) NULL DEFAULT NULL COMMENT '获得分数',
  `exam_type` tinyint(1) NULL DEFAULT 0 COMMENT '0正考  1模拟考  2能力测评  3顺序练习 4随机练习',
  `exam_id` int NULL DEFAULT NULL COMMENT '配置试卷的sim_exam/exam 主键,app3.2.3版本开始表示paper_detail表的exam_id',
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `type`(`type` ASC) USING BTREE,
  INDEX `paper_id`(`paper_id` ASC) USING BTREE,
  INDEX `detail_id`(`detail_id` ASC) USING BTREE,
  INDEX `exam_type`(`exam_type` ASC) USING BTREE,
  INDEX `exam_id`(`exam_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `deleted`(`deleted` ASC) USING BTREE,
  INDEX `sort_number`(`sort_number` ASC) USING BTREE,
  INDEX `tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49371 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试结果' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exam_result
-- ----------------------------

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
-- Records of exam_session
-- ----------------------------

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
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 136 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工种' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of exam_work_type
-- ----------------------------
INSERT INTO `exam_work_type` VALUES (3, '母液蒸发工', '', '', 22, 3742, 0, 1, 100, 10159, '2024-07-26 11:44:42');
INSERT INTO `exam_work_type` VALUES (8, '氢氧化铝分解工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-07-26 11:45:07');
INSERT INTO `exam_work_type` VALUES (11, '多功能机组操作工', '', '', 18, 3742, 0, 1, 100, 10159, '2024-07-26 11:33:51');
INSERT INTO `exam_work_type` VALUES (15, '铝电解槽操作工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-07-26 11:40:43');
INSERT INTO `exam_work_type` VALUES (18, '铝电解工', '6-17-06-02', '', NULL, 3742, 0, 1, 100, 10159, '2024-07-26 11:42:46');
INSERT INTO `exam_work_type` VALUES (22, '氧化铝制取工', '6-17-06-01', '', NULL, 3742, 0, 1, 100, 10159, '2024-07-26 11:44:13');
INSERT INTO `exam_work_type` VALUES (27, '金属轧制工', '6-17-09-02', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-03 13:50:55');
INSERT INTO `exam_work_type` VALUES (28, '车轮轧制工', NULL, '', 27, 3742, 0, 1, 100, 10159, '2024-09-03 13:51:40');
INSERT INTO `exam_work_type` VALUES (29, '重冶湿法冶炼工', '6-17-05-04', '', NULL, 4376, 0, 1, 100, 10159, '2024-09-05 09:49:17');
INSERT INTO `exam_work_type` VALUES (30, '净化工', NULL, '', 29, 4376, 0, 1, 100, 10159, '2024-09-05 09:49:46');
INSERT INTO `exam_work_type` VALUES (31, '浸出工', NULL, '', 29, 4376, 0, 1, 100, 10159, '2024-09-05 09:50:01');
INSERT INTO `exam_work_type` VALUES (32, '铝电解筑炉工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 08:57:58');
INSERT INTO `exam_work_type` VALUES (33, '物料分析检测工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 08:59:01');
INSERT INTO `exam_work_type` VALUES (34, '炭极生产工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 08:59:41');
INSERT INTO `exam_work_type` VALUES (35, '铝及铝合金熔铸工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 09:00:20');
INSERT INTO `exam_work_type` VALUES (36, '电解槽计算机监控工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 09:01:01');
INSERT INTO `exam_work_type` VALUES (37, '物料输送及烟气净化工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 09:01:33');
INSERT INTO `exam_work_type` VALUES (38, '铝吸出工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 09:01:57');
INSERT INTO `exam_work_type` VALUES (39, '铝电解综合工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-09-09 09:02:24');
INSERT INTO `exam_work_type` VALUES (40, '氧化铝焙烧工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:05:02');
INSERT INTO `exam_work_type` VALUES (41, '粗液脱硅工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:06:31');
INSERT INTO `exam_work_type` VALUES (42, '轻冶沉降工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:07:14');
INSERT INTO `exam_work_type` VALUES (43, '烧结法溶出工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:07:56');
INSERT INTO `exam_work_type` VALUES (44, '拜耳法溶出工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:08:24');
INSERT INTO `exam_work_type` VALUES (45, '熟料烧结工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:08:57');
INSERT INTO `exam_work_type` VALUES (46, '轻冶浆液调整输送工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:09:48');
INSERT INTO `exam_work_type` VALUES (47, '轻冶料浆配料工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-09-09 09:10:44');
INSERT INTO `exam_work_type` VALUES (48, '蒸发浓缩结晶工', NULL, '', 29, 3742, 0, 1, 100, 10159, '2024-09-09 09:12:32');
INSERT INTO `exam_work_type` VALUES (49, '重冶萃取工', '', '', 29, 3742, 0, 1, 100, 10159, '2024-09-09 09:13:04');
INSERT INTO `exam_work_type` VALUES (50, '有色液固分离工', '', '', 29, 3742, 0, 1, 100, 10159, '2024-09-09 09:13:33');
INSERT INTO `exam_work_type` VALUES (51, '轧制备品工', NULL, '', 27, 3742, 0, 1, 100, 10159, '2024-09-09 09:15:30');
INSERT INTO `exam_work_type` VALUES (52, '井下支护工', '6-16-01-06', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-09 09:16:30');
INSERT INTO `exam_work_type` VALUES (53, '矿山救护工', '6-16-01-12', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-09 09:17:22');
INSERT INTO `exam_work_type` VALUES (54, '重冶火法冶炼工', '6-17-05-03', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-09 09:18:05');
INSERT INTO `exam_work_type` VALUES (55, '电解精炼工', '6-17-05-05', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-09 09:18:47');
INSERT INTO `exam_work_type` VALUES (56, '重冶配液工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:19:25');
INSERT INTO `exam_work_type` VALUES (57, '重冶净液工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:01');
INSERT INTO `exam_work_type` VALUES (58, '阴阳极制作工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:26');
INSERT INTO `exam_work_type` VALUES (59, '电解槽操作工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:52');
INSERT INTO `exam_work_type` VALUES (60, '电解熔铸工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:21:13');
INSERT INTO `exam_work_type` VALUES (69, '金属挤压工', '6-17-09-09', '', NULL, 3742, 0, 1, 100, 10159, '2024-09-09 09:31:25');
INSERT INTO `exam_work_type` VALUES (70, '有色挤压工', NULL, '', 69, 3742, 0, 1, 100, 10159, '2024-09-09 09:31:51');
INSERT INTO `exam_work_type` VALUES (71, '挤压修模工', NULL, '', 69, 3742, 0, 1, 100, 10159, '2024-09-09 09:32:20');
INSERT INTO `exam_work_type` VALUES (76, '重冶火法冶炼工', NULL, '', 54, 3742, 0, 1, 100, 10159, '2024-10-17 16:52:18');
INSERT INTO `exam_work_type` VALUES (107, '矿山救护工', '6-16-01-12', '', 53, 26743, 0, 1, 1, 10159, '2025-03-14 08:37:32');
INSERT INTO `exam_work_type` VALUES (117, '氧化铝制取工', '6-17-06-02', '', 22, 3742, 0, 1, 100, 10159, '2025-06-18 16:26:42');
INSERT INTO `exam_work_type` VALUES (125, '重冶配液工', '', '', 55, NULL, 0, 1, 1, 10159, '2025-12-26 09:51:40');
INSERT INTO `exam_work_type` VALUES (126, '金属材热处理工', '9-11-01-01', '', 0, NULL, 0, 1, 1, 10159, '2025-12-29 11:31:53');
INSERT INTO `exam_work_type` VALUES (127, '有色金属材热处理工', '6-17-09-05', '', 126, NULL, 0, 1, 1, 10159, '2025-12-29 11:34:21');
INSERT INTO `exam_work_type` VALUES (128, '井下支护工', '6-16-01-06', '', 52, NULL, 0, 1, 1, 10159, '2025-12-30 11:06:23');
INSERT INTO `exam_work_type` VALUES (133, '金属材精整工', '6-17-09-07', '', 0, NULL, 0, 1, 1, 10159, '2025-12-30 11:15:47');
INSERT INTO `exam_work_type` VALUES (134, '管棒型材精整工', '6-17-09-07', '', 133, NULL, 0, 1, 1, 10159, '2025-12-30 11:16:33');
INSERT INTO `exam_work_type` VALUES (135, '板带箔材精整工', '6-17-09-07', '', 133, NULL, 0, 1, 1, 10159, '2025-12-30 11:24:09');

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
-- Records of flyway_schema_history
-- ----------------------------
INSERT INTO `flyway_schema_history` VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'root', '2026-05-13 14:49:30', 0, 1);
INSERT INTO `flyway_schema_history` VALUES (2, '20260505.001', 'init phase1 tables', 'SQL', 'V20260505_001__init_phase1_tables.sql', -294111305, 'root', '2026-05-13 14:49:30', 59, 1);
INSERT INTO `flyway_schema_history` VALUES (3, '20260505.002', 'seed demo tenant admin', 'SQL', 'V20260505_002__seed_demo_tenant_admin.sql', 1931927789, 'root', '2026-05-13 14:51:56', 73, 1);
INSERT INTO `flyway_schema_history` VALUES (4, '20260505.003', 'init knowledge base tables', 'SQL', 'V20260505_003__init_knowledge_base_tables.sql', 1155636813, 'root', '2026-05-13 14:52:53', 702, 1);
INSERT INTO `flyway_schema_history` VALUES (5, '20260505.004', 'seed knowledge permissions', 'SQL', 'V20260505_004__seed_knowledge_permissions.sql', -167822924, 'root', '2026-05-13 14:52:53', 10, 1);
INSERT INTO `flyway_schema_history` VALUES (6, '20260505.005', 'add knowledge document acl', 'SQL', 'V20260505_005__add_knowledge_document_acl.sql', -1153426914, 'root', '2026-05-13 14:52:53', 167, 1);
INSERT INTO `flyway_schema_history` VALUES (7, '20260515.001', 'create exam work type', 'SQL', 'V20260515_001__create_exam_work_type.sql', -817229399, 'root', '2026-05-15 18:08:32', 66, 1);

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
-- Records of kb_chunk
-- ----------------------------

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
-- Records of kb_document
-- ----------------------------

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
-- Records of kb_document_acl
-- ----------------------------

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
-- Records of kb_index_task
-- ----------------------------

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
-- Records of kb_space
-- ----------------------------

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
-- Records of kb_space_role
-- ----------------------------

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
-- Records of platform_user
-- ----------------------------

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
-- Records of question
-- ----------------------------

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
-- Records of question_option
-- ----------------------------

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
-- Records of student_answer
-- ----------------------------

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
-- Records of student_exam_attempt
-- ----------------------------

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
-- Records of student_learning_record
-- ----------------------------

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
-- Records of student_wrong_question
-- ----------------------------

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
-- Records of sys_audit_log
-- ----------------------------

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
-- Records of sys_dict
-- ----------------------------

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
-- Records of sys_dict_item
-- ----------------------------

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
-- Records of sys_file
-- ----------------------------

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
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (4000, 1, 0, 'page', 'dashboard', '管理概览', '/dashboard', 'dashboard/Overview', 'dashboard:view', 'DashboardOutlined', 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4100, 1, 0, 'directory', 'system', '系统管理', '/system', NULL, 'system:view', 'SettingOutlined', 10, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4101, 1, 4100, 'page', 'system_tenant', '租户管理', '/system/tenants', 'system/TenantList', 'tenant:view', NULL, 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4102, 1, 4100, 'page', 'system_org', '组织管理', '/system/orgs', 'system/OrgTree', 'org:view', NULL, 2, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4103, 1, 4100, 'page', 'system_user', '用户管理', '/system/users', 'system/UserList', 'user:view', NULL, 3, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4104, 1, 4100, 'page', 'system_role', '角色权限', '/system/roles', 'system/RoleList', 'role:view', NULL, 4, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4105, 1, 4100, 'page', 'system_menu', '菜单管理', '/system/menus', 'system/MenuTree', 'menu:view', NULL, 5, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4106, 1, 4100, 'page', 'system_dict', '字典管理', '/system/dicts', 'system/DictList', 'dict:view', NULL, 6, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4107, 1, 4100, 'page', 'system_audit_log', '操作日志', '/system/audit-logs', 'system/AuditLogList', 'audit-log:view', NULL, 7, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4200, 1, 0, 'directory', 'course_center', '课程题库', '/course', NULL, 'course:view', 'BookOutlined', 20, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4201, 1, 4200, 'page', 'course_manage', '课程管理', '/course/courses', 'course/CourseList', 'course:view', NULL, 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4202, 1, 4200, 'page', 'question_manage', '题库管理', '/course/questions', 'question/QuestionList', 'question:view', NULL, 2, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4203, 1, 4200, 'page', 'paper_manage', '试卷管理', '/course/exam-papers', 'exam/PaperList', 'exam:view', NULL, 3, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4300, 1, 0, 'directory', 'training', '培训考试', '/training', NULL, 'exam:view', 'ScheduleOutlined', 30, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4301, 1, 4300, 'page', 'exam_session', '考试安排', '/training/exam-sessions', 'exam/SessionList', 'exam:view', NULL, 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4400, 1, 0, 'directory', 'workflow', '流程审批', '/workflow', NULL, 'workflow:view', 'BranchesOutlined', 40, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4401, 1, 4400, 'page', 'workflow_tasks', '我的待办', '/workflow/tasks', 'workflow/TaskList', 'workflow:view', NULL, 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4500, 1, 0, 'directory', 'ai', 'AI 样板', '/ai', NULL, 'ai:course-outline', 'RobotOutlined', 50, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4501, 1, 4500, 'page', 'ai_course_outline', '课程大纲草稿', '/ai/course-outline', 'ai/CourseOutlineDraft', 'ai:course-outline', NULL, 1, 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4600, 1, 0, 'directory', 'knowledge', '知识库', '/knowledge', NULL, 'knowledge:view', 'DatabaseOutlined', 55, 1, 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4601, 1, 4600, 'page', 'knowledge_spaces', '企业知识库', '/knowledge/spaces', 'knowledge/KnowledgeSpaceList', 'knowledge:view', NULL, 1, 1, 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_menu` VALUES (4602, 1, 4600, 'page', 'knowledge_search', '知识库检索', '/knowledge/search', 'knowledge/KnowledgeSearch', 'knowledge:search', NULL, 2, 1, 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);

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
-- Records of sys_org
-- ----------------------------
INSERT INTO `sys_org` VALUES (100, 1, 0, 'company', 'demo-root', '演示公司', 1, 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);

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
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (5000, 1, 4000, 'dashboard:view', '查看管理概览', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5001, 1, 4101, 'tenant:view', '查看租户', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5002, 1, 4101, 'tenant:create', '新增租户', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5003, 1, 4101, 'tenant:update', '更新租户', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5004, 1, 4101, 'tenant:delete', '删除租户', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5010, 1, 4102, 'org:view', '查看组织', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5011, 1, 4102, 'org:create', '新增组织', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5012, 1, 4102, 'org:update', '更新组织', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5013, 1, 4102, 'org:delete', '删除组织', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5020, 1, 4103, 'user:view', '查看用户', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5021, 1, 4103, 'user:create', '新增用户', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5022, 1, 4103, 'user:update', '更新用户', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5023, 1, 4103, 'user:delete', '删除用户', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5030, 1, 4104, 'role:view', '查看角色', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5031, 1, 4104, 'role:create', '新增角色', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5032, 1, 4104, 'role:update', '更新角色', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5033, 1, 4104, 'role:delete', '删除角色', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5040, 1, 4105, 'menu:view', '查看菜单', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5041, 1, 4105, 'menu:create', '新增菜单', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5042, 1, 4105, 'menu:update', '更新菜单', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5043, 1, 4105, 'menu:delete', '删除菜单', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5050, 1, 4106, 'dict:view', '查看字典', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5051, 1, 4106, 'dict:create', '新增字典', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5052, 1, 4106, 'dict:update', '更新字典', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5055, 1, 4107, 'audit-log:view', '查看操作日志', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5060, 1, 4201, 'course:view', '查看课程', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5061, 1, 4201, 'course:create', '新增课程', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5062, 1, 4201, 'course:update', '更新课程', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5063, 1, 4201, 'course:delete', '删除课程', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5070, 1, 4202, 'question:view', '查看题目', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5071, 1, 4202, 'question:create', '新增题目', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5072, 1, 4202, 'question:update', '更新题目', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5073, 1, 4202, 'question:delete', '删除题目', 'delete', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5080, 1, 4203, 'exam:view', '查看考试', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5081, 1, 4203, 'exam:create', '新增考试', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5082, 1, 4203, 'exam:update', '更新考试', 'update', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5090, 1, 4401, 'workflow:view', '查看流程', 'view', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5091, 1, 4401, 'workflow:approve', '流程审批', 'approve', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5100, 1, 4501, 'ai:course-outline', 'AI 课程大纲草稿', 'create', 'enabled', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5110, 1, 4601, 'knowledge:view', '查看知识库', 'view', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5111, 1, 4601, 'knowledge:create', '新增知识库', 'create', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5112, 1, 4601, 'knowledge:update', '更新知识库', 'update', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5113, 1, 4601, 'knowledge:delete', '删除知识库', 'delete', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5114, 1, 4601, 'knowledge:document', '知识库资料入库', 'create', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5115, 1, 4602, 'knowledge:search', '知识库检索', 'view', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_permission` VALUES (5116, 1, 4602, 'knowledge:answer', '知识库问答草稿', 'create', 'enabled', '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);

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
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (2000, 1, 'PLATFORM_ADMIN', '平台管理员', 'platform', 'enabled', '拥有一期演示环境全部权限', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role` VALUES (2001, 1, 'TRAINING_MANAGER', '培训负责人', 'business', 'enabled', '管理课程发布、考试安排、学员学习和成绩', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role` VALUES (2002, 1, 'COURSE_STAFF', '课程人员', 'business', 'enabled', '维护课程、章节、题目和试卷', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role` VALUES (2003, 1, 'STUDENT', '学员', 'student', 'enabled', '学员学习与考试', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);

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
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (604000, 1, 2000, 4000, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604100, 1, 2000, 4100, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604101, 1, 2000, 4101, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604102, 1, 2000, 4102, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604103, 1, 2000, 4103, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604104, 1, 2000, 4104, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604105, 1, 2000, 4105, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604106, 1, 2000, 4106, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604107, 1, 2000, 4107, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604200, 1, 2000, 4200, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604201, 1, 2000, 4201, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604202, 1, 2000, 4202, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604203, 1, 2000, 4203, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604300, 1, 2000, 4300, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604301, 1, 2000, 4301, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604400, 1, 2000, 4400, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604401, 1, 2000, 4401, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604500, 1, 2000, 4500, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604501, 1, 2000, 4501, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604600, 1, 2000, 4600, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604601, 1, 2000, 4601, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_menu` VALUES (604602, 1, 2000, 4602, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);

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
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES (705000, 1, 2000, 5000, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705001, 1, 2000, 5001, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705002, 1, 2000, 5002, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705003, 1, 2000, 5003, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705004, 1, 2000, 5004, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705010, 1, 2000, 5010, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705011, 1, 2000, 5011, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705012, 1, 2000, 5012, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705013, 1, 2000, 5013, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705020, 1, 2000, 5020, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705021, 1, 2000, 5021, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705022, 1, 2000, 5022, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705023, 1, 2000, 5023, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705030, 1, 2000, 5030, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705031, 1, 2000, 5031, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705032, 1, 2000, 5032, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705033, 1, 2000, 5033, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705040, 1, 2000, 5040, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705041, 1, 2000, 5041, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705042, 1, 2000, 5042, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705043, 1, 2000, 5043, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705050, 1, 2000, 5050, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705051, 1, 2000, 5051, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705052, 1, 2000, 5052, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705055, 1, 2000, 5055, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705060, 1, 2000, 5060, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705061, 1, 2000, 5061, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705062, 1, 2000, 5062, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705063, 1, 2000, 5063, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705070, 1, 2000, 5070, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705071, 1, 2000, 5071, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705072, 1, 2000, 5072, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705073, 1, 2000, 5073, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705080, 1, 2000, 5080, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705081, 1, 2000, 5081, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705082, 1, 2000, 5082, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705090, 1, 2000, 5090, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705091, 1, 2000, 5091, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705100, 1, 2000, 5100, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705110, 1, 2000, 5110, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705111, 1, 2000, 5111, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705112, 1, 2000, 5112, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705113, 1, 2000, 5113, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705114, 1, 2000, 5114, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705115, 1, 2000, 5115, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);
INSERT INTO `sys_role_permission` VALUES (705116, 1, 2000, 5116, '2026-05-13 14:52:53', '2026-05-13 14:52:53', 1, 1, 0);

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
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, 'demo', '演示租户', 'enabled', '平台管理员', '13800000000', '一期本地开发演示租户', '2026-05-13 14:40:55', '2026-05-13 14:40:55', 0);

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
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1000, 1, 100, 'admin', '$2y$10$NeoXS685SYXSeLFA.o1Wj.p6nqLS8RixLRcq7WRh4R1EN4kGNsDDW', '平台管理员', 'admin', NULL, NULL, NULL, 'enabled', NULL, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);

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
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (3000, 1, 1000, 2000, '2026-05-13 14:40:55', '2026-05-13 14:40:55', 1, 1, 0);

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
-- Records of train_ability_comment_config
-- ----------------------------

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
-- Records of train_ability_config
-- ----------------------------

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
-- Records of train_base_data
-- ----------------------------

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
-- Records of train_brushing
-- ----------------------------

-- ----------------------------
-- Table structure for train_budget
-- ----------------------------
DROP TABLE IF EXISTS `train_budget`;
CREATE TABLE `train_budget`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `province` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '市',
  `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '市',
  `org_id` int NULL DEFAULT NULL COMMENT '企业id',
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业',
  `train_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训时间',
  `train_work_type_id` int NULL DEFAULT NULL COMMENT '培训工种id',
  `train_work_type_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训工种',
  `train_level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训级别',
  `bonus_standard` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '补贴标准',
  `expect_income` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '预计收入',
  `actual_income` decimal(10, 2) NULL DEFAULT NULL COMMENT '实际收入',
  `school_manage_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '学校管理费',
  `manage_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '公司运营管理费（含APP支持、等费用）',
  `research_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '研发费用',
  `company_require_commission` decimal(10, 2) NULL DEFAULT NULL COMMENT '企业要求分成',
  `agent_require_commission` decimal(10, 2) NULL DEFAULT NULL COMMENT '中介要求分成',
  `teacher_course_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '培训讲师课酬（含师傅带徒费用）',
  `teacher_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '培训讲师（含师傅）食宿差旅费',
  `staff_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '工作人员（含调研人员、教务人员、助教及其它人员）劳务费用',
  `staff_travel_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '工作人员（含调研人员、教务人员、助教及其它人员）食宿差旅费',
  `train_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '培训耗材费',
  `point_rewards` decimal(10, 2) NULL DEFAULT NULL COMMENT '积分奖励',
  `business_fee` decimal(10, 0) NULL DEFAULT NULL COMMENT '业务招待费',
  `people_num` int NULL DEFAULT NULL COMMENT '认定人数',
  `charge_fee` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收费标准',
  `service_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '行业服务费',
  `review_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '评审费用',
  `maintain_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '认定金额',
  `makeup_people_num` int NULL DEFAULT NULL COMMENT '补考人数',
  `theory_makeup_people_num` int NULL DEFAULT NULL COMMENT '理论补考',
  `operation_makeup_people_num` int NULL DEFAULT NULL COMMENT '实操补考',
  `staff_makeup_train_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '工作人员差旅费',
  `review_makeup_train_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '考评员差旅费',
  `profit` decimal(10, 2) NULL DEFAULT NULL COMMENT '项目利润',
  `comments` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态, 0正常, 1冻结',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `user_id` int NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `sort_number` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训费用' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_budget
-- ----------------------------

-- ----------------------------
-- Table structure for train_class_list
-- ----------------------------
DROP TABLE IF EXISTS `train_class_list`;
CREATE TABLE `train_class_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `is_leader` tinyint(1) NULL DEFAULT 0 COMMENT '是否班长',
  `is_committee` tinyint(1) NULL DEFAULT 0 COMMENT '是否班委',
  `class_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '班级名称',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 256 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分班' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_class_list
-- ----------------------------

-- ----------------------------
-- Table structure for train_course
-- ----------------------------
DROP TABLE IF EXISTS `train_course`;
CREATE TABLE `train_course`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `teacher_id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '教师身份证',
  `teacher_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '教师名字',
  `teacher_user_id` int NULL DEFAULT NULL COMMENT '教师id',
  `type` tinyint(1) NULL DEFAULT 1 COMMENT '1理论  2实操',
  `course_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上课日期',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '课程名称',
  `start_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上课时间',
  `end_time` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下课时间',
  `theory_time` int NULL DEFAULT NULL COMMENT '理论课时',
  `operation_time` int NULL DEFAULT NULL COMMENT '实操课时',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 859 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课时列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_course
-- ----------------------------

-- ----------------------------
-- Table structure for train_declare
-- ----------------------------
DROP TABLE IF EXISTS `train_declare`;
CREATE TABLE `train_declare`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `city_id` int NULL DEFAULT NULL,
  `city_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '申报任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_declare
-- ----------------------------

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
-- Records of train_declare_fields
-- ----------------------------

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
-- Records of train_declare_user_data
-- ----------------------------

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
-- Records of train_department_confirm
-- ----------------------------

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
-- Records of train_dev_organization
-- ----------------------------

-- ----------------------------
-- Table structure for train_exam_list
-- ----------------------------
DROP TABLE IF EXISTS `train_exam_list`;
CREATE TABLE `train_exam_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `train_id` int NULL DEFAULT NULL,
  `org_id` int NOT NULL,
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` tinyint(1) NULL DEFAULT 0 COMMENT '0线上  1线下',
  `source` tinyint(1) NULL DEFAULT 0 COMMENT '0内部  1外部',
  `invigilate_user_id` int NULL DEFAULT NULL COMMENT '监考人',
  `invigilate_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '监考人',
  `evaluate_user_id` int NULL DEFAULT NULL COMMENT '考评人',
  `evaluate_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '考评人',
  `watcher_user_id` int NULL DEFAULT NULL COMMENT '督导员',
  `watcher_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '督导员',
  `staff_user_id` int NULL DEFAULT NULL COMMENT '考务人员',
  `staff_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '考务人员',
  `proposition_user_id` int NULL DEFAULT NULL COMMENT '命题人员',
  `proposition_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '命题人员',
  `marking_user_id` int NULL DEFAULT NULL COMMENT '阅卷人员',
  `marking_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '阅卷人员',
  `exam_id` int NULL DEFAULT NULL COMMENT '试卷id',
  `exam_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '考试日期',
  `exam_date_mode` tinyint(1) NULL DEFAULT 0 COMMENT '0截止日期 1时间段（仅限线上）',
  `exam_date_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '考试日期',
  `exam_time` int NULL DEFAULT NULL COMMENT '考试时间(分钟)',
  `need_finish_adventure` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要先完成游戏学',
  `need_finish_normal` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要先完成常规学',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `jigou_id` int NOT NULL DEFAULT 1 COMMENT '机构id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`, `org_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 433 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_exam_list
-- ----------------------------

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
-- Records of train_exam_paper
-- ----------------------------

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
-- Records of train_exam_people_in_room
-- ----------------------------

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
-- Records of train_exam_people_list
-- ----------------------------

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
-- Records of train_exam_review
-- ----------------------------

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
-- Records of train_exam_room_list
-- ----------------------------

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
-- Records of train_exam_session
-- ----------------------------

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
-- Records of train_exam_shijuan
-- ----------------------------

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
-- Records of train_exam_staff
-- ----------------------------

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
-- Records of train_exam_staff_config
-- ----------------------------

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
-- Records of train_exam_work_type_list
-- ----------------------------

-- ----------------------------
-- Table structure for train_file_detail
-- ----------------------------
DROP TABLE IF EXISTS `train_file_detail`;
CREATE TABLE `train_file_detail`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `file_id` int NULL DEFAULT NULL COMMENT 'file_list 主键',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路径',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 311 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_file_detail
-- ----------------------------

-- ----------------------------
-- Table structure for train_file_list
-- ----------------------------
DROP TABLE IF EXISTS `train_file_list`;
CREATE TABLE `train_file_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名',
  `pk` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主键',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `user_id` int NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 699 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_file_list
-- ----------------------------

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
-- Records of train_group_ability
-- ----------------------------

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
-- Records of train_group_ability_task
-- ----------------------------

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
-- Records of train_group_album
-- ----------------------------

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
-- Records of train_group_knowledge
-- ----------------------------

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
-- Records of train_group_point_log
-- ----------------------------

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
-- Records of train_group_sign
-- ----------------------------

-- ----------------------------
-- Table structure for train_info_class
-- ----------------------------
DROP TABLE IF EXISTS `train_info_class`;
CREATE TABLE `train_info_class`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_info_id` int NOT NULL DEFAULT 0,
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级名称',
  `nums` int NOT NULL DEFAULT 0,
  `company_id` int NOT NULL,
  `work_type_id` int NOT NULL,
  `date_start` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'yyyy-mm-dd',
  `date_end` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'yyyy-mm-dd',
  `addtime` int NOT NULL,
  `file_temps` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '材料模板',
  `kcbiao_file` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训班级表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_info_class
-- ----------------------------

-- ----------------------------
-- Table structure for train_info_list
-- ----------------------------
DROP TABLE IF EXISTS `train_info_list`;
CREATE TABLE `train_info_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面',
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
  `group_type` tinyint(1) NULL DEFAULT 0 COMMENT '0培训班1技能大师工作室2企业级团队3部门级团队4班组级团队',
  `master_type` tinyint(1) NULL DEFAULT 0 COMMENT '工作室类型,0组织型团队 1学习型团队',
  `type` tinyint(1) NULL DEFAULT 0 COMMENT '0:新型学徒制培训    1:岗位技能提升',
  `cate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `company_id` int NULL DEFAULT NULL COMMENT '公司',
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `company_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `work_type_level` int NOT NULL DEFAULT 0,
  `course_id` int NULL DEFAULT NULL COMMENT '科目',
  `num` int NULL DEFAULT NULL COMMENT '人数',
  `position` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地点',
  `course_num` int NULL DEFAULT NULL COMMENT '课时',
  `head_confirm` tinyint(1) NULL DEFAULT 0 COMMENT '主任是否确认',
  `user_id` int NULL DEFAULT NULL,
  `company_has_confirm` tinyint(1) NULL DEFAULT 0,
  `company_confirm_time` timestamp NULL DEFAULT NULL COMMENT '企业确认时间',
  `company_confirm_user_id` int NULL DEFAULT NULL COMMENT '企业确认uid',
  `company_confirm_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pass_status` tinyint(1) NULL DEFAULT 0 COMMENT '是否立项',
  `check_user_id` int NULL DEFAULT NULL COMMENT '审核人',
  `check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `is_sorted_learn` tinyint(1) NULL DEFAULT 0 COMMENT '是否强制顺序学习',
  `max_normal_score` decimal(10, 2) NULL DEFAULT 50.00 COMMENT '最大平时分',
  `course_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '课程表',
  `start_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `end_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `train_company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训机构',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态:0进行中,1:待办公室主任确认,2已完成',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `point` int NULL DEFAULT 0,
  `album` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '总结',
  `province_id` int NULL DEFAULT NULL,
  `province_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `city_id` int NULL DEFAULT NULL,
  `city_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `ability_score` decimal(10, 3) NULL DEFAULT 0.000,
  `region_id` int NULL DEFAULT NULL,
  `region_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `in_charge_user_id` int NULL DEFAULT NULL,
  `in_charge_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `files` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `traindemand_id` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id` ASC) USING BTREE,
  INDEX `pass_status`(`pass_status` ASC) USING BTREE,
  INDEX `status`(`status` ASC) USING BTREE,
  INDEX `deleted`(`deleted` ASC) USING BTREE,
  INDEX `work_type_id`(`work_type_id` ASC) USING BTREE,
  INDEX `course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '开班信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_info_list
-- ----------------------------

-- ----------------------------
-- Table structure for train_need
-- ----------------------------
DROP TABLE IF EXISTS `train_need`;
CREATE TABLE `train_need`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `org_id` int NULL DEFAULT NULL,
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `train_object` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训对象',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位',
  `num` int NULL DEFAULT NULL,
  `work_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训目标',
  `method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训方式',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `train_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训时间',
  `budget` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训预算',
  `teacher_require` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '讲师要求',
  `need_exam` int NULL DEFAULT 0 COMMENT '是否考核',
  `need_match` int NULL DEFAULT 0 COMMENT '是否竞赛',
  `need_paper` int NULL DEFAULT 0 COMMENT '是否需求证书',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `json_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训需求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_need
-- ----------------------------

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
-- Records of train_need_chat
-- ----------------------------

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
-- Records of train_normal_score_log
-- ----------------------------

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
-- Records of train_offline_homework
-- ----------------------------

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
-- Records of train_offline_homework_report
-- ----------------------------

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
-- Records of train_paper_config
-- ----------------------------

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
-- Records of train_paper_template
-- ----------------------------

-- ----------------------------
-- Table structure for train_people_list
-- ----------------------------
DROP TABLE IF EXISTS `train_people_list`;
CREATE TABLE `train_people_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `train_class_id` int NOT NULL DEFAULT 0,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型:student:学员,staff:教师,inCharge:负责人',
  `user_id` int NULL DEFAULT NULL COMMENT '教师id',
  `platform_user_id` int NOT NULL DEFAULT 0,
  `realname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `is_leader` tinyint(1) NULL DEFAULT 0 COMMENT '是否班长',
  `is_committee` tinyint(1) NULL DEFAULT 0 COMMENT '是否班委',
  `exam_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '考试分数',
  `real_score` decimal(10, 1) NULL DEFAULT 0.0 COMMENT '真实分数',
  `paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证书',
  `normal_score` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '平时分',
  `national_paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '国家证书',
  `current_level` tinyint(1) NULL DEFAULT 0 COMMENT '当前等级',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1585 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '开班人员列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_people_list
-- ----------------------------

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
-- Records of train_point_config
-- ----------------------------

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
-- Records of train_position_to_work_type
-- ----------------------------

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
-- Records of train_recruit
-- ----------------------------

-- ----------------------------
-- Table structure for train_request
-- ----------------------------
DROP TABLE IF EXISTS `train_request`;
CREATE TABLE `train_request`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业名称',
  `company_id` int NULL DEFAULT NULL COMMENT '企业名称',
  `position` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训地点',
  `num` int NULL DEFAULT NULL COMMENT '培训人数',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '培训类型',
  `train_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '期望培训日期',
  `contact` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `is_on_position` int NULL DEFAULT 0 COMMENT '0脱岗培训  1在岗培训',
  `student_form` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学员名单',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训需求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_request
-- ----------------------------

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
-- Records of train_research
-- ----------------------------

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
-- Records of train_research_field
-- ----------------------------

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
-- Records of train_research_org
-- ----------------------------

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
-- Records of train_research_people_list
-- ----------------------------

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
-- Records of train_research_site
-- ----------------------------

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
-- Records of train_research_template
-- ----------------------------

-- ----------------------------
-- Table structure for train_resource
-- ----------------------------
DROP TABLE IF EXISTS `train_resource`;
CREATE TABLE `train_resource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路径',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '资源' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_resource
-- ----------------------------

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
-- Records of train_sign_up
-- ----------------------------

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
-- Records of train_sign_up_orc_self
-- ----------------------------

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
-- Records of train_sign_up_people
-- ----------------------------

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
-- Records of train_sign_up_people_self
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约报名' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_sign_up_self
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '信息收集材料导出任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_sign_up_selftask
-- ----------------------------

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
-- Records of train_sim_exam_list
-- ----------------------------

-- ----------------------------
-- Table structure for train_solution
-- ----------------------------
DROP TABLE IF EXISTS `train_solution`;
CREATE TABLE `train_solution`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_info_id` int NOT NULL DEFAULT 0,
  `company_id` int NULL DEFAULT NULL,
  `company_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `nation_policy` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '国家政策分析',
  `industry_situation_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '行业态势分析',
  `company_development_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '公司发展分析',
  `research_data_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '调研数据分析',
  `project_development_ideas` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目开展思路/培训思路',
  `main_objectives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目主要目标/培训目标',
  `main_initiatives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目主要举措',
  `development_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目开展计划、培训计划',
  `division` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目分工管理',
  `risk` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '风险管理',
  `communication` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目沟通管理',
  `checkpoint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '项目里程碑',
  `results` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '预期成效',
  `teacher_intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '讲师介绍',
  `request_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '需求分析',
  `train_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '培训内容',
  `train_method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '培训方式',
  `course` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NULL DEFAULT 0,
  `user_id` int NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_solution
-- ----------------------------

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
-- Records of train_solution_badge
-- ----------------------------

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
-- Records of train_student_chat
-- ----------------------------

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
-- Records of train_student_list
-- ----------------------------

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
-- Records of train_talent_need_chat
-- ----------------------------

-- ----------------------------
-- Table structure for train_talent_needs
-- ----------------------------
DROP TABLE IF EXISTS `train_talent_needs`;
CREATE TABLE `train_talent_needs`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `org_id` int NULL DEFAULT NULL,
  `org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `job` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位',
  `work_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工种',
  `major` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专业',
  `edu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学历',
  `gender` int NULL DEFAULT 0 COMMENT '0不限  1男  2女',
  `num` int NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地点',
  `work_year` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作年限',
  `in_position_date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '需求到岗时间',
  `paper_require` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证书要求',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `json_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `duty` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `school_need` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '人才需求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_talent_needs
-- ----------------------------

-- ----------------------------
-- Table structure for train_task_info
-- ----------------------------
DROP TABLE IF EXISTS `train_task_info`;
CREATE TABLE `train_task_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL COMMENT '培训id',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL COMMENT '课程id',
  `train_task_id` int NULL DEFAULT NULL COMMENT '任务id',
  `knowledge_id` int NULL DEFAULT NULL COMMENT '知识点id',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `is_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否必修',
  `score` int NULL DEFAULT NULL COMMENT '获得平时分',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 79 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训配置的任务列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_task_info
-- ----------------------------

-- ----------------------------
-- Table structure for train_task_info_detail
-- ----------------------------
DROP TABLE IF EXISTS `train_task_info_detail`;
CREATE TABLE `train_task_info_detail`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `task_list_id` int NULL DEFAULT NULL,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种id',
  `course_id` int NULL DEFAULT NULL COMMENT '科目id',
  `knowledge_id` int NULL DEFAULT NULL COMMENT '知识结构id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容',
  `get_learn_time` decimal(10, 1) NULL DEFAULT 0.0 COMMENT '获得学时',
  `min_minute` int NULL DEFAULT 0 COMMENT '至少学习(分钟)',
  `is_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否必修',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `score` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 156 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识点内的任务列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_task_info_detail
-- ----------------------------

-- ----------------------------
-- Table structure for train_task_list
-- ----------------------------
DROP TABLE IF EXISTS `train_task_list`;
CREATE TABLE `train_task_list`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种id',
  `course_id` int NULL DEFAULT NULL COMMENT '科目id',
  `knowledge_id` int NULL DEFAULT NULL COMMENT '知识结构id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容',
  `get_learn_time` decimal(10, 1) NULL DEFAULT 0.0 COMMENT '获得学时',
  `min_minute` int NULL DEFAULT 0 COMMENT '至少学习(分钟)',
  `is_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否必修',
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `score` int NULL DEFAULT 0,
  `get_point` decimal(10, 2) NULL DEFAULT 0.00,
  `check_status` tinyint(1) NULL DEFAULT 0 COMMENT '0待审核1通过2不通过',
  `final_check_status` tinyint(1) NULL DEFAULT 0 COMMENT '终审 0待审核1通过2不通过',
  `check_time` timestamp NULL DEFAULT NULL COMMENT '审题时间',
  `check_user_id` int NULL DEFAULT NULL COMMENT '审题用户',
  `check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '审题用户',
  `final_check_time` timestamp NULL DEFAULT NULL COMMENT '终审时间',
  `final_check_user_id` int NULL DEFAULT NULL COMMENT '终审用户',
  `final_check_username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '终审用户',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 76 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识点内的任务列表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_task_list
-- ----------------------------

-- ----------------------------
-- Table structure for train_task_log
-- ----------------------------
DROP TABLE IF EXISTS `train_task_log`;
CREATE TABLE `train_task_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_class_id` int NOT NULL DEFAULT 0,
  `ck_id` int NOT NULL DEFAULT 0 COMMENT 'course_kejian表主键',
  `date` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `addtime` int NOT NULL DEFAULT 0,
  `status` int NOT NULL DEFAULT 0,
  `times` json NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培训任务记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_task_log
-- ----------------------------

-- ----------------------------
-- Table structure for train_teacher_bank
-- ----------------------------
DROP TABLE IF EXISTS `train_teacher_bank`;
CREATE TABLE `train_teacher_bank`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证',
  `teacher_license` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '教师资格证',
  `engineer_license` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '工程师证',
  `level_license` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '技能等级证',
  `edu_license` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '学历证',
  `graduate_paper` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '毕业证',
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id_card_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_date` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `end_date` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `birthday` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `gender` tinyint(1) NULL DEFAULT NULL,
  `nation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `education` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学历',
  `major` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专业',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态, 0正常, 1冻结',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `user_id` int NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `sort_number` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 109 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '师资库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_teacher_bank
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_ability
-- ----------------------------
DROP TABLE IF EXISTS `train_user_ability`;
CREATE TABLE `train_user_ability`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `course_id` int NULL DEFAULT NULL COMMENT '科目',
  `knowledge_id` int NULL DEFAULT NULL,
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `config_id` int NULL DEFAULT NULL COMMENT '标题',
  `score` tinyint NULL DEFAULT NULL COMMENT '得分',
  `current_score` tinyint NULL DEFAULT NULL COMMENT '当前',
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `comments` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 131 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能矩阵' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_ability
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_course_paper
-- ----------------------------
DROP TABLE IF EXISTS `train_user_course_paper`;
CREATE TABLE `train_user_course_paper`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `course_id` int NULL DEFAULT NULL,
  `work_type_id` int NULL DEFAULT NULL COMMENT '工种',
  `get_year` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '获得年份',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户已获得证书' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_course_paper
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_course_plan
-- ----------------------------
DROP TABLE IF EXISTS `train_user_course_plan`;
CREATE TABLE `train_user_course_plan`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `realname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `add_from` tinyint(1) NULL DEFAULT 0 COMMENT '0用户添加 1后台添加',
  `course_id` int NULL DEFAULT NULL,
  `date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` int NOT NULL DEFAULT 0 COMMENT '0我的,1单位',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 246 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_course_plan
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_exam_log
-- ----------------------------
DROP TABLE IF EXISTS `train_user_exam_log`;
CREATE TABLE `train_user_exam_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `knowledge_id` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户刷题记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_exam_log
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_info
-- ----------------------------
DROP TABLE IF EXISTS `train_user_info`;
CREATE TABLE `train_user_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `company_id` int NULL DEFAULT NULL,
  `person_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人员类型',
  `person_supplementary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '人员补充身份',
  `house_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户口性质',
  `nationality` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '民族',
  `education` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '教育程度',
  `job_intention` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '就业意向',
  `is_party_member` tinyint(1) NULL DEFAULT 0 COMMENT '是否党员',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '家庭住址',
  `id_card_front` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证正',
  `id_card_back` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证反',
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '证件照',
  `edu_photo` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '学历证',
  `work_contract` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社保或劳动合同',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 222 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参与培训人员信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_info
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_learn_log
-- ----------------------------
DROP TABLE IF EXISTS `train_user_learn_log`;
CREATE TABLE `train_user_learn_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `train_id` int NULL DEFAULT NULL,
  `match_id` int NULL DEFAULT NULL,
  `train_task_id` int NULL DEFAULT NULL,
  `learn_time` int NULL DEFAULT NULL COMMENT '学习时间(秒)',
  `score` int NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 153 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学习记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_learn_log
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_paper
-- ----------------------------
DROP TABLE IF EXISTS `train_user_paper`;
CREATE TABLE `train_user_paper`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `platform_user_id` int NOT NULL DEFAULT 0,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除, 0否, 1是',
  `sort_number` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `tenant_id` int NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pk` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 61 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户证书' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_paper
-- ----------------------------

-- ----------------------------
-- Table structure for train_user_patent
-- ----------------------------
DROP TABLE IF EXISTS `train_user_patent`;
CREATE TABLE `train_user_patent`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `paper` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` int NULL DEFAULT NULL,
  `deleted` tinyint(1) NULL DEFAULT 0,
  `tenant_id` int NOT NULL DEFAULT 1 COMMENT '租户id',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专利' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of train_user_patent
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
