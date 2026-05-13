import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppShell } from './layouts/AppShell';
import { AuthGate } from './components/AuthGate';
import { KnowledgePage } from './pages/knowledge/KnowledgePage';
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
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#0f766e',
          colorInfo: '#0f766e',
          colorText: '#172033',
          colorTextSecondary: '#667085',
          colorBorder: '#d9e1e8',
          borderRadius: 6,
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
        },
        components: {
          Button: { controlHeight: 34, borderRadius: 6 },
          Input: { controlHeight: 34, borderRadius: 6 },
          Select: { controlHeight: 34, borderRadius: 6 },
          Table: { headerBg: '#f7f9fb', headerColor: '#344054', rowHoverBg: '#f4fbfa' },
        },
      }}
    >
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
