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
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
import { TrainingClassDeclarePage } from './pages/training/TrainingClassDeclarePage';
import { UserAuthCoursePage } from './pages/exam/UserAuthCoursePage';
import { UserListPage } from './pages/chazheng/UserListPage';
import { CertQueryPage } from './pages/chazheng/CertQueryPage';
import { RequirementsManagementPage } from './pages/training/RequirementsManagementPage';
import { TrainingProgramPage } from './pages/training/TrainingProgramPage';
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
  exam_course: <CoursePage />,
  'train_info-list': <TrainingInfoPage />,
  'train_people-list': <TrainingClassPeoplePage />,
  train_declares: <TrainingClassDeclarePage />,
  exam_user_auth_course: <UserAuthCoursePage />,
  chazheng_user_list: <UserListPage />,
  chazheng_cert_query: <CertQueryPage />,
  train_needs: <RequirementsManagementPage />,
  train_solutions: <TrainingProgramPage />,
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

