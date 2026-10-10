-- Add exam_task_ximu_expert table for tracking expert assignments
CREATE TABLE `exam_task_ximu_expert` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_id` int(11) NOT NULL COMMENT '关联 exam_task_ximu.id',
  `expert_id` int(11) NOT NULL COMMENT '关联 sys_user.id',
  `task_role` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=初审 1=终审',
  `tag` varchar(20) NOT NULL DEFAULT 'ximu' COMMENT '任务类型标识',
  `addtime` datetime DEFAULT NULL COMMENT '指派时间',
  `endtime` datetime DEFAULT NULL COMMENT '任务完成时间',
  `tenant_id` bigint(11) NOT NULL DEFAULT '1' COMMENT '租户id',
  `deleted` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除, 0否, 1是',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL,
  `created_by` int(11) DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assign` (`tenant_id`, `task_id`, `expert_id`, `task_role`, `tag`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='细目表任务专家指派记录';
