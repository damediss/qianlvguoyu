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
import { DataPanelPage } from './pages/DataPanelPage';
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

