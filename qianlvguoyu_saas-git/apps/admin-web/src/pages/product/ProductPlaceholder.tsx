import React from 'react';
import { Typography, Card, Divider, Tag } from 'antd';
import {
  BulbOutlined, BookOutlined, DatabaseOutlined, GlobalOutlined,
  ExperimentOutlined, HomeOutlined, TeamOutlined, SafetyCertificateOutlined,
  TrophyOutlined, FormOutlined, RobotOutlined, ReadOutlined,
  DesktopOutlined, UserSwitchOutlined, PlayCircleOutlined, CheckCircleOutlined,
  FileProtectOutlined, SolutionOutlined, GroupOutlined, ToolOutlined,
  RocketOutlined, SketchOutlined, AimOutlined, CloudOutlined,
  ScheduleOutlined, DollarOutlined, ApartmentOutlined, FieldTimeOutlined,
  CalendarOutlined, SendOutlined, CustomerServiceOutlined, SwapOutlined,
  TagOutlined, StarOutlined, HistoryOutlined, BellOutlined, UsergroupAddOutlined,
  IdcardOutlined, NodeIndexOutlined, ShopOutlined, FileTextOutlined,
  AccountBookOutlined, AuditOutlined, ThunderboltOutlined, GiftOutlined,
  QrcodeOutlined, ShareAltOutlined, FunnelPlotOutlined, LineChartOutlined,
  FundOutlined, DashboardOutlined, FolderOpenOutlined, SearchOutlined, FileAddOutlined,
  EditOutlined, ExportOutlined, SoundOutlined, BankOutlined, FlagOutlined, RiseOutlined,
  SafetyOutlined, EnvironmentOutlined, InboxOutlined, NotificationOutlined, CommentOutlined,
  CrownOutlined, SecurityScanOutlined,
} from '@ant-design/icons';

const { Title, Paragraph, Text } = Typography;

interface ModuleInfo {
  code: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  category: string;
}

const MODULE_MAP: Record<string, ModuleInfo> = {
  // 产品规划
  product_ai_industry:        { code: 'product_ai_industry',        name: 'AI规划（行业）',          description: '面向有色金属行业的 AI 战略规划与方案设计',           icon: <BulbOutlined />,           category: 'AI规划' },
  product_ai_college:         { code: 'product_ai_college',         name: 'AI规划（院校）',          description: '面向职业院校/高校的 AI 教学与实训规划',             icon: <BulbOutlined />,           category: 'AI规划' },
  product_ai_enterprise:      { code: 'product_ai_enterprise',      name: 'AI规划（企业）',          description: '面向企业端的 AI 能力建设与应用规划',                icon: <BulbOutlined />,           category: 'AI规划' },
  product_ai_individual:      { code: 'product_ai_individual',      name: 'AI规划（个人）',          description: '面向个人学习者的 AI 技能提升路径规划',              icon: <BulbOutlined />,           category: 'AI规划' },
  product_course_dev:         { code: 'product_course_dev',         name: '课程开发系统',            description: '课程内容生产与管理平台',                            icon: <BookOutlined />,           category: '平台/基础设施' },
  product_knowledge_nonferrous:{ code: 'product_knowledge_nonferrous', name: '有色培鉴赛知识库',       description: '有色金属行业培训、鉴定、竞赛专业知识库',            icon: <DatabaseOutlined />,       category: '平台/基础设施' },
  product_qianlv_ecosystem:   { code: 'product_qianlv_ecosystem',   name: '千铝技能生态系统',        description: '千铝品牌技能生态全景平台',                          icon: <GlobalOutlined />,         category: '平台/基础设施' },
  product_simulation:         { code: 'product_simulation',         name: '培鉴赛仿真平台',          description: '培训、鉴定、竞赛全流程仿真模拟平台',                icon: <ExperimentOutlined />,     category: '平台/基础设施' },
  product_training_base:      { code: 'product_training_base',      name: '培训基地运营系统',        description: '培训基地日常运营管理',                             icon: <HomeOutlined />,           category: '运营系统' },
  product_apprentice:         { code: 'product_apprentice',         name: '新型学徒运营系统',        description: '新型学徒制培训全流程管理',                         icon: <TeamOutlined />,           category: '运营系统' },
  product_cert_agency:        { code: 'product_cert_agency',        name: '自主认定机构运营系统',    description: '技能等级自主认定机构运营管理',                     icon: <SafetyCertificateOutlined />, category: '运营系统' },
  product_competition:        { code: 'product_competition',        name: '国行省市赛运营系统',      description: '国家级、行业、省市级竞赛全流程运营',                icon: <TrophyOutlined />,         category: '运营系统' },
  product_studio_apply:       { code: 'product_studio_apply',       name: '工作室申报运营系统',      description: '技能大师工作室等申报与管理',                       icon: <FormOutlined />,           category: '运营系统' },
  product_course_ops:         { code: 'product_course_ops',         name: '课程开发运营系统',        description: '课程开发项目运营与协作管理',                       icon: <BookOutlined />,           category: '运营系统' },
  product_ai_nonferrous_assess:{ code: 'product_ai_nonferrous_assess', name: '有色考评AI智能体',       description: '有色金属行业 AI 驱动的智能考评系统',               icon: <RobotOutlined />,          category: 'AI能力/智能体' },
  product_qianlv_knowledge:   { code: 'product_qianlv_knowledge',   name: '千铝知识库',              description: '千铝品牌通用知识库与知识管理',                     icon: <ReadOutlined />,           category: 'AI能力/智能体' },
  product_smart_classroom:    { code: 'product_smart_classroom',    name: '有色智慧教室',            description: '有色金属行业智慧教学场景解决方案',                 icon: <DesktopOutlined />,        category: 'AI能力/智能体' },
  product_expert_system:      { code: 'product_expert_system',      name: '职责能力建设专家库系统',  description: '专家资源库与职责能力模型管理',                     icon: <UserSwitchOutlined />,     category: 'AI能力/智能体' },
  product_ai_learning:        { code: 'product_ai_learning',        name: 'AI学（同步精准专项学）',  description: 'AI 驱动的个性化精准学习系统',                     icon: <PlayCircleOutlined />,     category: 'AI能力/智能体' },
  product_ai_assess:          { code: 'product_ai_assess',          name: 'AI考评',                  description: 'AI 辅助技能考评与评估',                            icon: <CheckCircleOutlined />,    category: 'AI能力/智能体' },
  product_ai_agency_apply:    { code: 'product_ai_agency_apply',    name: 'AI机构申报',              description: 'AI 辅助机构认定申报流程',                         icon: <FileProtectOutlined />,    category: 'AI能力/智能体' },
  product_ai_individual_apply: { code: 'product_ai_individual_apply', name: 'AI个人申报',             description: 'AI 辅助个人技能认定申报',                          icon: <SolutionOutlined />,       category: 'AI能力/智能体' },
  product_ai_class_manage:    { code: 'product_ai_class_manage',    name: 'AI班级管理',              description: 'AI 智能班级管理与教学辅助',                       icon: <GroupOutlined />,          category: 'AI能力/智能体' },
  product_ai_skill_manage:    { code: 'product_ai_skill_manage',    name: 'AI技能管理',              description: 'AI 驱动的技能图谱与能力管理',                     icon: <ToolOutlined />,           category: 'AI能力/智能体' },

  // 人才规划
  talent_three_year:      { code: 'talent_three_year',      name: '三年规划',             description: '人才三年发展规划与目标',                     icon: <FieldTimeOutlined />,        category: '人才规划' },
  talent_five_year:       { code: 'talent_five_year',       name: '五年规划',             description: '人才五年发展规划与目标',                     icon: <FieldTimeOutlined />,        category: '人才规划' },

  // 业务规划
  biz_product_design:     { code: 'biz_product_design',     name: '产品设计部工作规划',   description: '产品设计部年度工作规划与目标',               icon: <SketchOutlined />,           category: '业务规划' },
  biz_market_expand:      { code: 'biz_market_expand',      name: '市场拓展部工作规划',   description: '市场拓展部年度工作规划与目标',               icon: <AimOutlined />,              category: '业务规划' },
  biz_course_dev:         { code: 'biz_course_dev',         name: '课程开发部工作规划',   description: '课程开发部年度工作规划与目标',               icon: <BookOutlined />,             category: '业务规划' },
  biz_agent_dev:          { code: 'biz_agent_dev',          name: '智能体开发部工作规划', description: '智能体开发部年度工作规划与目标',             icon: <RobotOutlined />,            category: '业务规划' },
  biz_cloud_dev:          { code: 'biz_cloud_dev',          name: '云平台开发部工作规划', description: '云平台开发部年度工作规划与目标',             icon: <CloudOutlined />,            category: '业务规划' },
  biz_simulation_dev:     { code: 'biz_simulation_dev',     name: '仿真开发部工作规划',   description: '仿真开发部年度工作规划与目标',               icon: <ExperimentOutlined />,       category: '业务规划' },
  biz_training_ops:       { code: 'biz_training_ops',       name: '培训运营工作规划',     description: '培训运营年度工作规划与目标',                 icon: <ScheduleOutlined />,         category: '业务规划' },
  biz_cert_ops:           { code: 'biz_cert_ops',           name: '认定运营工作规划',     description: '认定运营年度工作规划与目标',                 icon: <SafetyCertificateOutlined />, category: '业务规划' },
  biz_competition_ops:    { code: 'biz_competition_ops',    name: '竞赛运营工作规划',     description: '竞赛运营年度工作规划与目标',                 icon: <TrophyOutlined />,           category: '业务规划' },
  biz_hr:                 { code: 'biz_hr',                 name: '人力资源工作规划',     description: '人力资源部年度工作规划与目标',              icon: <TeamOutlined />,             category: '业务规划' },
  biz_finance:            { code: 'biz_finance',            name: '业财融工作规划',       description: '业财融部门年度工作规划与目标',              icon: <DollarOutlined />,           category: '业务规划' },
  biz_admin:              { code: 'biz_admin',              name: '行政后勤工作规划',     description: '行政后勤部年度工作规划与目标',              icon: <HomeOutlined />,             category: '业务规划' },
  biz_platform_coop:      { code: 'biz_platform_coop',      name: '平台共建部工作规划',   description: '平台共建部年度工作规划与目标',              icon: <ApartmentOutlined />,        category: '业务规划' },

  // 工作计划
  plan_work_year:      { code: 'plan_work_year',      name: '年度计划',             description: '年度工作计划',                     icon: <CalendarOutlined />,        category: '计划管理' },
  plan_work_quarter:   { code: 'plan_work_quarter',   name: '季度计划',             description: '季度工作计划',                     icon: <CalendarOutlined />,        category: '计划管理' },
  plan_work_month:     { code: 'plan_work_month',     name: '月度计划',             description: '月度工作计划',                     icon: <CalendarOutlined />,        category: '计划管理' },

  // 培训计划
  plan_train_year:     { code: 'plan_train_year',     name: '年度计划',             description: '年度培训计划',                     icon: <CalendarOutlined />,        category: '计划管理' },
  plan_train_quarter:  { code: 'plan_train_quarter',  name: '季度计划',             description: '季度培训计划',                     icon: <CalendarOutlined />,        category: '计划管理' },
  plan_train_month:    { code: 'plan_train_month',    name: '月度计划',             description: '月度培训计划',                     icon: <CalendarOutlined />,        category: '计划管理' },

  // 运营流程 - 线索招生
  ops_lead_collect:    { code: 'ops_lead_collect',    name: '线索收集',             description: '多渠道线索收集与录入',                     icon: <AimOutlined />,             category: '运营流程' },
  ops_lead_assign:     { code: 'ops_lead_assign',     name: '线索分配',             description: '线索自动/手动分配至销售',                   icon: <SendOutlined />,            category: '运营流程' },
  ops_lead_follow:     { code: 'ops_lead_follow',     name: '客户跟进',             description: '客户跟进记录与进度追踪',                     icon: <CustomerServiceOutlined />, category: '运营流程' },
  ops_lead_convert:    { code: 'ops_lead_convert',    name: '报名转化',             description: '意向客户报名转化管理',                       icon: <SwapOutlined />,            category: '运营流程' },

  // 运营流程 - 客户管理
  ops_customer_archive: { code: 'ops_customer_archive', name: '客户档案',            description: '客户基本信息档案管理',                       icon: <IdcardOutlined />,          category: '运营流程' },
  ops_customer_tag:    { code: 'ops_customer_tag',    name: '标签画像',             description: '客户标签体系与用户画像',                     icon: <TagOutlined />,             category: '运营流程' },
  ops_customer_intent: { code: 'ops_customer_intent', name: '意向分级',             description: '客户意向等级评估与分级管理',                 icon: <StarOutlined />,            category: '运营流程' },
  ops_customer_visit:  { code: 'ops_customer_visit',  name: '回访记录',             description: '客户回访记录与满意度跟踪',                   icon: <HistoryOutlined />,         category: '运营流程' },
  ops_customer_remind: { code: 'ops_customer_remind', name: '跟进提醒',             description: '客户跟进任务提醒与日程管理',                 icon: <BellOutlined />,            category: '运营流程' },

  // 运营流程 - 订单财务
  ops_order_center:    { code: 'ops_order_center',    name: '订单中心（2期）',       description: '订单全生命周期管理',                       icon: <ShopOutlined />,            category: '运营流程' },
  ops_contract_mgmt:   { code: 'ops_contract_mgmt',   name: '合同管理（2期）',       description: '合同签订与执行管理',                       icon: <FileTextOutlined />,        category: '运营流程' },
  ops_receipt_record:  { code: 'ops_receipt_record',  name: '收款记录（2期）',       description: '收款记录与核销管理',                       icon: <AccountBookOutlined />,     category: '运营流程' },
  ops_invoice_mgmt:    { code: 'ops_invoice_mgmt',    name: '开票管理（2期）',       description: '发票开具与管理',                           icon: <FileProtectOutlined />,     category: '运营流程' },
  ops_settlement:      { code: 'ops_settlement',      name: '对账结算（2期）',       description: '财务对账与结算管理',                       icon: <AuditOutlined />,           category: '运营流程' },

  // 运营流程 - 营销推广
  ops_enroll_brochure: { code: 'ops_enroll_brochure', name: '招生简章（2期）',       description: '招生简章制作与发布',                       icon: <FileTextOutlined />,        category: '运营流程' },
  ops_activity_mkt:    { code: 'ops_activity_mkt',    name: '活动营销（2期）',       description: '营销活动策划与管理',                       icon: <ThunderboltOutlined />,     category: '运营流程' },
  ops_coupon_card:     { code: 'ops_coupon_card',     name: '优惠卡券（2期）',       description: '优惠券与卡券管理',                         icon: <GiftOutlined />,            category: '运营流程' },
  ops_channel_qrcode:  { code: 'ops_channel_qrcode',  name: '渠道活码（2期）',       description: '渠道推广活码管理',                         icon: <QrcodeOutlined />,          category: '运营流程' },
  ops_distribution:    { code: 'ops_distribution',    name: '分销裂变（2期）',       description: '分销裂变与推广管理',                       icon: <ShareAltOutlined />,        category: '运营流程' },

  // 运营流程 - 数据看板
  ops_dashboard_funnel: { code: 'ops_dashboard_funnel', name: '销售漏斗',             description: '销售漏斗可视化分析',                       icon: <FunnelPlotOutlined />,      category: '运营流程' },
  ops_dashboard_rank:   { code: 'ops_dashboard_rank',   name: '招生排行',             description: '招生业绩排行榜',                           icon: <TrophyOutlined />,          category: '运营流程' },
  ops_dashboard_convert:{ code: 'ops_dashboard_convert', name: '转化分析',            description: '线索到成交转化率分析',                     icon: <LineChartOutlined />,       category: '运营流程' },
  ops_dashboard_target: { code: 'ops_dashboard_target', name: '业绩目标',             description: '业绩目标设定与追踪',                       icon: <AimOutlined />,             category: '运营流程' },
  ops_dashboard_roi:    { code: 'ops_dashboard_roi',    name: '投入产出',             description: '投入产出比分析',                           icon: <FundOutlined />,            category: '运营流程' },

  // 运营流程 - 课程开发流程
  ops_std_dev_flow:        { code: 'ops_std_dev_flow',        name: '标准开发与共建流程',        description: '课程标准开发与共建协作流程',               icon: <FileTextOutlined />,        category: '运营流程' },
  ops_question_dev_flow:   { code: 'ops_question_dev_flow',   name: '题库开发与共建流程',        description: '题库资源开发与共建协作流程',               icon: <DatabaseOutlined />,        category: '运营流程' },
  ops_video_dev_flow:      { code: 'ops_video_dev_flow',      name: '视频开发与共建流程',        description: '教学视频开发与共建协作流程',               icon: <PlayCircleOutlined />,      category: '运营流程' },
  ops_simulation_dev_flow: { code: 'ops_simulation_dev_flow', name: '仿真教材开发与共建流程',    description: '仿真教材开发与共建协作流程',               icon: <ExperimentOutlined />,      category: '运营流程' },
  ops_tech_doc_dev_flow:   { code: 'ops_tech_doc_dev_flow',   name: '技术文件开发与共建流程',    description: '技术文件开发与共建协作流程',               icon: <FolderOpenOutlined />,      category: '运营流程' },

  // 运营流程 - 培训运营流程
  ops_train_demand_survey:   { code: 'ops_train_demand_survey',   name: '需求调研', description: '培训需求调研与信息采集',           icon: <SearchOutlined />,           category: '运营流程' },
  ops_train_solution_design: { code: 'ops_train_solution_design', name: '方案设计', description: '培训方案设计与编制',               icon: <FormOutlined />,             category: '运营流程' },
  ops_train_class_mgmt:      { code: 'ops_train_class_mgmt',      name: '开班管理', description: '培训班级开班与人员管理',           icon: <TeamOutlined />,             category: '运营流程' },
  ops_train_implementation:  { code: 'ops_train_implementation',  name: '培训实施', description: '培训过程实施与过程管理',           icon: <PlayCircleOutlined />,       category: '运营流程' },
  ops_train_cert_issue:      { code: 'ops_train_cert_issue',      name: '证书颁发', description: '培训证书颁发与管理',               icon: <SafetyCertificateOutlined />, category: '运营流程' },
  ops_train_graduation:      { code: 'ops_train_graduation',      name: '毕业典礼', description: '培训毕业典礼组织与管理',           icon: <TrophyOutlined />,           category: '运营流程' },
  ops_train_project_close:   { code: 'ops_train_project_close',   name: '项目结题', description: '培训项目结题与验收',               icon: <CheckCircleOutlined />,      category: '运营流程' },
  ops_train_project_apply:   { code: 'ops_train_project_apply',   name: '项目申报', description: '培训项目申报与立项',               icon: <FileAddOutlined />,          category: '运营流程' },

  // 培训运营 - 需求调研
  ops_talent_demand:   { code: 'ops_talent_demand',   name: '人才需求', description: '企业人才需求调研与采集',           icon: <TeamOutlined />,             category: '运营流程' },
  ops_training_demand: { code: 'ops_training_demand', name: '培训需求', description: '培训需求调研与信息采集',           icon: <BookOutlined />,             category: '运营流程' },

  // 培训运营 - 方案设计
  ops_class_split_setting:       { code: 'ops_class_split_setting',       name: '分班设置',           description: '培训班级划分与设置',               icon: <ApartmentOutlined />,        category: '运营流程' },
  ops_schedule_setting:          { code: 'ops_schedule_setting',          name: '排课（计划）设置',   description: '培训课程排课与计划设置',           icon: <CalendarOutlined />,         category: '运营流程' },
  ops_incentive_setting:         { code: 'ops_incentive_setting',         name: '激励设置',           description: '培训激励政策与规则设置',           icon: <GiftOutlined />,             category: '运营流程' },
  ops_budget_setting:            { code: 'ops_budget_setting',            name: '经费设置',           description: '培训经费预算与分配设置',           icon: <DollarOutlined />,           category: '运营流程' },
  ops_ai_learning_task_setting:  { code: 'ops_ai_learning_task_setting',  name: 'AI学习任务设置',     description: 'AI 驱动学习任务配置',              icon: <RobotOutlined />,            category: '运营流程' },
  ops_academic_division_setting: { code: 'ops_academic_division_setting', name: '教务分工设置',       description: '教务人员分工与职责设置',           icon: <UserSwitchOutlined />,       category: '运营流程' },

  // 培训运营 - 开班管理
  ops_material_generate:    { code: 'ops_material_generate',    name: '材料生成', description: '开班材料自动生成',                 icon: <FileAddOutlined />,          category: '运营流程' },
  ops_material_apply:       { code: 'ops_material_apply',       name: '材料申报', description: '开班材料申报与提交',               icon: <FormOutlined />,             category: '运营流程' },
  ops_mobilization_meeting: { code: 'ops_mobilization_meeting', name: '动员会',   description: '培训动员会组织与管理',             icon: <SoundOutlined />,            category: '运营流程' },

  // 培训运营 - 培训实施
  ops_attendance_mgmt:  { code: 'ops_attendance_mgmt',  name: '出勤率管理', description: '学员出勤率统计与管理',             icon: <CheckCircleOutlined />,      category: '运营流程' },
  ops_pass_rate_mgmt:   { code: 'ops_pass_rate_mgmt',   name: '通过率管理', description: '培训通过率统计与分析',             icon: <LineChartOutlined />,        category: '运营流程' },
  ops_homework_mgmt:    { code: 'ops_homework_mgmt',    name: '作业管理',   description: '培训作业布置与批改管理',           icon: <EditOutlined />,             category: '运营流程' },
  ops_evaluation_mgmt:  { code: 'ops_evaluation_mgmt',  name: '评价管理',   description: '培训效果评价与反馈管理',           icon: <StarOutlined />,             category: '运营流程' },
  ops_record_mgmt:      { code: 'ops_record_mgmt',      name: '记录管理',   description: '培训过程记录与档案管理',           icon: <HistoryOutlined />,          category: '运营流程' },

  // 培训运营 - 项目结题
  ops_subsidy_material_apply:  { code: 'ops_subsidy_material_apply',  name: '补贴申报材料申报', description: '培训补贴申报材料在线申报',         icon: <FileProtectOutlined />,      category: '运营流程' },
  ops_subsidy_material_export: { code: 'ops_subsidy_material_export', name: '补贴申报材料导出', description: '培训补贴申报材料批量导出',         icon: <ExportOutlined />,           category: '运营流程' },

  // 培训运营 - 项目申报
  ops_qianlv_reward_apply:     { code: 'ops_qianlv_reward_apply',     name: '千铝奖励申报',       description: '千铝品牌奖励项目申报',             icon: <TrophyOutlined />,           category: '运营流程' },
  ops_gov_reward_guide:        { code: 'ops_gov_reward_guide',        name: '政府奖励申报指引',   description: '政府奖励政策申报指引',             icon: <BankOutlined />,             category: '运营流程' },
  ops_enterprise_reward_apply: { code: 'ops_enterprise_reward_apply', name: '企业奖励申报',       description: '企业奖励项目申报',                 icon: <ShopOutlined />,             category: '运营流程' },
  ops_college_reward_apply:    { code: 'ops_college_reward_apply',    name: '院校奖励申报',       description: '院校奖励项目申报',                 icon: <ReadOutlined />,             category: '运营流程' },
  ops_student_service_flow:    { code: 'ops_student_service_flow',    name: '学员服务流程',       description: '学员服务全流程管理',               icon: <CustomerServiceOutlined />,  category: '运营流程' },
  ops_title_apply_flow:        { code: 'ops_title_apply_flow',        name: '职称申报流程',       description: '学员职称申报流程指引',             icon: <IdcardOutlined />,           category: '运营流程' },
  ops_subsidy_apply_flow:      { code: 'ops_subsidy_apply_flow',      name: '补贴申报流程',       description: '培训补贴申报流程指引',             icon: <AccountBookOutlined />,      category: '运营流程' },
  ops_points_exchange_flow:    { code: 'ops_points_exchange_flow',    name: '积分兑换流程',       description: '学员积分兑换流程管理',             icon: <GiftOutlined />,             category: '运营流程' },
  ops_expert_apply_flow:       { code: 'ops_expert_apply_flow',       name: '专家申请流程',       description: '专家资格申请流程指引',             icon: <UserSwitchOutlined />,       category: '运营流程' },
  ops_honor_apply_flow:        { code: 'ops_honor_apply_flow',        name: '荣誉申报流程',       description: '荣誉奖项申报流程指引',             icon: <SafetyCertificateOutlined />, category: '运营流程' },

  // 认定运营流程
  ops_cert_notice_mgmt:   { code: 'ops_cert_notice_mgmt',   name: '通知管理', description: '认定考试通知发布与管理',           icon: <BellOutlined />,              category: '运营流程' },
  ops_cert_qualification: { code: 'ops_cert_qualification', name: '资格审核', description: '考生资格审核与确认',               icon: <AuditOutlined />,             category: '运营流程' },
  ops_cert_plan_mgmt:     { code: 'ops_cert_plan_mgmt',     name: '计划管理', description: '认定考试计划制定与管理',           icon: <CalendarOutlined />,          category: '运营流程' },
  ops_cert_exam_arrange:  { code: 'ops_cert_exam_arrange',  name: '考场编排', description: '认定考场编排与座位分配',           icon: <ApartmentOutlined />,         category: '运营流程' },
  ops_cert_pre_exam_prep: { code: 'ops_cert_pre_exam_prep', name: '考前准备', description: '认定考前物资与设备准备',           icon: <FileProtectOutlined />,       category: '运营流程' },
  ops_cert_exam_meeting:  { code: 'ops_cert_exam_meeting',  name: '考务会议', description: '认定考务工作会议组织',             icon: <TeamOutlined />,              category: '运营流程' },
  ops_cert_assessment:    { code: 'ops_cert_assessment',    name: '考评进行', description: '认定考评过程管理与监控',           icon: <CheckCircleOutlined />,       category: '运营流程' },
  ops_cert_score_publish: { code: 'ops_cert_score_publish', name: '成绩公示', description: '认定成绩公示与发布',               icon: <FundOutlined />,              category: '运营流程' },

  // 竞赛运营流程
  ops_comp_apply:          { code: 'ops_comp_apply',          name: '竞赛申报',       description: '竞赛项目申报与立项',               icon: <FormOutlined />,              category: '运营流程' },
  ops_comp_register:       { code: 'ops_comp_register',       name: '竞赛报名',       description: '竞赛选手报名与审核',               icon: <UsergroupAddOutlined />,      category: '运营流程' },
  ops_comp_tech_seminar:   { code: 'ops_comp_tech_seminar',   name: '技术研讨',       description: '竞赛技术研讨与交流',               icon: <BulbOutlined />,              category: '运营流程' },
  ops_comp_enterprise_sel: { code: 'ops_comp_enterprise_sel', name: '企业选拔赛',     description: '企业内部选拔赛组织',               icon: <ShopOutlined />,              category: '运营流程' },
  ops_comp_preliminary:    { code: 'ops_comp_preliminary',    name: '初赛（地市赛）', description: '地市初赛组织与管理',               icon: <FlagOutlined />,              category: '运营流程' },
  ops_comp_semifinal:      { code: 'ops_comp_semifinal',      name: '复赛',           description: '复赛组织与管理',                   icon: <RiseOutlined />,              category: '运营流程' },
  ops_comp_pre_training:   { code: 'ops_comp_pre_training',   name: '赛前培训',       description: '竞赛赛前培训组织',                 icon: <BookOutlined />,              category: '运营流程' },
  ops_comp_opening:        { code: 'ops_comp_opening',        name: '开幕仪式',       description: '竞赛开幕仪式组织',                 icon: <PlayCircleOutlined />,        category: '运营流程' },
  ops_comp_final:          { code: 'ops_comp_final',          name: '决赛',           description: '竞赛决赛组织与管理',               icon: <TrophyOutlined />,            category: '运营流程' },
  ops_comp_closing:        { code: 'ops_comp_closing',        name: '闭幕仪式',       description: '竞赛闭幕仪式组织',                 icon: <CheckCircleOutlined />,       category: '运营流程' },
  ops_comp_project_close:  { code: 'ops_comp_project_close',  name: '竞赛结题',       description: '竞赛项目结题与总结',               icon: <FileTextOutlined />,          category: '运营流程' },

  // 竞赛结题
  ops_comp_close_report:     { code: 'ops_comp_close_report',     name: '报告', description: '竞赛结题报告编制与提交',           icon: <FileTextOutlined />,        category: '运营流程' },
  ops_comp_close_settlement: { code: 'ops_comp_close_settlement', name: '结算', description: '竞赛费用结算与对账',               icon: <AccountBookOutlined />,     category: '运营流程' },
  ops_comp_close_apply:      { code: 'ops_comp_close_apply',      name: '申报', description: '竞赛结题材料申报',                 icon: <FormOutlined />,            category: '运营流程' },

  // 支持保障 - 政策库
  sup_policy_national:   { code: 'sup_policy_national',   name: '国家政策', description: '国家层面政策法规库',               icon: <BankOutlined />,              category: '支持保障' },
  sup_policy_provincial: { code: 'sup_policy_provincial', name: '省级政策', description: '省级政策法规库',                   icon: <GlobalOutlined />,            category: '支持保障' },
  sup_policy_city:       { code: 'sup_policy_city',       name: '地市政策', description: '地市级政策法规库',                 icon: <EnvironmentOutlined />,       category: '支持保障' },
  sup_policy_company:    { code: 'sup_policy_company',    name: '公司政策', description: '公司内部政策制度库',               icon: <ShopOutlined />,              category: '支持保障' },
  // 支持保障 - 专家库
  sup_expert_question: { code: 'sup_expert_question', name: '题库专家管理', description: '题库领域专家资源管理',           icon: <DatabaseOutlined />,          category: '支持保障' },
  sup_trainer_mgmt:    { code: 'sup_trainer_mgmt',    name: '培训师管理',   description: '培训师资质与档案管理',           icon: <TeamOutlined />,              category: '支持保障' },
  sup_assessor_mgmt:   { code: 'sup_assessor_mgmt',   name: '考评员管理',   description: '考评员资质与排班管理',           icon: <SafetyCertificateOutlined />, category: '支持保障' },
  sup_referee_mgmt:    { code: 'sup_referee_mgmt',    name: '裁判管理',     description: '竞赛裁判资源管理',               icon: <TrophyOutlined />,            category: '支持保障' },
  sup_academic_staff:  { code: 'sup_academic_staff',  name: '教务人员管理', description: '教务人员档案与分工管理',         icon: <ReadOutlined />,              category: '支持保障' },
  sup_exam_staff:      { code: 'sup_exam_staff',      name: '考务人员管理', description: '考务人员档案与排班管理',         icon: <ScheduleOutlined />,          category: '支持保障' },
  sup_comp_staff:      { code: 'sup_comp_staff',      name: '赛务人员管理', description: '赛务人员档案与分工管理',         icon: <FlagOutlined />,              category: '支持保障' },
  // 支持保障 - 设备库
  sup_equip_practical: { code: 'sup_equip_practical', name: '实训设备', description: '实训设备台账与管理',               icon: <ExperimentOutlined />,        category: '支持保障' },
  sup_equip_training:  { code: 'sup_equip_training',  name: '培训设备', description: '培训设备台账与管理',               icon: <BookOutlined />,              category: '支持保障' },
  sup_equip_cert:      { code: 'sup_equip_cert',      name: '鉴定设备', description: '鉴定考试设备台账与管理',         icon: <SafetyCertificateOutlined />, category: '支持保障' },
  sup_equip_comp:      { code: 'sup_equip_comp',      name: '竞赛设备', description: '竞赛专用设备台账与管理',         icon: <TrophyOutlined />,            category: '支持保障' },
  // 支持保障 - 物料库
  sup_job_mgmt:      { code: 'sup_job_mgmt',      name: '职业管理', description: '职业分类与标准管理',                 icon: <IdcardOutlined />,            category: '支持保障' },
  sup_post_mgmt:     { code: 'sup_post_mgmt',     name: '岗位管理', description: '岗位体系与任职要求管理',           icon: <ApartmentOutlined />,         category: '支持保障' },
  sup_standard_mgmt: { code: 'sup_standard_mgmt', name: '标准管理', description: '行业标准与规范管理',               icon: <FileTextOutlined />,          category: '支持保障' },
  sup_question_bank: { code: 'sup_question_bank', name: '题库管理', description: '题库资源统一管理',                 icon: <DatabaseOutlined />,          category: '支持保障' },
  sup_textbook_mgmt: { code: 'sup_textbook_mgmt', name: '教材管理', description: '教材资源统一管理',                 icon: <BookOutlined />,              category: '支持保障' },
  sup_video_mgmt:    { code: 'sup_video_mgmt',    name: '视频管理', description: '教学视频资源管理',                 icon: <PlayCircleOutlined />,        category: '支持保障' },
  sup_material_mgmt: { code: 'sup_material_mgmt', name: '物料管理', description: '培训物料与耗材管理',               icon: <InboxOutlined />,             category: '支持保障' },
  sup_ad_mgmt:       { code: 'sup_ad_mgmt',       name: '广告管理', description: '宣传广告素材管理',                 icon: <NotificationOutlined />,      category: '支持保障' },
  sup_logo_mgmt:     { code: 'sup_logo_mgmt',     name: '标识管理', description: '品牌标识与视觉规范管理',           icon: <TagOutlined />,               category: '支持保障' },
  sup_budget_mgmt:   { code: 'sup_budget_mgmt',   name: '经费管理', description: '项目经费预算与使用管理',           icon: <DollarOutlined />,            category: '支持保障' },
  // 支持保障 - 方法库
  sup_rule_standard:  { code: 'sup_rule_standard',  name: '标准规则',         description: '标准制定与执行规则',           icon: <FileTextOutlined />,          category: '支持保障' },
  sup_rule_question:  { code: 'sup_rule_question',  name: '题库规则',         description: '题库建设与管理规则',           icon: <DatabaseOutlined />,          category: '支持保障' },
  sup_rule_textbook:  { code: 'sup_rule_textbook',  name: '教材规则',         description: '教材开发与审核规则',           icon: <BookOutlined />,              category: '支持保障' },
  sup_rule_video:     { code: 'sup_rule_video',     name: '视频规则',         description: '视频制作与发布规则',           icon: <PlayCircleOutlined />,        category: '支持保障' },
  sup_rule_practical: { code: 'sup_rule_practical', name: '实训规则',         description: '实训教学组织规则',             icon: <ExperimentOutlined />,        category: '支持保障' },
  sup_rule_forum:     { code: 'sup_rule_forum',     name: '论坛规则',         description: '技术论坛组织规则',             icon: <CommentOutlined />,           category: '支持保障' },
  sup_rule_training:  { code: 'sup_rule_training',  name: '培训规则',         description: '培训项目实施规则',             icon: <ScheduleOutlined />,          category: '支持保障' },
  sup_rule_cert:      { code: 'sup_rule_cert',      name: '认定规则',         description: '技能认定考试规则',             icon: <SafetyCertificateOutlined />, category: '支持保障' },
  sup_rule_comp:      { code: 'sup_rule_comp',      name: '竞赛规则',         description: '技能竞赛组织规则',             icon: <TrophyOutlined />,            category: '支持保障' },
  sup_rule_ceremony:  { code: 'sup_rule_ceremony',  name: '开闭幕仪式规则',   description: '开闭幕仪式组织规则',           icon: <FlagOutlined />,              category: '支持保障' },
  // 支持保障 - 环境库
  sup_env_forum:           { code: 'sup_env_forum',           name: '论坛布置标准',           description: '技术论坛场地布置标准',       icon: <CommentOutlined />,           category: '支持保障' },
  sup_env_question_accept: { code: 'sup_env_question_accept', name: '题库验收布置标准',       description: '题库验收现场布置标准',       icon: <DatabaseOutlined />,          category: '支持保障' },
  sup_env_ceremony:        { code: 'sup_env_ceremony',        name: '开（闭）幕仪式布置标准', description: '开闭幕仪式场地布置标准',     icon: <FlagOutlined />,              category: '支持保障' },
  sup_env_training_class:  { code: 'sup_env_training_class',  name: '培训班布置标准',         description: '培训班教室布置标准',         icon: <BookOutlined />,              category: '支持保障' },
  sup_env_cert_exam:       { code: 'sup_env_cert_exam',       name: '认定考场布置标准',       description: '技能认定考场布置标准',       icon: <SafetyCertificateOutlined />, category: '支持保障' },
  sup_env_comp_site:       { code: 'sup_env_comp_site',       name: '竞赛现场布置标准',       description: '技能竞赛现场布置标准',       icon: <TrophyOutlined />,            category: '支持保障' },

  // 绩效人才
  perf_assess_structure:     { code: 'perf_assess_structure',     name: '结构考核', description: '组织架构与岗位结构考核',     icon: <ApartmentOutlined />,         category: '绩效人才' },
  perf_assess_qualification: { code: 'perf_assess_qualification', name: '资质考核', description: '人员资质与能力达标考核',     icon: <SafetyCertificateOutlined />, category: '绩效人才' },
  perf_assess_resource:      { code: 'perf_assess_resource',      name: '资源考核', description: '培训鉴定竞赛资源利用考核',   icon: <DatabaseOutlined />,          category: '绩效人才' },
  perf_skill_map:            { code: 'perf_skill_map',            name: '技能图谱', description: '人才技能图谱与能力模型',     icon: <NodeIndexOutlined />,         category: '绩效人才' },
  perf_pipeline_build:       { code: 'perf_pipeline_build',       name: '梯队建设', description: '人才梯队建设与培养规划',     icon: <RiseOutlined />,              category: '绩效人才' },
  perf_talent_map:           { code: 'perf_talent_map',           name: '人才地图', description: '人才分布与储备全景地图',     icon: <GlobalOutlined />,            category: '绩效人才' },

  // 机构风控
  risk_product_design:  { code: 'risk_product_design',  name: '产品设计部风控',   description: '产品设计部风险识别与管控',   icon: <SketchOutlined />,            category: '机构风控' },
  risk_market_expand:   { code: 'risk_market_expand',   name: '市场拓展部风控',   description: '市场拓展部风险识别与管控',   icon: <AimOutlined />,               category: '机构风控' },
  risk_course_dev:      { code: 'risk_course_dev',      name: '课程开发部风控',   description: '课程开发部风险识别与管控',   icon: <BookOutlined />,              category: '机构风控' },
  risk_agent_dev:       { code: 'risk_agent_dev',       name: '智能体开发部风控', description: '智能体开发部风险识别与管控', icon: <RobotOutlined />,             category: '机构风控' },
  risk_cloud_dev:       { code: 'risk_cloud_dev',       name: '云平台开发部风控', description: '云平台开发部风险识别与管控', icon: <CloudOutlined />,               category: '机构风控' },
  risk_simulation_dev:  { code: 'risk_simulation_dev',  name: '仿真开发部风控',   description: '仿真开发部风险识别与管控',   icon: <ExperimentOutlined />,        category: '机构风控' },
  risk_training_ops:    { code: 'risk_training_ops',    name: '培训运营风控',     description: '培训运营风险识别与管控',     icon: <ScheduleOutlined />,          category: '机构风控' },
  risk_cert_ops:        { code: 'risk_cert_ops',        name: '认定运营风控',     description: '认定运营风险识别与管控',     icon: <SafetyCertificateOutlined />, category: '机构风控' },
  risk_competition_ops: { code: 'risk_competition_ops', name: '竞赛运营风控',     description: '竞赛运营风险识别与管控',     icon: <TrophyOutlined />,            category: '机构风控' },
  risk_hr:              { code: 'risk_hr',              name: '人力资源风控',     description: '人力资源风险识别与管控',     icon: <TeamOutlined />,              category: '机构风控' },
  risk_finance:         { code: 'risk_finance',         name: '业财融风控',       description: '业财融风险识别与管控',       icon: <DollarOutlined />,            category: '机构风控' },
  risk_admin:           { code: 'risk_admin',           name: '行政后勤风控',     description: '行政后勤风险识别与管控',     icon: <HomeOutlined />,              category: '机构风控' },
  risk_platform_coop:   { code: 'risk_platform_coop',   name: '平台共建部风控',   description: '平台共建部风险识别与管控',   icon: <ApartmentOutlined />,         category: '机构风控' },
};

const CATEGORY_COLORS: Record<string, string> = {
  'AI规划': 'blue',
  '平台/基础设施': 'purple',
  '运营系统': 'green',
  'AI能力/智能体': 'orange',
  '业务规划': 'cyan',
  '人才规划': 'geekblue',
  '计划管理': 'lime',
  '运营流程': 'volcano',
  '支持保障': 'gold',
  '绩效人才': 'blue',
  '机构风控': 'blue',
};

interface ProductPlaceholderProps {
  moduleCode?: string;
}

export const ProductPlaceholder: React.FC<ProductPlaceholderProps> = ({ moduleCode }) => {
  const code = moduleCode || 'product_ai_industry';
  const info = MODULE_MAP[code] || MODULE_MAP.product_ai_industry;
  const categoryColor = CATEGORY_COLORS[info.category] || 'default';

  return (
    <div style={{ padding: 24, maxWidth: 900, margin: '0 auto' }}>
      <Card
        style={{
          borderRadius: 12,
          boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 12,
              background: 'linear-gradient(135deg, #165dff 0%, #4080ff 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 28,
              color: '#fff',
              flexShrink: 0,
            }}
          >
            {info.icon}
          </div>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              {info.name}
            </Title>
            <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
              <Tag color={categoryColor}>{info.category}</Tag>
              <Tag>{info.code}</Tag>
            </div>
          </div>
        </div>

        <Divider style={{ margin: '12px 0 20px' }} />

        <Paragraph style={{ fontSize: 15, color: '#4b5563', lineHeight: 1.8 }}>
          <Text strong>模块描述：</Text>
          {info.description}
        </Paragraph>

        <Divider style={{ margin: '20px 0' }} />

        <div
          style={{
            background: '#f6f8fa',
            borderRadius: 8,
            padding: 20,
            textAlign: 'center',
          }}
        >
          <RocketOutlined style={{ fontSize: 32, color: '#165dff', marginBottom: 12 }} />
          <Title level={5} style={{ color: '#165dff' }}>
            此模块正在规划建设中
          </Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            该功能模块已纳入产品规划路线图，具体开发计划请关注项目进度更新。
          </Paragraph>
        </div>

        <Divider style={{ margin: '20px 0' }} />

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <div>
            <Text type="secondary">模块编码：</Text>
            <Text code>{info.code}</Text>
          </div>
          <div>
            <Text type="secondary">所属分类：</Text>
            <Tag color={categoryColor}>{info.category}</Tag>
          </div>
        </div>
      </Card>
    </div>
  );
};