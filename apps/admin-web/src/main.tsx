import './utils/legacyBrowserPolyfills';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import type { CompetitionEventModule } from './pages/competition/CompetitionEventModulePage';
import { findCompetitionMenuCode, isCompetitionPath } from './routes/competitionRoute';
import { useAuthStore } from './stores/authStore';
import { useTabStore } from './stores/tabStore';
import { NavigationBlockerProvider, useNavigationBlocker } from './hooks/useUnsavedChangesBlocker';

import { appTheme } from './theme';
import './styles/tokens.css';
import './styles/global.css';

// 页面级懒加载：pageMap 全量页面拆包，进入对应菜单才下载 chunk（§ 前端构建优化）
const KnowledgePage = React.lazy(() => import('./pages/knowledge/KnowledgePage').then((m) => ({ default: m.KnowledgePage })));
const ResourceCenterPage = React.lazy(() => import('./pages/question_bank/ResourceCenterPage'));
const KnowledgeFixPage = React.lazy(() => import('./pages/question_bank/KnowledgeFixPage'));
const XiMuBiaoGuanLiPage = React.lazy(() => import('./pages/zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage'));
const TheoryQuestionGenPage = React.lazy(() => import('./pages/zhi_neng_ke_cheng_cai_liao_guan_li/TheoryQuestionGenPage'));
const SkillQuestionGenPage = React.lazy(() => import('./pages/zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage'));
const AiCoursewarePage = React.lazy(() => import('./pages/zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage'));
const RencaishibieEmbedPage = React.lazy(() => import('./pages/knowledge/RencaishibieEmbedPage').then((m) => ({ default: m.RencaishibieEmbedPage })));
const WorkTypePage = React.lazy(() => import('./pages/exam/WorkTypePage').then((m) => ({ default: m.WorkTypePage })));
const CoursePage = React.lazy(() => import('./pages/exam/CoursePage').then((m) => ({ default: m.CoursePage })));
const DevTaskDetailPage = React.lazy(() => import('./pages/exam/DevTaskDetailPage').then((m) => ({ default: m.DevTaskDetailPage })));
const TaskExamAuditAssignListPage = React.lazy(() => import('./pages/exam/TaskExamAuditAssignListPage').then((m) => ({ default: m.TaskExamAuditAssignListPage })));
const TaskExamshicaoAuditAssignListPage = React.lazy(() => import('./pages/exam/TaskExamshicaoAuditAssignListPage').then((m) => ({ default: m.TaskExamshicaoAuditAssignListPage })));
const TaskExamsubAuditAssignListPage = React.lazy(() => import('./pages/exam/TaskExamsubAuditAssignListPage').then((m) => ({ default: m.TaskExamsubAuditAssignListPage })));
const TrainingInfoPage = React.lazy(() => import('./pages/training/TrainingInfoPage').then((m) => ({ default: m.TrainingInfoPage })));
const TrainingDashboardPage = React.lazy(() => import('./pages/training/TrainingDashboardPage').then((m) => ({ default: m.TrainingDashboardPage })));
const TrainingFollowClassPage = React.lazy(() => import('./pages/training/TrainingFollowClassPage').then((m) => ({ default: m.TrainingFollowClassPage })));
const TrainingFollowExamPage = React.lazy(() => import('./pages/training/TrainingFollowExamPage').then((m) => ({ default: m.TrainingFollowExamPage })));
const TrainingSubsidyDeclarePage = React.lazy(() => import('./pages/training/TrainingSubsidyDeclarePage').then((m) => ({ default: m.TrainingSubsidyDeclarePage })));
const TrainingClassPeoplePage = React.lazy(() => import('./pages/training/TrainingClassPeoplePage').then((m) => ({ default: m.TrainingClassPeoplePage })));
const TrainingClassDivisionPage = React.lazy(() => import('./pages/training/TrainingClassDivisionPage').then((m) => ({ default: m.TrainingClassDivisionPage })));
const TrainingClassDeclarePage = React.lazy(() => import('./pages/training/TrainingClassDeclarePage').then((m) => ({ default: m.TrainingClassDeclarePage })));
const UserAuthCoursePage = React.lazy(() => import('./pages/exam/UserAuthCoursePage').then((m) => ({ default: m.UserAuthCoursePage })));
const UserListPage = React.lazy(() => import('./pages/chazheng/UserListPage').then((m) => ({ default: m.UserListPage })));
const CertQueryPage = React.lazy(() => import('./pages/chazheng/CertQueryPage').then((m) => ({ default: m.CertQueryPage })));
const RequirementsManagementPage = React.lazy(() => import('./pages/training/RequirementsManagementPage').then((m) => ({ default: m.RequirementsManagementPage })));
const EnterpriseTrainingDemandPage = React.lazy(() => import('./pages/training/EnterpriseTrainingDemandPage').then((m) => ({ default: m.EnterpriseTrainingDemandPage })));
const EnterpriseTalentDemandPage = React.lazy(() => import('./pages/training/EnterpriseTalentDemandPage').then((m) => ({ default: m.EnterpriseTalentDemandPage })));
const TrainingProgramPage = React.lazy(() => import('./pages/training/TrainingProgramPage').then((m) => ({ default: m.TrainingProgramPage })));
const TrainingSchedulePage = React.lazy(() => import('./pages/training/TrainingSchedulePage').then((m) => ({ default: m.TrainingSchedulePage })));
const TrainingGraduationPage = React.lazy(() => import('./pages/training/TrainingGraduationPage').then((m) => ({ default: m.TrainingGraduationPage })));
const TrainingGraduationCeremonyPage = React.lazy(() => import('./pages/training/TrainingGraduationCeremonyPage').then((m) => ({ default: m.TrainingGraduationCeremonyPage })));
const TrainingGraduationClassDetailPage = React.lazy(() => import('./pages/training/TrainingGraduationClassDetailPage').then((m) => ({ default: m.TrainingGraduationClassDetailPage })));
const TrainingCapabilityPage = React.lazy(() => import('./pages/training/TrainingCapabilityPage').then((m) => ({ default: m.TrainingCapabilityPage })));
const TrainingSummaryPage = React.lazy(() => import('./pages/training/TrainingSummaryPage').then((m) => ({ default: m.TrainingSummaryPage })));
const TrainingCertificateTemplatePage = React.lazy(() => import('./pages/training/TrainingCertificateTemplatePage').then((m) => ({ default: m.TrainingCertificateTemplatePage })));
const TrainingInformationPage = React.lazy(() => import('./pages/training/TrainingInformationPage').then((m) => ({ default: m.TrainingInformationPage })));
const TrainingStudentPage = React.lazy(() => import('./pages/training/TrainingStudentPage').then((m) => ({ default: m.TrainingStudentPage })));
const TrainingAcademicManagePage = React.lazy(() => import('./pages/training/TrainingAcademicManagePage').then((m) => ({ default: m.TrainingAcademicManagePage })));
const EnterpriseResearchPage = React.lazy(() => import('./pages/training/EnterpriseResearchPage').then((m) => ({ default: m.EnterpriseResearchPage })));
const EnterpriseManagePage = React.lazy(() => import('./pages/training/EnterpriseManagePage').then((m) => ({ default: m.EnterpriseManagePage })));
const SolutionPlanningPage = React.lazy(() => import('./pages/training/SolutionPlanningPage').then((m) => ({ default: m.SolutionPlanningPage })));
const EnterprisePlanReviewPage = React.lazy(() => import('./pages/training/EnterprisePlanReviewPage').then((m) => ({ default: m.EnterprisePlanReviewPage })));
const DataPanelPage = React.lazy(() => import('./pages/zhixunplatform/DataPanelPage').then((m) => ({ default: m.DataPanelPage })));
const TaskManagementPage = React.lazy(() => import('./pages/zhixunplatform/TaskManagementPage').then((m) => ({ default: m.TaskManagementPage })));
const TaskArchivesPage = React.lazy(() => import('./pages/zhixunplatform/TaskArchivesPage').then((m) => ({ default: m.TaskArchivesPage })));
const TrainingProcessPage = React.lazy(() => import('./pages/zhixunplatform/TrainingProcessPage').then((m) => ({ default: m.TrainingProcessPage })));
const ProjectArchivesPage = React.lazy(() => import('./pages/zhixunplatform/ProjectArchivesPage').then((m) => ({ default: m.ProjectArchivesPage })));
const MaterialArchivesPage = React.lazy(() => import('./pages/zhixunplatform/MaterialArchivesPage').then((m) => ({ default: m.MaterialArchivesPage })));
const TalentIdentificationPage = React.lazy(() => import('./pages/zhixunplatform/TalentIdentificationPage').then((m) => ({ default: m.TalentIdentificationPage })));
const PreliminaryPlanPage = React.lazy(() => import('./pages/zhixunplatform/PreliminaryPlanPage').then((m) => ({ default: m.PreliminaryPlanPage })));
const CreateTaskPage = React.lazy(() => import('./pages/zhixunplatform/CreateTaskPage').then((m) => ({ default: m.CreateTaskPage })));
const FinalPlanPage = React.lazy(() => import('./pages/zhixunplatform/FinalPlanPage').then((m) => ({ default: m.FinalPlanPage })));
const ClassDeclarationPage = React.lazy(() => import('./pages/zhixunplatform/ClassDeclarationPage').then((m) => ({ default: m.ClassDeclarationPage })));
const TrainingImplementationPage = React.lazy(() => import('./pages/zhixunplatform/TrainingImplementationPage').then((m) => ({ default: m.TrainingImplementationPage })));
const CertificationPage = React.lazy(() => import('./pages/zhixunplatform/CertificationPage').then((m) => ({ default: m.CertificationPage })));
const DeclarationServicePage = React.lazy(() => import('./pages/zhixunplatform/DeclarationServicePage').then((m) => ({ default: m.DeclarationServicePage })));
const GraduationPage = React.lazy(() => import('./pages/zhixunplatform/GraduationPage').then((m) => ({ default: m.GraduationPage })));
const RefundPage = React.lazy(() => import('./pages/zhixunplatform/RefundPage').then((m) => ({ default: m.RefundPage })));
const PersonnelArchivePage = React.lazy(() => import('./pages/zhixunplatform/PersonnelArchivePage').then((m) => ({ default: m.PersonnelArchivePage })));
const TaskStartPage = React.lazy(() => import('./pages/expert/TaskStartPage').then((m) => ({ default: m.TaskStartPage })));
const QuestionBankPage = React.lazy(() => import('./pages/exam/QuestionBankPage').then((m) => ({ default: m.QuestionBankPage })));
const CompanyQuestionPage = React.lazy(() => import('./pages/exam/CompanyQuestionPage').then((m) => ({ default: m.CompanyQuestionPage })));
const MakeExamPlanPage = React.lazy(() => import('./pages/exam/MakeExamPlanPage').then((m) => ({ default: m.MakeExamPlanPage })));
const KnowledgeRatePage = React.lazy(() => import('./pages/exam/KnowledgeRatePage').then((m) => ({ default: m.KnowledgeRatePage })));
const KnowledgeStructurePage = React.lazy(() => import('./pages/exam/KnowledgeStructurePage').then((m) => ({ default: m.KnowledgeStructurePage })));
const FaqConversionPage = React.lazy(() => import('./pages/exam/FaqConversionPage').then((m) => ({ default: m.FaqConversionPage })));
const TrainExamListPage = React.lazy(() => import('./pages/training/TrainExamListPage').then((m) => ({ default: m.TrainExamListPage })));
const StatsOverviewPage = React.lazy(() => import('./pages/exam/reviewSyncStats/StatsOverviewPage').then((m) => ({ default: m.StatsOverviewPage })));
const StudentDetailPage = React.lazy(() => import('./pages/exam/reviewSyncStats/StudentDetailPage').then((m) => ({ default: m.StudentDetailPage })));
const TabletAssessorApp = React.lazy(() => import('./pages/assessor/TabletAssessorApp').then((m) => ({ default: m.TabletAssessorApp })));
const DefenseReviewApp = React.lazy(() => import('./pages/assessor/DefenseReviewApp').then((m) => ({ default: m.DefenseReviewApp })));
const PracticalCollectionApp = React.lazy(() => import('./pages/practicalCollection/PracticalCollectionApp').then((m) => ({ default: m.PracticalCollectionApp })));
const CompetitionEventManagementPage = React.lazy(() => import('./pages/competition/CompetitionEventManagementPage').then((m) => ({ default: m.CompetitionEventManagementPage })));
const CompetitionPlayerManagementPage = React.lazy(() => import('./pages/competition/CompetitionPlayerManagementPage').then((m) => ({ default: m.CompetitionPlayerManagementPage })));
const CompetitionPlayerSortingManagementPage = React.lazy(() => import('./pages/competition/CompetitionPlayerSortingPage').then((m) => ({ default: m.CompetitionPlayerSortingManagementPage })));
const CompetitionPlayerDrawManagementPage = React.lazy(() => import('./pages/competition/CompetitionPlayerDrawPage').then((m) => ({ default: m.CompetitionPlayerDrawManagementPage })));
const CompetitionPlayerClassifyManagementPage = React.lazy(() => import('./pages/competition/CompetitionPlayerClassifyPage').then((m) => ({ default: m.CompetitionPlayerClassifyManagementPage })));
const CompetitionRefereeManagementPage = React.lazy(() => import('./pages/competition/CompetitionRefereeManagementPage').then((m) => ({ default: m.CompetitionRefereeManagementPage })));
const CompetitionRefereeGroupPage = React.lazy(() => import('./pages/competition/CompetitionRefereeGroupPage').then((m) => ({ default: m.CompetitionRefereeGroupPage })));
const CompetitionEventModulePage = React.lazy(() => import('./pages/competition/CompetitionEventModulePage').then((m) => ({ default: m.CompetitionEventModulePage })));
const CompetitionQuestionBankPage = React.lazy(() => import('./pages/competition/CompetitionQuestionBankPage').then((m) => ({ default: m.CompetitionQuestionBankPage })));
const CompetitionSimplePracticalQuestionPage = React.lazy(() => import('./pages/competition/CompetitionQuestionBankPage').then((m) => ({ default: m.CompetitionSimplePracticalQuestionPage })));
const CompetitionQuestionBlueprintPage = React.lazy(() => import('./pages/competition/CompetitionQuestionBlueprintPage').then((m) => ({ default: m.CompetitionQuestionBlueprintPage })));
const CompetitionMakeExamPlanPage = React.lazy(() => import('./pages/competition/CompetitionMakeExamPlanPage').then((m) => ({ default: m.CompetitionMakeExamPlanPage })));
const CompetitionPracticalScoringSchemePage = React.lazy(() => import('./pages/competition/CompetitionPracticalScoringSchemePage').then((m) => ({ default: m.CompetitionPracticalScoringSchemePage })));
const CompetitionPracticalJudgeApp = React.lazy(() => import('./pages/competition/CompetitionPracticalJudgeApp').then((m) => ({ default: m.CompetitionPracticalJudgeApp })));
const FormalExamPlayerApp = React.lazy(() => import('./pages/formalExam/FormalExamPlayerApp').then((m) => ({ default: m.FormalExamPlayerApp })));
const ArchiveEquipmentPage = React.lazy(() => import('./pages/zhixunplatform/ArchiveTablePage').then((m) => ({ default: m.createArchiveTablePage('equipment') })));
const ArchiveMaterialsPage = React.lazy(() => import('./pages/zhixunplatform/ArchiveTablePage').then((m) => ({ default: m.createArchiveTablePage('materials') })));
const ArchiveRulesPage = React.lazy(() => import('./pages/zhixunplatform/ArchiveTablePage').then((m) => ({ default: m.createArchiveTablePage('rules') })));
const ArchiveEnvironmentPage = React.lazy(() => import('./pages/zhixunplatform/ArchiveTablePage').then((m) => ({ default: m.createArchiveTablePage('environment') })));
const PmStrategicPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('strategic') })));
const PmOrgPerfPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('orgPerf') })));
const PmPbcPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('pbc') })));
const PmCharterPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('charter') })));
const PmMilestonePage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('milestone') })));
const PmRiskPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('risk') })));
const PmIssuePage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('issue') })));
const PmChangePage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('change') })));
const PmMeetingPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('meeting') })));
const PmKnowledgePage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('knowledge') })));
const PmWbsPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('wbs') })));
const PmCommunicationPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('communication') })));
const PmStatusPage = React.lazy(() => import('./pages/zhixunplatform/PmFormPage').then((m) => ({ default: m.createPmFormPage('status') })));
const ProductPlaceholder = React.lazy(() => import('./pages/product/ProductPlaceholder').then((m) => ({ default: m.ProductPlaceholder })));
const TaskEndPage = React.lazy(() => import('./pages/expert/TaskEndPage').then((m) => ({ default: m.TaskEndPage })));
const ProjectManagementPage = React.lazy(() => import('./pages/certification/ProjectManagementPage').then((m) => ({ default: m.ProjectManagementPage })));
const CertificationDataPanelPage = React.lazy(() => import('./pages/certification/CertificationDataPanelPage').then((m) => ({ default: m.CertificationDataPanelPage })));
const PracticalConfigurationPage = React.lazy(() => import('./pages/certification/PracticalConfigurationPage').then((m) => ({ default: m.PracticalConfigurationPage })));
const PreparationPage = React.lazy(() => import('./pages/certification/PreparationPage').then((m) => ({ default: m.PreparationPage })));
const DefenseManagementPage = React.lazy(() => import('./pages/certification/DefenseManagementPage').then((m) => ({ default: m.DefenseManagementPage })));
const CollectionTaskListPage = React.lazy(() => import('./pages/certification/collection/CollectionTaskListPage').then((m) => ({ default: m.CollectionTaskListPage })));
const CollectionManagePage = React.lazy(() => import('./pages/certification/collection/CollectionManagePage').then((m) => ({ default: m.CollectionManagePage })));
const ArchivePage = React.lazy(() => import('./pages/certification/ArchivePage').then((m) => ({ default: m.ArchivePage })));
const TaskArchivePage = React.lazy(() => import('./pages/certification/TaskArchivePage').then((m) => ({ default: m.TaskArchivePage })));
const AssessorInfoPage = React.lazy(() => import('./pages/certification/info/AssessorInfoPage').then((m) => ({ default: m.AssessorInfoPage })));
const ExamSiteInfoPage = React.lazy(() => import('./pages/certification/info/ExamSiteInfoPage').then((m) => ({ default: m.ExamSiteInfoPage })));
const ExamEquipmentInfoPage = React.lazy(() => import('./pages/certification/info/ExamEquipmentInfoPage').then((m) => ({ default: m.ExamEquipmentInfoPage })));
const ExamStaffInfoPage = React.lazy(() => import('./pages/certification/info/ExamStaffInfoPage').then((m) => ({ default: m.ExamStaffInfoPage })));
const TenantPage = React.lazy(() => import('./pages/system/TenantPage').then((m) => ({ default: m.TenantPage })));
const UserPage = React.lazy(() => import('./pages/system/UserPage').then((m) => ({ default: m.UserPage })));
const OrgPage = React.lazy(() => import('./pages/system/OrgPage').then((m) => ({ default: m.OrgPage })));
const PostPage = React.lazy(() => import('./pages/system/PostPage').then((m) => ({ default: m.PostPage })));
const RolePage = React.lazy(() => import('./pages/system/RolePage').then((m) => ({ default: m.RolePage })));
const MenuPage = React.lazy(() => import('./pages/system/MenuPage').then((m) => ({ default: m.MenuPage })));
const DictPage = React.lazy(() => import('./pages/system/DictPage').then((m) => ({ default: m.DictPage })));
const AuditLogPage = React.lazy(() => import('./pages/system/AuditLogPage').then((m) => ({ default: m.AuditLogPage })));
const PermissionPage = React.lazy(() => import('./pages/system/PermissionPage').then((m) => ({ default: m.PermissionPage })));

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
  competition_player_draw: <CompetitionPlayerDrawManagementPage />,
  competition_player_classify: <CompetitionPlayerClassifyManagementPage />,
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

  // §3.7 覆盖态标签：原子入口记录 overridePath（D1）——先取当前完整 URL 再入 store，
  // openOverrideTab 全程不触碰 URL，杜绝"菜单 path 改写直达 URL"连锁丢页。
  React.useEffect(() => {
    const overrideUrl = window.location.pathname + window.location.search;
    useTabStore.getState().openOverrideTab('certification_practical_collection', undefined, overrideUrl);
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
    // §3.7 覆盖态标签（D1 + 幽灵标签守卫）：menuTree 未到时不开标签（避免兜底码开幽灵标签），
    // 到达后按真实菜单码 openOverrideTab——原子记录 overridePath，全程不触碰 URL。
    if (!menuTree.length) return;
    const overrideUrl = window.location.pathname + window.location.search;
    useTabStore.getState().openOverrideTab(competitionMenuKey, undefined, overrideUrl);
  }, [pathname, competitionMenuKey, menuTree.length]);

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
    // §3.7 覆盖态标签（D1）：先取当前完整 URL 再入 store，原子记录 overridePath 不触碰 URL
    const overrideUrl = window.location.pathname + window.location.search;
    useTabStore.getState().openOverrideTab('kao_shi_ji_hua', undefined, overrideUrl);
  }, [pathname]);

  return (
    <AppShell
      pageMap={pageMap}
      selectedMenuKeyOverride={statsActive ? 'kao_shi_ji_hua' : undefined}
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
    // §3.7 覆盖态标签（D1）：先取当前完整 URL 再入 store，原子记录 overridePath 不触碰 URL
    const overrideUrl = window.location.pathname + window.location.search;
    useTabStore.getState().openOverrideTab('train_graduation_ceremony', undefined, overrideUrl);
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
      <React.Suspense fallback={null}>
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
      </React.Suspense>
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
