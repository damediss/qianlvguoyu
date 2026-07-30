-- ===========================================================================
-- 题库审核指派 三子菜单 - PHP 老数据种子
-- 日期: 2026-06-02
--
-- 每题型 2-3 个 task,每个 task 5-10 条 detail,字段对齐 V20260528_006 + V20260602_001
-- 工种使用 Phase1 已有的 exam_work_type (id=1~3),租户 id=1 (demo)
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 理论任务 (exam_task_exam) - 3 个 task
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exam_task_exam
  (id, tenant_id, work_type_id, level, status, expert_start, expert_end, file_path,
   nums, addtime, create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, 1, 0, NULL, NULL, NULL, 5, '2026-05-15 09:00:00', NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, 2, 1, 2,    NULL, NULL, 6, '2026-05-16 10:00:00', NOW(), NOW(), 1, 1, 0),
  (3, 1, 2, 1, 2, 3,    4,    NULL, 5, '2026-05-17 11:00:00', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_exam_detail
  (id, tenant_id, task_id, xingwei, kaohe1, kaohe2, kaohe3, kaohe4, jianding,
   level, nanyicd, exam_type, exam_content, exam_a, exam_b, exam_c, exam_d,
   exam_e, exam_f, exam_g, exam_h, exam_answer, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, '安全检查', '穿戴', '工具', '环境', '应急', 'JD-001-1', 1, '易', '单选题',
   '电工上岗前必须佩戴的个人防护用品是？',
   '安全帽', '绝缘鞋', '防护手套', '以上都是', '', '', '', '', 'D', 0,
   NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, '安全检查', '穿戴', '工具', '环境', '应急', 'JD-001-2', 1, '易', '判断题',
   '在潮湿环境下可以使用 220V 普通电工工具。',
   '正确', '错误', '', '', '', '', '', '', 'B', 0,
   NOW(), NOW(), 1, 1, 0),
  (3, 1, 1, '安全检查', '穿戴', '工具', '环境', '应急', 'JD-001-3', 1, '中', '多选题',
   '触电急救的主要步骤包括哪些？',
   '脱离电源', '判断伤情', '呼叫 120', '心肺复苏', '以上都是', '', '', '', 'ABCD', 1,
   NOW(), NOW(), 1, 1, 0),
  (4, 1, 1, '操作规范', '接线', '通电', '测试', '收工', 'JD-002-1', 1, '中', '单选题',
   '三相四线制中，零线的主要作用是？',
   '传输电能', '保护接地', '提供回路', '节省导线', '', '', '', '', 'C', 0,
   NOW(), NOW(), 1, 1, 0),
  (5, 1, 1, '操作规范', '接线', '通电', '测试', '收工', 'JD-002-2', 1, '难', '单选题',
   '低压电器设备的工作电压上限通常为多少？',
   '220V', '380V', '1000V', '1500V', '', '', '', '', 'C', 0,
   NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_exam_detail
  (id, tenant_id, task_id, xingwei, kaohe1, kaohe2, kaohe3, kaohe4, jianding,
   level, nanyicd, exam_type, exam_content, exam_a, exam_b, exam_c, exam_d,
   exam_e, exam_f, exam_g, exam_h, exam_answer, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (6, 1, 2, '识图', '主电路', '控制电路', '保护电路', '信号电路', 'JD-101-1', 2, '中', '单选题',
   '在电气控制原理图中,常开触点的图形符号是？',
   '一条斜线穿过横线', '两条平行横线', '一个圆圈', '一个三角', '', '', '', '', 'A', 0,
   NOW(), NOW(), 1, 1, 0),
  (7, 1, 2, '识图', '主电路', '控制电路', '保护电路', '信号电路', 'JD-101-2', 2, '难', '多选题',
   'PLC 输入端常见的接线方式有？',
   '源型输入', '漏型输入', '差分输入', '交流输入', '直流输入', '', '', '', 'AB', 0,
   NOW(), NOW(), 1, 1, 0),
  (8, 1, 2, '接线', '端子', '线号', '压接', '紧固', 'JD-102-1', 2, '中', '判断题',
   '接线端子松动可能导致设备误动作。',
   '正确', '错误', '', '', '', '', '', '', 'A', 1,
   NOW(), NOW(), 1, 1, 0),
  (9, 1, 2, '调试', '通电', '空载', '负载', '记录', 'JD-103-1', 2, '中', '单选题',
   '电机空载试运行时间一般不少于多少分钟？',
   '5', '15', '30', '60', '', '', '', '', 'C', 0,
   NOW(), NOW(), 1, 1, 0),
  (10, 1, 2, '调试', '通电', '空载', '负载', '记录', 'JD-103-2', 2, '难', '单选题',
   '变频器过流故障的常见原因是？',
   '参数错误', '负载过重', '电机短路', '以上都是', '', '', '', '', 'D', 0,
   NOW(), NOW(), 1, 1, 0),
  (11, 1, 2, '维护', '清洁', '紧固', '润滑', '更换', 'JD-104-1', 2, '中', '多选题',
   '电机日常维护的内容包括？',
   '清洁外观', '检查温度', '紧固螺栓', '更换轴承', '', '', '', '', 'ABC', 0,
   NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_exam_detail
  (id, tenant_id, task_id, xingwei, kaohe1, kaohe2, kaohe3, kaohe4, jianding,
   level, nanyicd, exam_type, exam_content, exam_a, exam_b, exam_c, exam_d,
   exam_e, exam_f, exam_g, exam_h, exam_answer, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (12, 1, 3, '故障诊断', '观察', '测量', '分析', '处理', 'JD-201-1', 1, '难', '单选题',
   '用万用表测量二极管时,正向阻值一般在多少？',
   '0', '几百欧', '几千欧', '无穷大', '', '', '', '', 'B', 0,
   NOW(), NOW(), 1, 1, 0),
  (13, 1, 3, '故障诊断', '观察', '测量', '分析', '处理', 'JD-201-2', 1, '难', '判断题',
   '示波器探头接地夹可以随意接在被测电路的任意点。',
   '正确', '错误', '', '', '', '', '', '', 'B', 1,
   NOW(), NOW(), 1, 1, 0),
  (14, 1, 3, '故障诊断', '观察', '测量', '分析', '处理', 'JD-201-3', 1, '中', '单选题',
   '绝缘电阻表的常用电压等级不包括？',
   '250V', '500V', '1000V', '5000V', '', '', '', '', 'D', 0,
   NOW(), NOW(), 1, 1, 0),
  (15, 1, 3, '故障诊断', '观察', '测量', '分析', '处理', 'JD-201-4', 1, '中', '多选题',
   '电力电缆常见故障类型有？',
   '接地故障', '短路故障', '断线故障', '闪络故障', '', '', '', '', 'ABCD', 0,
   NOW(), NOW(), 1, 1, 0),
  (16, 1, 3, '故障诊断', '观察', '测量', '分析', '处理', 'JD-201-5', 1, '难', '单选题',
   '查找电缆故障点的常用方法是？',
   '电桥法', '声测法', '声磁同步法', '以上都是', '', '', '', '', 'D', 0,
   NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 实操任务 (exam_task_examshicao) - 2 个 task
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exam_task_examshicao
  (id, tenant_id, work_type_id, level, status, expert_start, expert_end, file_path,
   nums, addtime, create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, 1, 0, NULL, NULL, NULL, 4, '2026-05-18 09:00:00', NOW(), NOW(), 1, 1, 0),
  (2, 1, 2, 2, 1, 5,    NULL, NULL, 3, '2026-05-19 10:00:00', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_examshicao_detail
  (id, tenant_id, task_id, xingwei, kaohe1, kaohe2, kaohe3, kaohe4, jianding,
   nanyicd, content, pingfen, kaochang, kaosheng, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, '操作前', '检查', '准备', '记录', '交接', 'SC-001-1', '中',
   '请在三相异步电动机控制电路中,完成正反转控制线路的安装与调试。',
   '[{"item":"接线工艺","score":20},{"item":"通电测试","score":30},{"item":"功能验证","score":30},{"item":"安全规范","score":20}]',
   '准备三相异步电动机、接触器、按钮、导线、万用表等。',
   '穿戴绝缘鞋、防护手套,熟悉电路图。', 0,
   NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, '操作中', '接线', '检查', '通电', '记录', 'SC-001-2', '难',
   '请完成星三角降压启动控制电路的接线与参数整定。',
   '[{"item":"主电路接线","score":25},{"item":"控制电路接线","score":25},{"item":"时间继电器整定","score":25},{"item":"通电测试","score":25}]',
   '时间继电器、接触器、热继电器、按钮。',
   '检查绝缘,确认接地。', 1,
   NOW(), NOW(), 1, 1, 0),
  (3, 1, 1, '操作后', '收尾', '清洁', '记录', '交接', 'SC-001-3', '中',
   '请完成 PLC 控制的三相异步电动机正反转控制程序编写与调试。',
   '[{"item":"I/O 分配","score":20},{"item":"梯形图","score":40},{"item":"联机调试","score":30},{"item":"安全规范","score":10}]',
   'PLC 主机、输入输出模块、计算机、编程电缆。',
   '断电接线,完成后通电调试。', 0,
   NOW(), NOW(), 1, 1, 0),
  (4, 1, 1, '故障', '诊断', '处理', '测试', '记录', 'SC-001-4', '难',
   '请诊断并排除三相异步电动机不能启动故障。',
   '[{"item":"故障分析","score":30},{"item":"测量过程","score":30},{"item":"故障处理","score":30},{"item":"结果验证","score":10}]',
   '故障电动机一台、万用表、兆欧表、示波器。',
   '穿戴防护用品,熟悉安全规程。', 0,
   NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_examshicao_detail
  (id, tenant_id, task_id, xingwei, kaohe1, kaohe2, kaohe3, kaohe4, jianding,
   nanyicd, content, pingfen, kaochang, kaosheng, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (5, 1, 2, '识图', '图纸', '元件', '布线', '标识', 'SC-101-1', '中',
   '请根据电气原理图,完成 PLC 控制柜的元件布局与布线。',
   '[{"item":"元件布局","score":25},{"item":"布线工艺","score":35},{"item":"标识规范","score":20},{"item":"通电测试","score":20}]',
   '电气原理图、PLC 柜、导线、线槽、号码管。',
   '熟悉图纸,清点元件。', 0,
   NOW(), NOW(), 1, 1, 0),
  (6, 1, 2, '编程', '分配', '程序', '调试', '记录', 'SC-101-2', '难',
   '请使用 SFC 编程方法完成自动门控制程序设计。',
   '[{"item":"I/O 分配","score":20},{"item":"SFC 流程","score":40},{"item":"调试运行","score":30},{"item":"安全互锁","score":10}]',
   'PLC、计算机、编程软件、模拟输入输出装置。',
   '理解工艺要求,设计安全互锁。', 0,
   NOW(), NOW(), 1, 1, 0),
  (7, 1, 2, '调试', '单体', '联动', '优化', '记录', 'SC-101-3', '中',
   '请完成自动化生产线多工位控制的联机调试。',
   '[{"item":"信号测试","score":25},{"item":"逻辑验证","score":35},{"item":"故障排除","score":25},{"item":"优化建议","score":15}]',
   '整套自动化生产线模拟装置。',
   '熟悉工艺流程,准备好记录表格。', 0,
   NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 主观任务 (exam_task_examsub) - 2 个 task
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exam_task_examsub
  (id, tenant_id, work_type_id, level, status, expert_start, expert_end, file_path,
   nums, addtime, create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, 1, 0, NULL, NULL, NULL, 3, '2026-05-20 09:00:00', NOW(), NOW(), 1, 1, 0),
  (2, 1, 2, 1, 0, NULL, NULL, NULL, 3, '2026-05-21 10:00:00', NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_examsub_detail
  (id, tenant_id, task_id, zhishidian, content, answer, pic, isJisuan, nanyicd, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 1, 1, '安全规程', '请简述电工岗位的安全操作规程,以及发生触电事故时的应急处理步骤。',
   '1. 持证上岗,穿戴防护用品;2. 作业前检查工具和环境;3. 严格执行停电、验电、挂牌、接地;4. 触电急救:脱离电源、判断伤情、心肺复苏、呼叫 120。',
   NULL, 0, '中', 0,
   NOW(), NOW(), 1, 1, 0),
  (2, 1, 1, '电路分析', '请分析三相异步电动机正反转控制电路的工作原理,说明互锁的作用。',
   '正反转通过改变电源相序实现;KM1、KM2 两个接触器互锁,防止主触点同时闭合造成短路;按钮互锁和接触器互锁构成双重保护。',
   NULL, 0, '中', 1,
   NOW(), NOW(), 1, 1, 0),
  (3, 1, 1, '故障诊断', '请描述三相异步电动机不能启动的常见故障原因及排查方法。',
   '常见原因:电源缺相、控制电路断路、热继电器动作、接触器线圈损坏、电机本体故障。排查方法:先查电源,再查控制电路,最后查电机本体。',
   NULL, 0, '难', 0,
   NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO exam_task_examsub_detail
  (id, tenant_id, task_id, zhishidian, content, answer, pic, isJisuan, nanyicd, isblod,
   create_time, updated_at, created_by, updated_by, deleted)
VALUES
  (4, 1, 2, 'PLC 编程', '请说明 PLC 扫描周期的工作过程,以及影响扫描周期长短的因素。',
   'PLC 工作过程分三阶段:输入采样、程序执行、输出刷新。影响因素:程序长短、I/O 点数、指令类型、通讯任务等。',
   NULL, 0, '中', 0,
   NOW(), NOW(), 1, 1, 0),
  (5, 1, 2, '计算题', '某三相异步电动机额定功率 Pn=7.5kW,效率 η=0.85,功率因数 cosφ=0.85,求额定电流 In。',
   'In = Pn / (√3 × U × η × cosφ) = 7500 / (1.732 × 380 × 0.85 × 0.85) ≈ 15.8 A',
   NULL, 1, '中', 1,
   NOW(), NOW(), 1, 1, 0),
  (6, 1, 2, '工艺规范', '请简述电气设备日常维护的"四项要求"和"五项纪律"。',
   '四项要求:整齐、清洁、润滑、安全。五项纪律:凭证操作、巡回检查、记录数据、隐患处理、交接清楚。',
   NULL, 0, '易', 0,
   NOW(), NOW(), 1, 1, 0);

-- ---------------------------------------------------------------------------
-- 预置 expert 分配记录 (与任务 status 推进演示配套)
-- task 2 (exam) 已指派初审 expert_id=2
-- task 3 (exam) 已指派初审 expert_id=3 + 终审 expert_id=4
-- task 2 (examshicao) 已指派初审 expert_id=5
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO exam_task_expert
  (tenant_id, task_id, task_type, expert_id, task_role, tag, addtime,
   created_at, updated_at, created_by, updated_by, deleted)
VALUES
  (1, 2, 'exam',        2, 0, 'exam',        '2026-05-16 10:05:00', NOW(), NOW(), 1, 1, 0),
  (1, 3, 'exam',        3, 0, 'exam',        '2026-05-17 11:05:00', NOW(), NOW(), 1, 1, 0),
  (1, 3, 'exam',        4, 1, 'exam',        '2026-05-17 11:10:00', NOW(), NOW(), 1, 1, 0),
  (1, 2, 'examshicao',  5, 0, 'examshicao',  '2026-05-19 10:05:00', NOW(), NOW(), 1, 1, 0);
