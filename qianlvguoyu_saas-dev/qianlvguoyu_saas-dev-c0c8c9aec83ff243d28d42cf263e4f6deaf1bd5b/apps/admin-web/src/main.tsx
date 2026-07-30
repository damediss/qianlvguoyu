import React from 'react';
import ReactDOM from 'react-dom/client';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { MessageBridge } from './components/MessageBridge';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
import { WorkTypePage } from './pages/exam/WorkTypePage';
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
import { TrainingClassDeclarePage } from './pages/training/TrainingClassDeclarePage';
import { UserAuthCoursePage } from './pages/exam/UserAuthCoursePage';
import { UserListPage } from './pages/chazheng/UserListPage';
import { CertQueryPage } from './pages/chazheng/CertQueryPage';
import { RequirementsManagementPage } from './pages/training/RequirementsManagementPage';
import { TrainingProgramPage } from './pages/training/TrainingProgramPage';
import { CollegePage } from './pages/industrial/CollegePage';
import { PlatformPage } from './pages/industrial/PlatformPage';
import { ProjectResourcePage } from './pages/industrial/ProjectResourcePage';
import { TalentTrainingPage } from './pages/industrial/TalentTrainingPage';
import { TalentServicePage } from './pages/industrial/TalentServicePage';
import { ProjectManagementPage } from './pages/certification/ProjectManagementPage';
import { PreparationPage } from './pages/certification/PreparationPage';
import { ArchivePage } from './pages/certification/ArchivePage';
import { TaskArchivePage } from './pages/certification/TaskArchivePage';
import { AssessorInfoPage } from './pages/certification/info/AssessorInfoPage';
import { ExamSiteInfoPage } from './pages/certification/info/ExamSiteInfoPage';
import { ExamEquipmentInfoPage } from './pages/certification/info/ExamEquipmentInfoPage';
import { ExamStaffInfoPage } from './pages/certification/info/ExamStaffInfoPage';
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
  'exam-work-type': <WorkTypePage />,
  'training-info': <TrainingInfoPage />,
  'training-class-people': <TrainingClassPeoplePage />,
  'training-class-declare': <TrainingClassDeclarePage />,
  'exam-user-auth-course': <UserAuthCoursePage />,
  'chazheng-user-list': <UserListPage />,
  'chazheng-cert-query': <CertQueryPage />,
  'requirements-management': <RequirementsManagementPage />,
  'training-program': <TrainingProgramPage />,
  'industrial-college': <CollegePage />,
  'industrial-platform-master': <PlatformPage category="master" />,
  'industrial-platform-study': <PlatformPage category="study" />,
  'industrial-platform-match': <PlatformPage category="match" />,
  'industrial-platform-apprentice': <PlatformPage category="apprentice" />,
  'industrial-platform-comment': <PlatformPage category="comment" />,
  'industrial-project-course': <ProjectResourcePage type="course" />,
  'industrial-project-teach': <ProjectResourcePage type="teach" />,
  'industrial-project-exam': <ProjectResourcePage type="exam" />,
  'industrial-project-train': <ProjectResourcePage type="train" />,
  'industrial-project-criterion': <ProjectResourcePage type="criterion" />,
  'industrial-talent-training': <TalentTrainingPage />,
  'industrial-talent-service': <TalentServicePage />,
  'certification-project': <ProjectManagementPage />,
  'certification-preparation': <PreparationPage />,
  'certification-archive': <ArchivePage />,
  'certification-task-archive': <TaskArchivePage />,
  'certification-info-assessor': <AssessorInfoPage />,
  'certification-info-site': <ExamSiteInfoPage />,
  'certification-info-equipment': <ExamEquipmentInfoPage />,
  'certification-info-exam-staff': <ExamStaffInfoPage />,
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <AntdApp>
        <MessageBridge />
        <QueryClientProvider client={queryClient}>
          <AuthGate>
            <AppShell pageMap={pageMap} />
          </AuthGate>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>,
);
