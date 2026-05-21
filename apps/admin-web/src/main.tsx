import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
import { WorkTypePage } from './pages/exam/WorkTypePage';
import { TrainingSolutionsPage } from './pages/training/TrainingSolutionsPage';
import { TrainingInfoPage } from './pages/training/TrainingInfoPage';
import { TrainingClassPeoplePage } from './pages/training/TrainingClassPeoplePage';
import { TrainingClassDeclarePage } from './pages/training/TrainingClassDeclarePage';
import { UserAuthCoursePage } from './pages/exam/UserAuthCoursePage';
import { UserListPage } from './pages/chazheng/UserListPage';
import { CertQueryPage } from './pages/chazheng/CertQueryPage';
import { RequirementsManagementPage } from './pages/training/RequirementsManagementPage';
import { TrainingProgramPage } from './pages/training/TrainingProgramPage';
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
  'training-needs': <TrainingNeedsPage />,
  'training-solutions': <TrainingSolutionsPage />,
  'training-info': <TrainingInfoPage />,
  'training-class-people': <TrainingClassPeoplePage />,
  'training-class-declare': <TrainingClassDeclarePage />,
  'exam-user-auth-course': <UserAuthCoursePage />,
  'chazheng-user-list': <UserListPage />,
  'chazheng-cert-query': <CertQueryPage />,
  'requirements-management': <RequirementsManagementPage />,
  'training-program': <TrainingProgramPage />,
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

