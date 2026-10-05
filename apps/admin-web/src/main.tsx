import './utils/legacyBrowserPolyfills';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
import ResourceCenterPage from './pages/question_bank/ResourceCenterPage';
import KnowledgeFixPage from './pages/question_bank/KnowledgeFixPage';
import XiMuBiaoGuanLiPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage';
import TheoryQuestionGenPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/TheoryQuestionGenPage';
import SkillQuestionGenPage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage';
import AiCoursewarePage from './pages/zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage';
import { RencaishibieEmbedPage } from './pages/knowledge/RencaishibieEmbedPage';
import { WorkTypePage } from './pages/exam/WorkTypePage';
import { CoursePage } from './pages/exam/CoursePage';
import { DevTaskDetailPage } from './pages/exam/DevTaskDetailPage';
import { TaskExamAuditAssignListPage } from './pages/exam/TaskExamAuditAssignListPage';
import { TaskExamshicaoAuditAssignListPage } from './pages/exam/TaskExamshicaoAuditAssignListPage';
import { TaskExamsubAuditAssignListPage } from './pages/exam/TaskExamsubAuditAssignListPage';
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingDashboardPage } from './pages/training/TrainingDashboardPage';
import { TrainingFollowClassPage } from './pages/training/TrainingFollowClassPage';
import { TrainingFollowExamPage } from './pages/training/TrainingFollowExamPage';
import { TrainingSubsidyDeclarePage } from './pages/training/TrainingSubsidyDeclarePage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
import { TrainingClassDivisionPage } from './pages/training/TrainingClassDivisionPage';
import { TrainingClassDeclarePage } from './pages/training/TrainingClassDeclarePage';
import { UserAuthCoursePage } from './pages/exam/UserAuthCoursePage';
import { UserListPage } from './pages/chazheng/UserListPage';
import { CertQueryPage } from './pages/chazheng/CertQueryPage';
import { RequirementsManagementPage } from './pages/training/RequirementsManagementPage';
import { EnterpriseTrainingDemandPage } from './pages/training/EnterpriseTrainingDemandPage';
import { EnterpriseTalentDemandPage } from './pages/training/EnterpriseTalentDemandPage';
import { TrainingProgramPage } from './pages/training/TrainingProgramPage';
import { TrainingSchedulePage } from './pages/training/TrainingSchedulePage';
import { TrainingGraduationPage } from './pages/training/TrainingGraduationPage';
import { TrainingGraduationCeremonyPage } from './pages/training/TrainingGraduationCeremonyPage';
import { TrainingGraduationClassDetailPage } from './pages/training/TrainingGraduationClassDetailPage';
import { TrainingCapabilityPage } from './pages/training/TrainingCapabilityPage';
import { TrainingSummaryPage } from './pages/training/TrainingSummaryPage';
import { TrainingCertificateTemplatePage } from './pages/training/TrainingCertificateTemplatePage';
import { TrainingInformationPage } from './pages/training/TrainingInformationPage';
import { TrainingStudentPage } from './pages/training/TrainingStudentPage';
import { TrainingAcademicManagePage } from './pages/training/TrainingAcademicManagePage';
import { EnterpriseResearchPage } from './pages/training/EnterpriseResearchPage';
import { EnterpriseManagePage } from './pages/training/EnterpriseManagePage';
import { SolutionPlanningPage } from './pages/training/SolutionPlanningPage';
import { EnterprisePlanReviewPage } from './pages/training/EnterprisePlanReviewPage';
import { DataPanelPage } from './pages/zhixunplatform/DataPanelPage';
import { TaskManagementPage } from './pages/zhixunplatform/TaskManagementPage';
import { TaskArchivesPage } from './pages/zhixunplatform/TaskArchivesPage';
import { TrainingProcessPage } from './pages/zhixunplatform/TrainingProcessPage';
import { ProjectArchivesPage } from './pages/zhixunplatform/ProjectArchivesPage';
import { MaterialArchivesPage } from './pages/zhixunplatform/MaterialArchivesPage';
import { TalentIdentificationPage } from './pages/zhixunplatform/TalentIdentificationPage';
import { PreliminaryPlanPage } from './pages/zhixunplatform/PreliminaryPlanPage';
import { CreateTaskPage } from './pages/zhixunplatform/CreateTaskPage';
import { FinalPlanPage } from './pages/zhixunplatform/FinalPlanPage';
import { ClassDeclarationPage } from './pages/zhixunplatform/ClassDeclarationPage';
import { TrainingImplementationPage } from './pages/zhixunplatform/TrainingImplementationPage';
import { CertificationPage } from './pages/zhixunplatform/CertificationPage';
import { DeclarationServicePage } from './pages/zhixunplatform/DeclarationServicePage';
import { GraduationPage } from './pages/zhixunplatform/GraduationPage';
import { RefundPage } from './pages/zhixunplatform/RefundPage';
import { PersonnelArchivePage } from './pages/zhixunplatform/PersonnelArchivePage';
import { createArchiveTablePage } from './pages/zhixunplatform/ArchiveTablePage';
import { createPmFormPage } from './pages/zhixunplatform/PmFormPage';
import { TaskStartPage } from './pages/expert/TaskStartPage';
import { QuestionBankPage } from './pages/exam/QuestionBankPage';
import { CompanyQuestionPage } from './pages/exam/CompanyQuestionPage';
import { MakeExamPlanPage } from './pages/exam/MakeExamPlanPage';
import { KnowledgeRatePage } from './pages/exam/KnowledgeRatePage';
import { KnowledgeStructurePage } from './pages/exam/KnowledgeStructurePage';
import { FaqConversionPage } from './pages/exam/FaqConversionPage';
import { TrainExamListPage } from './pages/training/TrainExamListPage';
import { StatsOverviewPage } from './pages/exam/reviewSyncStats/StatsOverviewPage';
import { StudentDetailPage } from './pages/exam/reviewSyncStats/StudentDetailPage';
import { TabletAssessorApp } from './pages/assessor/TabletAssessorApp';
import { DefenseReviewApp } from './pages/assessor/DefenseReviewApp';
import { PracticalCollectionApp } from './pages/practicalCollection/PracticalCollectionApp';
import { CompetitionEventManagementPage } from './pages/competition/CompetitionEventManagementPage';
import { CompetitionPlayerManagementPage } from './pages/competition/CompetitionPlayerManagementPage';
import { CompetitionPlayerSortingManagementPage } from './pages/competition/CompetitionPlayerSortingPage';
import { CompetitionRefereeManagementPage } from './pages/competition/CompetitionRefereeManagementPage';
import { CompetitionRefereeGroupPage } from './pages/competition/CompetitionRefereeGroupPage';
import { CompetitionEventModulePage, type CompetitionEventModule } from './pages/competition/CompetitionEventModulePage';
import { CompetitionQuestionBankPage, CompetitionSimplePracticalQuestionPage } from './pages/competition/CompetitionQuestionBankPage';
import { CompetitionQuestionBlueprintPage } from './pages/competition/CompetitionQuestionBlueprintPage';
import { CompetitionMakeExamPlanPage } from './pages/competition/CompetitionMakeExamPlanPage';
import { CompetitionPracticalScoringSchemePage } from './pages/competition/CompetitionPracticalScoringSchemePage';
import { CompetitionPracticalJudgeApp } from './pages/competition/CompetitionPracticalJudgeApp';
import { FormalExamPlayerApp } from './pages/formalExam/FormalExamPlayerApp';
import { findCompetitionMenuCode, isCompetitionPath } from './routes/competitionRoute';
import { useAuthStore } from './stores/authStore';
import { useTabStore } from './stores/tabStore';
import { NavigationBlockerProvider, useNavigationBlocker } from './hooks/useUnsavedChangesBlocker';

// Pre-instantiate factory components for stable React references
const ArchiveEquipmentPage = createArchiveTablePage('equipment');
const ArchiveMaterialsPage = createArchiveTablePage('materials');
const ArchiveRulesPage = createArchiveTablePage('rules');
const ArchiveEnvironmentPage = createArchiveTablePage('environment');
const PmStrategicPage = createPmFormPage('strategic');
const PmOrgPerfPage = createPmFormPage('orgPerf');
const PmPbcPage = createPmFormPage('pbc');
const PmCharterPage = createPmFormPage('charter');
const PmMilestonePage = createPmFormPage('milestone');
const PmRiskPage = createPmFormPage('risk');
const PmIssuePage = createPmFormPage('issue');
const PmChangePage = createPmFormPage('change');
const PmMeetingPage = createPmFormPage('meeting');
const PmKnowledgePage = createPmFormPage('knowledge');
const PmWbsPage = createPmFormPage('wbs');
const PmCommunicationPage = createPmFormPage('communication');
const PmStatusPage = createPmFormPage('status');
import { ProductPlaceholder } from './pages/product/ProductPlaceholder';
import { TaskEndPage } from './pages/expert/TaskEndPage';
import { ProjectManagementPage } from './pages/certification/ProjectManagementPage';
import { CertificationDataPanelPage } from './pages/certification/CertificationDataPanelPage';
import { PracticalConfigurationPage } from './pages/certification/PracticalConfigurationPage';
import { PreparationPage } from './pages/certification/PreparationPage';
import { DefenseManagementPage } from './pages/certification/DefenseManagementPage';
import { CollectionTaskListPage } from './pages/certification/collection/CollectionTaskListPage';
import { CollectionManagePage } from './pages/certification/collection/CollectionManagePage';
import { ArchivePage } from './pages/certification/ArchivePage';
import { TaskArchivePage } from './pages/certification/TaskArchivePage';
import { AssessorInfoPage } from './pages/certification/info/AssessorInfoPage';
import { ExamSiteInfoPage } from './pages/certification/info/ExamSiteInfoPage';
import { ExamEquipmentInfoPage } from './pages/certification/info/ExamEquipmentInfoPage';
import { ExamStaffInfoPage } from './pages/certification/info/ExamStaffInfoPage';
import { TenantPage } from './pages/system/TenantPage';
import { UserPage } from './pages/system/UserPage';
import { OrgPage } from './pages/system/OrgPage';
import { PostPage } from './pages/system/PostPage';
import { RolePage } from './pages/system/RolePage';
import { MenuPage } from './pages/system/MenuPage';
import { DictPage } from './pages/system/DictPage';
import { AuditLogPage } from './pages/system/AuditLogPage';
import { PermissionPage } from './pages/system/PermissionPage';
import { appTheme } from './theme';
import './styles/tokens.css';
import './styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const pageMap: Record<string, React.ReactNode> = {
  knowledge: <KnowledgePage />,
  // 智能课程材料管理 > 细目表管理
  // 课程管理 > 资源管理（知识归属修正）
  question_resource: <ResourceCenterPage />,
  knowledge_fix: <KnowledgeFixPage />,
  xi_mu_biao_guan_li: <XiMuBiaoGuanLiPage />,
  // 智能课程材料管理 > 理论知识试题生成管理
  theory_question_gen: <TheoryQuestionGenPage />,
  // 智能课程材料管理 > 操作技能试题生成管理
  skill_question_gen: <SkillQuestionGenPage />,
  // 智能课程材料管理 > AI课件生成管理
  ai_courseware_gen: <AiCoursewarePage />,
  exam_work_type: <WorkTypePage />,
  dev_task_detail: <DevTaskDetailPage />,
  // 题库审核指派(管理端 3 子菜单,挂在开发任务下)
  task_exam_audit_assign: <TaskExamAuditAssignListPage />,
  task_examshicao_audit_assign: <TaskExamshicaoAuditAssignListPage />,
  task_examsub_audit_assign: <TaskExamsubAuditAssignListPage />,
  exam_course: <CoursePage />,
  'train_info-list': <TrainingInfoPage />,
  train_dashboard: <TrainingDashboardPage />,
  train_follow: <TrainingFollowClassPage />,
  train_follow_exam: <TrainingFollowExamPage />,
  train_graduation_ceremony: <TrainingGraduationCeremonyPage />,
  train_subsidy_declare: <TrainingSubsidyDeclarePage />,
  fen_ban: <TrainingClassDivisionPage />,
  'train_people-list': <TrainingClassPeoplePage />,
  train_people: <TrainingClassPeoplePage />,
  train_declares: <TrainingClassDeclarePage />,
  exam_user_auth_course: <UserAuthCoursePage />,
  chazheng_user_list: <UserListPage />,
  chazheng_cert_query: <CertQueryPage />,
  // 培训管理 → 战略规划
  pxyan_jiu: <EnterpriseResearchPage />,
  pxan_fang_gui_hua: <SolutionPlanningPage />,
  train_needs: <RequirementsManagementPage />,
  'train_demand-train': <EnterpriseTrainingDemandPage />,
  company_demand_train: <EnterpriseTrainingDemandPage />,
  company_plan_review: <EnterprisePlanReviewPage />,
  'train_demand-employee': <EnterpriseTalentDemandPage />,
  company_demand_employee: <EnterpriseTalentDemandPage />,
  train_solutions: <TrainingProgramPage />,
  train_schedule: <TrainingSchedulePage />,
  train_graduation: <TrainingGraduationPage />,
  'train_user-ability': <TrainingCapabilityPage />,
  train_summary: <TrainingSummaryPage />,
  train_certificates: <TrainingCertificateTemplatePage />,
  'train_sign-up': <TrainingInformationPage />,
  'train_sign-up-people': <TrainingInformationPage />,
  'train_user-info': <TrainingStudentPage />,
  'train_teacher-bank': <TrainingAcademicManagePage />,
  train_company: <EnterpriseManagePage />,
  knowledge_spaces: <KnowledgePage />,
  knowledge_search: <KnowledgePage />,
  knowledge_rencaishibie: <RencaishibieEmbedPage />,
  zhixun_data_panel: <DataPanelPage />,
  zhixun_task_management: <TaskManagementPage />,
  zhixun_task_archives: <TaskArchivesPage />,
  zhixun_training_process: <TrainingProcessPage />,
  zhixun_project_archives: <ProjectArchivesPage />,
  zhixun_material_archives: <MaterialArchivesPage />,
  zhixun_talent_identification: <TalentIdentificationPage />,
  zhixun_preliminary_plan: <PreliminaryPlanPage />,
  zhixun_create_task: <CreateTaskPage />,
  zhixun_final_plan: <FinalPlanPage />,
  zhixun_class_declaration: <ClassDeclarationPage />,
  zhixun_training_implementation: <TrainingImplementationPage />,
  zhixun_certification: <CertificationPage />,
  zhixun_declaration_service: <DeclarationServicePage />,
  zhixun_graduation: <GraduationPage />,
  zhixun_refund: <RefundPage />,
  zhixun_archive_personnel: <PersonnelArchivePage />,
  zhixun_archive_equipment: <ArchiveEquipmentPage />,
  zhixun_archive_materials: <ArchiveMaterialsPage />,
  zhixun_archive_rules: <ArchiveRulesPage />,
  zhixun_archive_environment: <ArchiveEnvironmentPage />,
  zhixun_table_strategic: <PmStrategicPage />,
  zhixun_table_org_perf: <PmOrgPerfPage />,
  zhixun_table_pbc: <PmPbcPage />,
  zhixun_table_charter: <PmCharterPage />,
  zhixun_table_milestone: <PmMilestonePage />,
  zhixun_table_risk: <PmRiskPage />,
  zhixun_table_issue: <PmIssuePage />,
  zhixun_table_change: <PmChangePage />,
  zhixun_table_meeting: <PmMeetingPage />,
  zhixun_table_knowledge: <PmKnowledgePage />,
  zhixun_table_wbs: <PmWbsPage />,
  zhixun_table_communication: <PmCommunicationPage />,
  zhixun_table_status: <PmStatusPage />,
  exam_question_bank: <QuestionBankPage />,
  gong_si_shi_ti_guan_li: <CompanyQuestionPage />,
  zu_juan_ji_hua: <MakeExamPlanPage />,
  'train_exam-list': <TrainExamListPage />,
  kao_shi_ji_hua: <TrainExamListPage />,
  exam_knowledge_rate: <KnowledgeRatePage />,
  zhi_shi_jie_gou_xi_mu_biao_guan_li: <KnowledgeStructurePage />,
  exam_faq_conversion: <FaqConversionPage />,
  expert_task_start: <TaskStartPage />,
  expert_task_end: <TaskEndPage />,
  certification_dashboard: <CertificationDataPanelPage />,
  certification_project: <ProjectManagementPage />,
  certification_preparation: <PreparationPage />,
  certification_practical_config: <PracticalConfigurationPage />,
  certification_defense_management: <DefenseManagementPage />,
  certification_practical_collection: <CollectionTaskListPage />,
  certification_archive: <ArchivePage />,
  certification_task_archive: <TaskArchivePage />,
  certification_info_assessor: <AssessorInfoPage />,
  certification_info_site: <ExamSiteInfoPage />,
  certification_info_equipment: <ExamEquipmentInfoPage />,
  certification_info_exam_staff: <ExamStaffInfoPage />,
  // System management pages
  system_tenant: <TenantPage />,
  system_user: <UserPage />,
  system_org: <OrgPage />,
  system_post: <PostPage />,
  system_role: <RolePage />,
  system_menu: <MenuPage />,
  system_dict: <DictPage />,
  system_audit_log: <AuditLogPage />,
  system_permission: <PermissionPage />,
  // kebab-case aliases for page-internal navigation (navStore.setSelectedKey)
  'certification-dashboard': <CertificationDataPanelPage />,
  'zhixun-data-panel': <DataPanelPage />,
  'zhixun-task-management': <TaskManagementPage />,
  'zhixun-task-archives': <TaskArchivesPage />,
  'zhixun-training-process': <TrainingProcessPage />,
  'zhixun-project-archives': <ProjectArchivesPage />,
  'zhixun-material-archives': <MaterialArchivesPage />,
  'zhixun-talent-identification': <TalentIdentificationPage />,
  'zhixun-preliminary-plan': <PreliminaryPlanPage />,
  'zhixun-create-task': <CreateTaskPage />,
  'zhixun-final-plan': <FinalPlanPage />,
  'zhixun-class-declaration': <ClassDeclarationPage />,
  'zhixun-training-implementation': <TrainingImplementationPage />,
  'zhixun-certification': <CertificationPage />,
  'zhixun-declaration-service': <DeclarationServicePage />,
  'zhixun-graduation': <GraduationPage />,
  'zhixun-refund': <RefundPage />,
  'zhixun-archive-personnel': <PersonnelArchivePage />,
  'zhixun-archive-equipment': <ArchiveEquipmentPage />,
  'zhixun-archive-materials': <ArchiveMaterialsPage />,
  'zhixun-archive-rules': <ArchiveRulesPage />,
  'zhixun-archive-environment': <ArchiveEnvironmentPage />,
  'zhixun-table-strategic': <PmStrategicPage />,
  'zhixun-table-org-perf': <PmOrgPerfPage />,
  'zhixun-table-pbc': <PmPbcPage />,
  'zhixun-table-charter': <PmCharterPage />,
  'zhixun-table-milestone': <PmMilestonePage />,
  'zhixun-table-risk': <PmRiskPage />,
  'zhixun-table-issue': <PmIssuePage />,
  'zhixun-table-change': <PmChangePage />,
  'zhixun-table-meeting': <PmMeetingPage />,
  'zhixun-table-knowledge': <PmKnowledgePage />,
  'zhixun-table-wbs': <PmWbsPage />,
  'zhixun-table-communication': <PmCommunicationPage />,
  'zhixun-table-status': <PmStatusPage />,
  'certification-task-archive': <TaskArchivePage />,
  'certification-practical-config': <PracticalConfigurationPage />,
  'certification-defense-management': <DefenseManagementPage />,
  // 战略规划 → 产品规划 模块
  strate_talent_standard: <ProductPlaceholder moduleCode="strate_talent_standard" />,
  strate_talent_check: <ProductPlaceholder moduleCode="strate_talent_check" />,
  product_ai_industry: <ProductPlaceholder moduleCode="product_ai_industry" />,
  product_ai_college: <ProductPlaceholder moduleCode="product_ai_college" />,
  product_ai_enterprise: <ProductPlaceholder moduleCode="product_ai_enterprise" />,
  product_ai_individual: <ProductPlaceholder moduleCode="product_ai_individual" />,
  product_course_dev: <ProductPlaceholder moduleCode="product_course_dev" />,
  product_knowledge_nonferrous: <ProductPlaceholder moduleCode="product_knowledge_nonferrous" />,
  product_qianlv_ecosystem: <ProductPlaceholder moduleCode="product_qianlv_ecosystem" />,
  product_simulation: <ProductPlaceholder moduleCode="product_simulation" />,
  product_training_base: <ProductPlaceholder moduleCode="product_training_base" />,
  product_apprentice: <ProductPlaceholder moduleCode="product_apprentice" />,
  product_cert_agency: <ProductPlaceholder moduleCode="product_cert_agency" />,
  product_competition: <ProductPlaceholder moduleCode="product_competition" />,
  product_studio_apply: <ProductPlaceholder moduleCode="product_studio_apply" />,
  product_course_ops: <ProductPlaceholder moduleCode="product_course_ops" />,
  product_ai_nonferrous_assess: <ProductPlaceholder moduleCode="product_ai_nonferrous_assess" />,
  product_qianlv_knowledge: <ProductPlaceholder moduleCode="product_qianlv_knowledge" />,
  product_smart_classroom: <ProductPlaceholder moduleCode="product_smart_classroom" />,
  product_expert_system: <ProductPlaceholder moduleCode="product_expert_system" />,
  product_ai_learning: <ProductPlaceholder moduleCode="product_ai_learning" />,
  product_ai_assess: <ProductPlaceholder moduleCode="product_ai_assess" />,
  product_ai_agency_apply: <ProductPlaceholder moduleCode="product_ai_agency_apply" />,
  product_ai_individual_apply: <ProductPlaceholder moduleCode="product_ai_individual_apply" />,
  product_ai_class_manage: <ProductPlaceholder moduleCode="product_ai_class_manage" />,
  product_ai_skill_manage: <ProductPlaceholder moduleCode="product_ai_skill_manage" />,
  // 业务规划 13 个部门
  biz_product_design: <ProductPlaceholder moduleCode="biz_product_design" />,
  biz_market_expand: <ProductPlaceholder moduleCode="biz_market_expand" />,
  biz_course_dev: <ProductPlaceholder moduleCode="biz_course_dev" />,
  biz_agent_dev: <ProductPlaceholder moduleCode="biz_agent_dev" />,
  biz_cloud_dev: <ProductPlaceholder moduleCode="biz_cloud_dev" />,
  biz_simulation_dev: <ProductPlaceholder moduleCode="biz_simulation_dev" />,
  biz_training_ops: <ProductPlaceholder moduleCode="biz_training_ops" />,
  biz_cert_ops: <ProductPlaceholder moduleCode="biz_cert_ops" />,
  biz_competition_ops: <ProductPlaceholder moduleCode="biz_competition_ops" />,
  biz_hr: <ProductPlaceholder moduleCode="biz_hr" />,
  biz_finance: <ProductPlaceholder moduleCode="biz_finance" />,
  biz_admin: <ProductPlaceholder moduleCode="biz_admin" />,
  biz_platform_coop: <ProductPlaceholder moduleCode="biz_platform_coop" />,
  // 人才规划
  talent_three_year: <ProductPlaceholder moduleCode="talent_three_year" />,
  talent_five_year: <ProductPlaceholder moduleCode="talent_five_year" />,
  // 计划管理
  plan_work_year: <ProductPlaceholder moduleCode="plan_work_year" />,
  plan_work_quarter: <ProductPlaceholder moduleCode="plan_work_quarter" />,
  plan_work_month: <ProductPlaceholder moduleCode="plan_work_month" />,
  plan_train_year: <ProductPlaceholder moduleCode="plan_train_year" />,
  plan_train_quarter: <ProductPlaceholder moduleCode="plan_train_quarter" />,
  plan_train_month: <ProductPlaceholder moduleCode="plan_train_month" />,
  // 运营流程 - 线索招生
  ops_lead_collect: <ProductPlaceholder moduleCode="ops_lead_collect" />,
  ops_lead_assign: <ProductPlaceholder moduleCode="ops_lead_assign" />,
  ops_lead_follow: <ProductPlaceholder moduleCode="ops_lead_follow" />,
  ops_lead_convert: <ProductPlaceholder moduleCode="ops_lead_convert" />,
  // 运营流程 - 客户管理
  ops_customer_archive: <ProductPlaceholder moduleCode="ops_customer_archive" />,
  ops_customer_tag: <ProductPlaceholder moduleCode="ops_customer_tag" />,
  ops_customer_intent: <ProductPlaceholder moduleCode="ops_customer_intent" />,
  ops_customer_visit: <ProductPlaceholder moduleCode="ops_customer_visit" />,
  ops_customer_remind: <ProductPlaceholder moduleCode="ops_customer_remind" />,
  // 运营流程 - 订单财务
  ops_order_center: <ProductPlaceholder moduleCode="ops_order_center" />,
  ops_contract_mgmt: <ProductPlaceholder moduleCode="ops_contract_mgmt" />,
  ops_receipt_record: <ProductPlaceholder moduleCode="ops_receipt_record" />,
  ops_invoice_mgmt: <ProductPlaceholder moduleCode="ops_invoice_mgmt" />,
  ops_settlement: <ProductPlaceholder moduleCode="ops_settlement" />,
  // 运营流程 - 营销推广
  ops_enroll_brochure: <ProductPlaceholder moduleCode="ops_enroll_brochure" />,
  ops_activity_mkt: <ProductPlaceholder moduleCode="ops_activity_mkt" />,
  ops_coupon_card: <ProductPlaceholder moduleCode="ops_coupon_card" />,
  ops_channel_qrcode: <ProductPlaceholder moduleCode="ops_channel_qrcode" />,
  ops_distribution: <ProductPlaceholder moduleCode="ops_distribution" />,
  // 运营流程 - 数据看板
  ops_dashboard_funnel: <ProductPlaceholder moduleCode="ops_dashboard_funnel" />,
  ops_dashboard_rank: <ProductPlaceholder moduleCode="ops_dashboard_rank" />,
  ops_dashboard_convert: <ProductPlaceholder moduleCode="ops_dashboard_convert" />,
  ops_dashboard_target: <ProductPlaceholder moduleCode="ops_dashboard_target" />,
  ops_dashboard_roi: <ProductPlaceholder moduleCode="ops_dashboard_roi" />,
  // 运营流程 - 课程开发流程
  ops_std_dev_flow: <ProductPlaceholder moduleCode="ops_std_dev_flow" />,
  ops_question_dev_flow: <ProductPlaceholder moduleCode="ops_question_dev_flow" />,
  ops_video_dev_flow: <ProductPlaceholder moduleCode="ops_video_dev_flow" />,
  ops_simulation_dev_flow: <ProductPlaceholder moduleCode="ops_simulation_dev_flow" />,
  ops_tech_doc_dev_flow: <ProductPlaceholder moduleCode="ops_tech_doc_dev_flow" />,
  // 运营流程 - 培训运营流程
  ops_train_demand_survey: <ProductPlaceholder moduleCode="ops_train_demand_survey" />,
  ops_train_solution_design: <ProductPlaceholder moduleCode="ops_train_solution_design" />,
  ops_train_class_mgmt: <ProductPlaceholder moduleCode="ops_train_class_mgmt" />,
  ops_train_implementation: <ProductPlaceholder moduleCode="ops_train_implementation" />,
  ops_train_cert_issue: <ProductPlaceholder moduleCode="ops_train_cert_issue" />,
  ops_train_graduation: <TrainingGraduationCeremonyPage />,
  ops_train_project_close: <ProductPlaceholder moduleCode="ops_train_project_close" />,
  ops_train_project_apply: <ProductPlaceholder moduleCode="ops_train_project_apply" />,
  // 培训运营 - 需求调研
  ops_talent_demand: <ProductPlaceholder moduleCode="ops_talent_demand" />,
  ops_training_demand: <ProductPlaceholder moduleCode="ops_training_demand" />,
  // 培训运营 - 方案设计
  ops_class_split_setting: <ProductPlaceholder moduleCode="ops_class_split_setting" />,
  ops_schedule_setting: <ProductPlaceholder moduleCode="ops_schedule_setting" />,
  ops_incentive_setting: <ProductPlaceholder moduleCode="ops_incentive_setting" />,
  ops_budget_setting: <ProductPlaceholder moduleCode="ops_budget_setting" />,
  ops_ai_learning_task_setting: <ProductPlaceholder moduleCode="ops_ai_learning_task_setting" />,
  ops_academic_division_setting: <ProductPlaceholder moduleCode="ops_academic_division_setting" />,
  // 培训运营 - 开班管理
  ops_material_generate: <ProductPlaceholder moduleCode="ops_material_generate" />,
  ops_material_apply: <ProductPlaceholder moduleCode="ops_material_apply" />,
  ops_mobilization_meeting: <ProductPlaceholder moduleCode="ops_mobilization_meeting" />,
  // 培训运营 - 培训实施
  ops_attendance_mgmt: <ProductPlaceholder moduleCode="ops_attendance_mgmt" />,
  ops_pass_rate_mgmt: <ProductPlaceholder moduleCode="ops_pass_rate_mgmt" />,
  ops_homework_mgmt: <ProductPlaceholder moduleCode="ops_homework_mgmt" />,
  ops_evaluation_mgmt: <ProductPlaceholder moduleCode="ops_evaluation_mgmt" />,
  ops_record_mgmt: <ProductPlaceholder moduleCode="ops_record_mgmt" />,
  // 培训运营 - 项目结题
  ops_subsidy_material_apply: <ProductPlaceholder moduleCode="ops_subsidy_material_apply" />,
  ops_subsidy_material_export: <ProductPlaceholder moduleCode="ops_subsidy_material_export" />,
  // 培训运营 - 项目申报
  ops_qianlv_reward_apply: <ProductPlaceholder moduleCode="ops_qianlv_reward_apply" />,
  ops_gov_reward_guide: <ProductPlaceholder moduleCode="ops_gov_reward_guide" />,
  ops_enterprise_reward_apply: <ProductPlaceholder moduleCode="ops_enterprise_reward_apply" />,
  ops_college_reward_apply: <ProductPlaceholder moduleCode="ops_college_reward_apply" />,
  ops_student_service_flow: <ProductPlaceholder moduleCode="ops_student_service_flow" />,
  ops_title_apply_flow: <ProductPlaceholder moduleCode="ops_title_apply_flow" />,
  ops_subsidy_apply_flow: <ProductPlaceholder moduleCode="ops_subsidy_apply_flow" />,
  ops_points_exchange_flow: <ProductPlaceholder moduleCode="ops_points_exchange_flow" />,
  ops_expert_apply_flow: <ProductPlaceholder moduleCode="ops_expert_apply_flow" />,
  ops_honor_apply_flow: <ProductPlaceholder moduleCode="ops_honor_apply_flow" />,
  // 认定运营流程
  ops_cert_notice_mgmt: <ProductPlaceholder moduleCode="ops_cert_notice_mgmt" />,
  ops_cert_qualification: <ProductPlaceholder moduleCode="ops_cert_qualification" />,
  ops_cert_plan_mgmt: <ProductPlaceholder moduleCode="ops_cert_plan_mgmt" />,
  ops_cert_exam_arrange: <ProductPlaceholder moduleCode="ops_cert_exam_arrange" />,
  ops_cert_pre_exam_prep: <ProductPlaceholder moduleCode="ops_cert_pre_exam_prep" />,
  ops_cert_exam_meeting: <ProductPlaceholder moduleCode="ops_cert_exam_meeting" />,
  ops_cert_assessment: <ProductPlaceholder moduleCode="ops_cert_assessment" />,
  ops_cert_score_publish: <ProductPlaceholder moduleCode="ops_cert_score_publish" />,
  // 竞赛运营流程
  ops_comp_apply: <ProductPlaceholder moduleCode="ops_comp_apply" />,
  ops_comp_register: <ProductPlaceholder moduleCode="ops_comp_register" />,
  ops_comp_tech_seminar: <ProductPlaceholder moduleCode="ops_comp_tech_seminar" />,
  ops_comp_enterprise_sel: <ProductPlaceholder moduleCode="ops_comp_enterprise_sel" />,
  ops_comp_preliminary: <ProductPlaceholder moduleCode="ops_comp_preliminary" />,
  ops_comp_semifinal: <ProductPlaceholder moduleCode="ops_comp_semifinal" />,
  ops_comp_pre_training: <ProductPlaceholder moduleCode="ops_comp_pre_training" />,
  ops_comp_opening: <ProductPlaceholder moduleCode="ops_comp_opening" />,
  ops_comp_final: <ProductPlaceholder moduleCode="ops_comp_final" />,
  ops_comp_closing: <ProductPlaceholder moduleCode="ops_comp_closing" />,
  ops_comp_project_close: <ProductPlaceholder moduleCode="ops_comp_project_close" />,
  // 竞赛结题
  ops_comp_close_report: <ProductPlaceholder moduleCode="ops_comp_close_report" />,
  ops_comp_close_settlement: <ProductPlaceholder moduleCode="ops_comp_close_settlement" />,
  ops_comp_close_apply: <ProductPlaceholder moduleCode="ops_comp_close_apply" />,
  // 支持保障 - 政策库
  sup_policy_national: <ProductPlaceholder moduleCode="sup_policy_national" />,
  sup_policy_provincial: <ProductPlaceholder moduleCode="sup_policy_provincial" />,
  sup_policy_city: <ProductPlaceholder moduleCode="sup_policy_city" />,
  sup_policy_company: <ProductPlaceholder moduleCode="sup_policy_company" />,
  // 支持保障 - 专家库
  sup_expert_question: <ProductPlaceholder moduleCode="sup_expert_question" />,
  sup_trainer_mgmt: <ProductPlaceholder moduleCode="sup_trainer_mgmt" />,
  sup_assessor_mgmt: <ProductPlaceholder moduleCode="sup_assessor_mgmt" />,
  sup_referee_mgmt: <ProductPlaceholder moduleCode="sup_referee_mgmt" />,
  sup_academic_staff: <ProductPlaceholder moduleCode="sup_academic_staff" />,
  sup_exam_staff: <ProductPlaceholder moduleCode="sup_exam_staff" />,
  sup_comp_staff: <ProductPlaceholder moduleCode="sup_comp_staff" />,
  // 支持保障 - 设备库
  sup_equip_practical: <ProductPlaceholder moduleCode="sup_equip_practical" />,
  sup_equip_training: <ProductPlaceholder moduleCode="sup_equip_training" />,
  sup_equip_cert: <ProductPlaceholder moduleCode="sup_equip_cert" />,
  sup_equip_comp: <ProductPlaceholder moduleCode="sup_equip_comp" />,
  // 支持保障 - 物料库
  sup_job_mgmt: <ProductPlaceholder moduleCode="sup_job_mgmt" />,
  sup_post_mgmt: <ProductPlaceholder moduleCode="sup_post_mgmt" />,
  sup_standard_mgmt: <ProductPlaceholder moduleCode="sup_standard_mgmt" />,
  sup_question_bank: <ProductPlaceholder moduleCode="sup_question_bank" />,
  sup_textbook_mgmt: <ProductPlaceholder moduleCode="sup_textbook_mgmt" />,
  sup_video_mgmt: <ProductPlaceholder moduleCode="sup_video_mgmt" />,
  sup_material_mgmt: <ProductPlaceholder moduleCode="sup_material_mgmt" />,
  sup_ad_mgmt: <ProductPlaceholder moduleCode="sup_ad_mgmt" />,
  sup_logo_mgmt: <ProductPlaceholder moduleCode="sup_logo_mgmt" />,
  sup_budget_mgmt: <ProductPlaceholder moduleCode="sup_budget_mgmt" />,
  // 支持保障 - 方法库
  sup_rule_standard: <ProductPlaceholder moduleCode="sup_rule_standard" />,
  sup_rule_question: <ProductPlaceholder moduleCode="sup_rule_question" />,
  sup_rule_textbook: <ProductPlaceholder moduleCode="sup_rule_textbook" />,
  sup_rule_video: <ProductPlaceholder moduleCode="sup_rule_video" />,
  sup_rule_practical: <ProductPlaceholder moduleCode="sup_rule_practical" />,
  sup_rule_forum: <ProductPlaceholder moduleCode="sup_rule_forum" />,
  sup_rule_training: <ProductPlaceholder moduleCode="sup_rule_training" />,
  sup_rule_cert: <ProductPlaceholder moduleCode="sup_rule_cert" />,
  sup_rule_comp: <ProductPlaceholder moduleCode="sup_rule_comp" />,
  sup_rule_ceremony: <ProductPlaceholder moduleCode="sup_rule_ceremony" />,
  // 支持保障 - 环境库
  sup_env_forum: <ProductPlaceholder moduleCode="sup_env_forum" />,
  sup_env_question_accept: <ProductPlaceholder moduleCode="sup_env_question_accept" />,
  sup_env_ceremony: <ProductPlaceholder moduleCode="sup_env_ceremony" />,
  sup_env_training_class: <ProductPlaceholder moduleCode="sup_env_training_class" />,
  sup_env_cert_exam: <ProductPlaceholder moduleCode="sup_env_cert_exam" />,
  sup_env_comp_site: <ProductPlaceholder moduleCode="sup_env_comp_site" />,
  // 绩效人才
  perf_assess_structure: <ProductPlaceholder moduleCode="perf_assess_structure" />,
  perf_assess_qualification: <ProductPlaceholder moduleCode="perf_assess_qualification" />,
  perf_assess_resource: <ProductPlaceholder moduleCode="perf_assess_resource" />,
  perf_skill_map: <ProductPlaceholder moduleCode="perf_skill_map" />,
  perf_pipeline_build: <ProductPlaceholder moduleCode="perf_pipeline_build" />,
  perf_talent_map: <ProductPlaceholder moduleCode="perf_talent_map" />,
  // 机构风控
  risk_product_design: <ProductPlaceholder moduleCode="risk_product_design" />,
  risk_market_expand: <ProductPlaceholder moduleCode="risk_market_expand" />,
  risk_course_dev: <ProductPlaceholder moduleCode="risk_course_dev" />,
  risk_agent_dev: <ProductPlaceholder moduleCode="risk_agent_dev" />,
  risk_cloud_dev: <ProductPlaceholder moduleCode="risk_cloud_dev" />,
  risk_simulation_dev: <ProductPlaceholder moduleCode="risk_simulation_dev" />,
  risk_training_ops: <ProductPlaceholder moduleCode="risk_training_ops" />,
  risk_cert_ops: <ProductPlaceholder moduleCode="risk_cert_ops" />,
  risk_competition_ops: <ProductPlaceholder moduleCode="risk_competition_ops" />,
  risk_hr: <ProductPlaceholder moduleCode="risk_hr" />,
  risk_finance: <ProductPlaceholder moduleCode="risk_finance" />,
  risk_admin: <ProductPlaceholder moduleCode="risk_admin" />,
  risk_platform_coop: <ProductPlaceholder moduleCode="risk_platform_coop" />,
  'certification-practical-collection': <CollectionTaskListPage />,
  competition_event: <CompetitionEventManagementPage />,
  competition_player: <CompetitionPlayerManagementPage />,
  competition_player_info: <CompetitionPlayerManagementPage />,
  competition_player_sorting: <CompetitionPlayerSortingManagementPage />,
  competition_referee: <CompetitionRefereeManagementPage />,
  competition_referee_info: <CompetitionRefereeManagementPage />,
  competition_referee_group: <CompetitionRefereeGroupPage />,
  competition_stage: <CompetitionModuleShell module="stages" />,
  competition_notice: <CompetitionModuleShell module="notices" />,
  competition_theory_exam: <CompetitionModuleShell module="theory-exam" />,
  competition_formal_exam: <CompetitionModuleShell module="formal-exam-console" />,
  competition_practical_exam: <CompetitionModuleShell module="practical-exam-console" />,
  competition_score_ranking: <CompetitionModuleShell module="scores" />,
  competition_theory_question: <CompetitionQuestionBankPage contentType="theory" />,
  competition_practical_question: <CompetitionQuestionBankPage contentType="practical" />,
  competition_simple_theory_question: <CompetitionQuestionBankPage contentType="theory" simpleMode />,
  competition_simple_practical_question: <CompetitionSimplePracticalQuestionPage />,
  competition_theory_blueprint: <CompetitionQuestionBlueprintPage contentType="theory" />,
  competition_practical_blueprint: <CompetitionQuestionBlueprintPage contentType="practical" />,
  competition_theory_make_exam_plan: <CompetitionMakeExamPlanPage contentType="theory" />,
  competition_practical_make_exam_plan: <CompetitionMakeExamPlanPage contentType="practical" />,
  competition_simple_theory_make_exam_plan: <CompetitionMakeExamPlanPage contentType="theory" simpleMode />,
  competition_simple_practical_make_exam_plan: <CompetitionPracticalScoringSchemePage />,
};

function useBrowserPathname() {
  const [pathname, setPathname] = React.useState(() => window.location.pathname);

  React.useEffect(() => {
    const syncPathname = () => setPathname(window.location.pathname);
    window.addEventListener('popstate', syncPathname);
    return () => window.removeEventListener('popstate', syncPathname);
  }, []);

  return pathname;
}

function CollectionManageShell({ taskId, examSessionId }: { taskId: string; examSessionId?: string }) {
  const [manageActive, setManageActive] = React.useState(true);

  // §3.7 覆盖态标签：进入覆盖态 = 覆盖态标签进列表 + 记录 overridePath（覆盖态回退载体）
  React.useEffect(() => {
    useTabStore.getState().openTab('certification_practical_collection');
    useTabStore.getState().setOverridePath('certification_practical_collection',
      window.location.pathname + window.location.search);
  }, [taskId]);

  return (
    <AppShell
      pageMap={pageMap}
      selectedMenuKeyOverride={manageActive ? 'certification_practical_collection' : undefined}
      contentOverride={manageActive ? (
        <CollectionManagePage taskId={taskId} examSessionId={examSessionId} />
      ) : undefined}
      onMenuNavigate={() => setManageActive(false)}
    />
  );
}

function CompetitionModuleShell({ module }: { module: CompetitionEventModule }) {
  const { confirmNavigation } = useNavigationBlocker();
  const searchParams = new URLSearchParams(window.location.search);
  const navigate = async (path: string) => {
    if (!(await confirmNavigation())) return;
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
  };

  return (
    <CompetitionEventModulePage
      module={module}
      onNavigate={navigate}
      initialEventId={searchParams.get('eventId') || undefined}
      initialStageId={searchParams.get('stageId') || undefined}
      initialEventWorkTypeId={searchParams.get('eventWorkTypeId') || undefined}
    />
  );
}

function CompetitionEventShell({ pathname }: { pathname: string }) {
  const [eventActive, setEventActive] = React.useState(true);
  const menuTree = useAuthStore((state) => state.menuTree);
  const competitionMenuKey = findCompetitionMenuCode(menuTree, pathname) || 'competition_event';
  const competitionPage = pageMap[competitionMenuKey] || pageMap.competition_event;

  React.useEffect(() => {
    setEventActive(true);
    // §3.7 覆盖态标签：进入覆盖态 = 覆盖态标签进列表 + 记录 overridePath（覆盖态回退载体）
    useTabStore.getState().openTab(competitionMenuKey);
    useTabStore.getState().setOverridePath(competitionMenuKey,
      window.location.pathname + window.location.search);
  }, [pathname, competitionMenuKey]);

  return (
    <AppShell
      pageMap={pageMap}
      selectedMenuKeyOverride={eventActive ? competitionMenuKey : undefined}
      contentOverride={eventActive ? <React.Fragment key={pathname}>{competitionPage}</React.Fragment> : undefined}
      onMenuNavigate={(node) => {
        if (node?.path && isCompetitionPath(node.path)) {
          setEventActive(true);
          if (node.path !== window.location.pathname) {
            window.history.pushState({}, '', node.path);
            window.dispatchEvent(new PopStateEvent('popstate'));
          }
          return;
        }
        setEventActive(false);
      }}
      hideBreadcrumb={eventActive}
    />
  );
}

/** AI 同步学统计隐藏路由：/training/exam-lists/:examId/review-sync-stats[/student/:planId]
 *  （《AI同步学统计功能设计方案》v1.1 §6.1：仅考试管理列表页行内按钮进入，不进菜单） */
function isReviewSyncStatsPath(pathname: string) {
  return /^\/training\/exam-lists\/\d+\/review-sync-stats/.test(pathname);
}

function ReviewSyncStatsShell({ pathname }: { pathname: string }) {
  const [statsActive, setStatsActive] = React.useState(true);
  const match = pathname.match(/^\/training\/exam-lists\/(\d+)\/review-sync-stats(?:\/student\/(\d+))?\/?$/);
  const examId = match ? Number(match[1]) : 0;
  const planId = match?.[2];

  React.useEffect(() => {
    setStatsActive(true);
    // §3.7 覆盖态标签：进入覆盖态 = 覆盖态标签进列表 + 记录 overridePath（覆盖态回退载体）
    useTabStore.getState().openTab('train_exam-list');
    useTabStore.getState().setOverridePath('train_exam-list',
      window.location.pathname + window.location.search);
  }, [pathname]);

  return (
    <AppShell
      pageMap={pageMap}
      selectedMenuKeyOverride={statsActive ? 'train_exam-list' : undefined}
      contentOverride={statsActive ? (
        <React.Fragment key={pathname}>
          {planId
            ? <StudentDetailPage examId={examId} planId={planId} />
            : <StatsOverviewPage examId={examId} />}
        </React.Fragment>
      ) : undefined}
      onMenuNavigate={() => setStatsActive(false)}
      hideBreadcrumb={statsActive}
    />
  );
}

/** 毕业典礼班级详情隐藏路由：/training/graduation/classes/:classId */
function isGraduationClassDetailPath(pathname: string) {
  return /^\/training\/graduation\/classes\/\d+\/?$/.test(pathname);
}

function GraduationClassDetailShell({ pathname }: { pathname: string }) {
  const [detailActive, setDetailActive] = React.useState(true);
  const match = pathname.match(/^\/training\/graduation\/classes\/(\d+)\/?$/);
  const classId = match ? Number(match[1]) : 0;

  React.useEffect(() => {
    setDetailActive(true);
    // §3.7 覆盖态标签：进入覆盖态 = 覆盖态标签进列表 + 记录 overridePath（覆盖态回退载体）
    useTabStore.getState().openTab('train_graduation_ceremony');
    useTabStore.getState().setOverridePath('train_graduation_ceremony',
      window.location.pathname + window.location.search);
  }, [pathname]);

  return (
    <AppShell
      pageMap={pageMap}
      selectedMenuKeyOverride={detailActive ? 'train_graduation_ceremony' : undefined}
      contentOverride={
        detailActive ? (
          <React.Fragment key={pathname}>
            <TrainingGraduationClassDetailPage classId={classId} />
          </React.Fragment>
        ) : undefined
      }
      onMenuNavigate={() => setDetailActive(false)}
      hideBreadcrumb={detailActive}
    />
  );
}

function App() {
  const pathname = useBrowserPathname();
  const formalExamMode = pathname === '/formal-exam' || pathname.startsWith('/formal-exam/');
  const assessorMode = pathname.startsWith('/assessor-scoring');
  const defenseMode = pathname.startsWith('/assessor-defense');
  const practicalCollectionMode = pathname.startsWith('/practical-collection');
  const competitionJudgeMode = pathname.startsWith('/competition-practical-judge');
  const collectionManageTaskId = pathname.match(/^\/certification\/practical-collection\/([^/]+)\/manage\/?$/)?.[1];
  const collectionManageExamSessionId = new URLSearchParams(window.location.search).get('examSessionId') || undefined;
  const collectionManageMode = Boolean(collectionManageTaskId);
  const competitionEventMode = isCompetitionPath(pathname);
  const reviewSyncStatsMode = isReviewSyncStatsPath(pathname);
  const graduationClassDetailMode = isGraduationClassDetailPath(pathname);

  return (
    <NavigationBlockerProvider>
      {formalExamMode ? (
        <FormalExamPlayerApp />
      ) : (
      <AuthGate mode={assessorMode ? 'assessor' : defenseMode ? 'defense' : practicalCollectionMode ? 'collection' : competitionJudgeMode ? 'competitionJudge' : 'admin'}>
        {competitionJudgeMode ? (
          <CompetitionPracticalJudgeApp />
        ) : assessorMode ? (
          <TabletAssessorApp />
        ) : defenseMode ? (
          <DefenseReviewApp />
        ) : practicalCollectionMode ? (
          <PracticalCollectionApp />
        ) : collectionManageMode ? (
          <CollectionManageShell
            taskId={decodeURIComponent(collectionManageTaskId!)}
            examSessionId={collectionManageExamSessionId}
          />
        ) : graduationClassDetailMode ? (
          <GraduationClassDetailShell pathname={pathname} />
        ) : reviewSyncStatsMode ? (
          <ReviewSyncStatsShell pathname={pathname} />
        ) : competitionEventMode ? (
          <CompetitionEventShell pathname={pathname} />
        ) : (
          <AppShell pageMap={pageMap} />
        )}
      </AuthGate>
      )}
    </NavigationBlockerProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>,
);
