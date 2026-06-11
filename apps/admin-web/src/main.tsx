import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
import { WorkTypePage } from './pages/exam/WorkTypePage';
import { CoursePage } from './pages/exam/CoursePage';
import { DevTaskDetailPage } from './pages/exam/DevTaskDetailPage';
import { TaskExamAuditAssignListPage } from './pages/exam/TaskExamAuditAssignListPage';
import { TaskExamshicaoAuditAssignListPage } from './pages/exam/TaskExamshicaoAuditAssignListPage';
import { TaskExamsubAuditAssignListPage } from './pages/exam/TaskExamsubAuditAssignListPage';
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
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
import { TrainingCapabilityPage } from './pages/training/TrainingCapabilityPage';
import { TrainingSummaryPage } from './pages/training/TrainingSummaryPage';
import { TrainingCertificateTemplatePage } from './pages/training/TrainingCertificateTemplatePage';
import { TrainingInformationPage } from './pages/training/TrainingInformationPage';
import { TrainingStudentPage } from './pages/training/TrainingStudentPage';
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
import { MakeExamPlanPage } from './pages/exam/MakeExamPlanPage';
import { KnowledgeRatePage } from './pages/exam/KnowledgeRatePage';

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
import { TaskEndPage } from './pages/expert/TaskEndPage';
import { ProjectManagementPage } from './pages/certification/ProjectManagementPage';
import { PreparationPage } from './pages/certification/PreparationPage';
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
  exam_work_type: <WorkTypePage />,
  dev_task_detail: <DevTaskDetailPage />,
  // 题库审核指派(管理端 3 子菜单,挂在开发任务下)
  task_exam_audit_assign: <TaskExamAuditAssignListPage />,
  task_examshicao_audit_assign: <TaskExamshicaoAuditAssignListPage />,
  task_examsub_audit_assign: <TaskExamsubAuditAssignListPage />,
  exam_course: <CoursePage />,
  'train_info-list': <TrainingInfoPage />,
  'train_people-list': <TrainingClassPeoplePage />,
  train_people: <TrainingClassPeoplePage />,
  train_declares: <TrainingClassDeclarePage />,
  exam_user_auth_course: <UserAuthCoursePage />,
  chazheng_user_list: <UserListPage />,
  chazheng_cert_query: <CertQueryPage />,
  train_needs: <RequirementsManagementPage />,
  'train_demand-train': <EnterpriseTrainingDemandPage />,
  company_demand_train: <EnterpriseTrainingDemandPage />,
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
  knowledge_spaces: <KnowledgePage />,
  knowledge_search: <KnowledgePage />,
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
  zu_juan_ji_hua: <MakeExamPlanPage />,
  exam_knowledge_rate: <KnowledgeRatePage />,
  expert_task_start: <TaskStartPage />,
  expert_task_end: <TaskEndPage />,
  certification_project: <ProjectManagementPage />,
  certification_preparation: <PreparationPage />,
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
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          <AppShell pageMap={pageMap} />
        </AuthGate>
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>,
);

