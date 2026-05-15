import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
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

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          <AppShell>
            <KnowledgePage />
          </AppShell>
        </AuthGate>
      </QueryClientProvider>
    </ConfigProvider>
  </React.StrictMode>,
);

