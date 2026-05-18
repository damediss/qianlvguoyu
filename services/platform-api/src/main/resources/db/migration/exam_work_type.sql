/*
 Navicat Premium Dump SQL

 Source Server         : 114.132.67.185 - root
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44-log)
 Source Host           : 114.132.67.185:3308
 Source Schema         : gjjddb

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44-log)
 File Encoding         : 65001

 Date: 15/05/2026 15:24:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for exam_work_type
-- ----------------------------
DROP TABLE IF EXISTS `exam_work_type`;
CREATE TABLE `exam_work_type`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `series_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职业编码',
  `levels_no` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '级别',
  `parent_id` int(11) NULL DEFAULT NULL COMMENT '上级ID',
  `user_id` int(11) NULL DEFAULT NULL,
  `deleted` int(1) NULL DEFAULT 0 COMMENT '1已删除，0正常',
  `status` int(1) NOT NULL DEFAULT 1 COMMENT '状态',
  `sort_number` int(11) NOT NULL DEFAULT 1 COMMENT '排序号，降序排序',
  `tenant_id` int(11) NOT NULL DEFAULT 10159 COMMENT '租户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`id`) USING BTREE,
  INDEX `parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 136 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工种' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_work_type
-- ----------------------------
INSERT INTO `exam_work_type` VALUES (3, '母液蒸发工', '', '', 22, 3742, 0, 1, 100, 10159, '2024-07-26 11:44:42');
INSERT INTO `exam_work_type` VALUES (8, '氢氧化铝分解工', NULL, '', 22, 3742, 0, 1, 100, 10159, '2024-07-26 11:45:07');
INSERT INTO `exam_work_type` VALUES (11, '多功能机组操作工', '', '', 18, 3742, 0, 1, 100, 10159, '2024-07-26 11:33:51');
INSERT INTO `exam_work_type` VALUES (15, '铝电解槽操作工', NULL, '', 18, 3742, 0, 1, 100, 10159, '2024-07-26 11:40:43');
INSERT INTO `exam_work_type` VALUES (18, '铝电解工', '6-17-06-02', NULL, '', 3742, 0, 1, 100, 10159, '2024-07-26 11:42:46');
INSERT INTO `exam_work_type` VALUES (22, '氧化铝制取工', '6-17-06-01', NULL, '', 3742, 0, 1, 100, 10159, '2024-07-26 11:44:13');
INSERT INTO `exam_work_type` VALUES (27, '金属轧制工', '6-17-09-02', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-03 13:50:55');
INSERT INTO `exam_work_type` VALUES (28, '车轮轧制工', NULL, '', 27, 3742, 0, 1, 100, 10159, '2024-09-03 13:51:40');
INSERT INTO `exam_work_type` VALUES (29, '重冶湿法冶炼工', '6-17-05-04', NULL, '', 4376, 0, 1, 100, 10159, '2024-09-05 09:49:17');
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
INSERT INTO `exam_work_type` VALUES (52, '井下支护工', '6-16-01-06', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-09 09:16:30');
INSERT INTO `exam_work_type` VALUES (53, '矿山救护工', '6-16-01-12', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-09 09:17:22');
INSERT INTO `exam_work_type` VALUES (54, '重冶火法冶炼工', '6-17-05-03', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-09 09:18:05');
INSERT INTO `exam_work_type` VALUES (55, '电解精炼工', '6-17-05-05', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-09 09:18:47');
INSERT INTO `exam_work_type` VALUES (56, '重冶配液工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:19:25');
INSERT INTO `exam_work_type` VALUES (57, '重冶净液工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:01');
INSERT INTO `exam_work_type` VALUES (58, '阴阳极制作工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:26');
INSERT INTO `exam_work_type` VALUES (59, '电解槽操作工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:20:52');
INSERT INTO `exam_work_type` VALUES (60, '电解熔铸工', NULL, '', 55, 3742, 0, 1, 100, 10159, '2024-09-09 09:21:13');
INSERT INTO `exam_work_type` VALUES (69, '金属挤压工', '6-17-09-09', NULL, '', 3742, 0, 1, 100, 10159, '2024-09-09 09:31:25');
INSERT INTO `exam_work_type` VALUES (70, '有色挤压工', NULL, '', 69, 3742, 0, 1, 100, 10159, '2024-09-09 09:31:51');
INSERT INTO `exam_work_type` VALUES (71, '挤压修模工', NULL, '', 69, 3742, 0, 1, 100, 10159, '2024-09-09 09:32:20');
INSERT INTO `exam_work_type` VALUES (76, '重冶火法冶炼工', NULL, '', 54, 3742, 0, 1, 100, 10159, '2024-10-17 16:52:18');
INSERT INTO `exam_work_type` VALUES (107, '矿山救护工', '6-16-01-12', '', 53, 26743, 0, 1, 1, 10159, '2025-03-14 08:37:32');
INSERT INTO `exam_work_type` VALUES (117, '氧化铝制取工', '6-17-06-02', '', 22, 3742, 0, 1, 100, 10159, '2025-06-18 16:26:42');
INSERT INTO `exam_work_type` VALUES (125, '重冶配液工', '', 55, NULL, '', 0, 1, 1, 10159, '2025-12-26 09:51:40');
INSERT INTO `exam_work_type` VALUES (126, '金属材热处理工', '9-11-01-01', '', 0, NULL, 0, 1, 1, 10159, '2025-12-29 11:31:53');
INSERT INTO `exam_work_type` VALUES (127, '有色金属材热处理工', '6-17-09-05', '', 126, NULL, 0, 1, 1, 10159, '2025-12-29 11:34:21');
INSERT INTO `exam_work_type` VALUES (128, '井下支护工', '6-16-01-06', '', 52, NULL, 0, 1, 1, 10159, '2025-12-30 11:06:23');
INSERT INTO `exam_work_type` VALUES (133, '金属材精整工', '6-17-09-07', '', 0, NULL, 0, 1, 1, 10159, '2025-12-30 11:15:47');
INSERT INTO `exam_work_type` VALUES (134, '管棒型材精整工', '6-17-09-07', '', 133, NULL, 0, 1, 1, 10159, '2025-12-30 11:16:33');
INSERT INTO `exam_work_type` VALUES (135, '板带箔材精整工', '6-17-09-07', '', 133, NULL, 0, 1, 1, 10159, '2025-12-30 11:24:09');

SET FOREIGN_KEY_CHECKS = 1;
