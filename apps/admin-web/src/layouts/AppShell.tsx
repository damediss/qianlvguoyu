import {
  ApiOutlined,
  AuditOutlined,
  BookOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  RobotOutlined,
  ScheduleOutlined,
  SearchOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Input, Layout, Menu, Space, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Header, Sider, Content } = Layout;

interface AppShellProps {
  children: ReactNode;
}

const menuItems = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: '管理概览' },
  { key: 'system', icon: <SettingOutlined />, label: '系统管理' },
  { key: 'course', icon: <BookOutlined />, label: '课程题库' },
  { key: 'training', icon: <ScheduleOutlined />, label: '培训考试' },
  { key: 'workflow', icon: <BranchesOutlined />, label: '流程审批' },
  { key: 'ai', icon: <RobotOutlined />, label: 'AI 样板' },
  { key: 'knowledge', icon: <DatabaseOutlined />, label: '知识库' },
  { key: 'integration', icon: <ApiOutlined />, label: 'MCP 工具' },
  { key: 'audit', icon: <AuditOutlined />, label: '审计留痕' },
];

export function AppShell({ children }: AppShellProps) {
  return (
    <Layout className="app-shell">
      <Sider width={232} className="app-sider">
        <div className="brand-block">
          <div className="brand-mark">国</div>
          <div>
            <Typography.Text className="brand-name">千铝国昱云平台</Typography.Text>
            <Typography.Text className="brand-sub">培训学习考试闭环</Typography.Text>
          </div>
        </div>
        <Menu
          className="app-menu"
          theme="light"
          mode="inline"
          selectedKeys={['knowledge']}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Input
            className="top-search"
            prefix={<SearchOutlined />}
            placeholder="搜索课程、资料、任务"
            variant="filled"
          />
          <Space size={12}>
            <Button>操作日志</Button>
            <Avatar icon={<UserOutlined />} />
            <div className="user-meta">
              <span>平台管理员</span>
              <small>demo tenant</small>
            </div>
          </Space>
        </Header>
        <Content className="app-content">{children}</Content>
      </Layout>
    </Layout>
  );
}
