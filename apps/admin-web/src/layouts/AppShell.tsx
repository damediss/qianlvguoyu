import {
  ApiOutlined,
  AuditOutlined,
  BookOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  ReadOutlined,
  RobotOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  SearchOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Input, Layout, Menu, Space, Typography } from 'antd';
import { useState } from 'react';
import type { ReactNode } from 'react';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: '管理概览' },
  { key: 'system', icon: <SettingOutlined />, label: '系统管理' },
  {
    key: 'question-bank',
    icon: <BookOutlined />,
    label: '题库管理',
    children: [
      { key: 'exam-work-type', icon: <ExperimentOutlined />, label: '工种管理' },
      { key: 'exam-course', icon: <ReadOutlined />, label: '科目管理' },
      { key: 'exam-user-auth-course', icon: <SafetyOutlined />, label: '专家授权管理' },
    ],
  },
  { key: 'training', icon: <ScheduleOutlined />, label: '培训考试' },
  { key: 'workflow', icon: <BranchesOutlined />, label: '流程审批' },
  { key: 'ai', icon: <RobotOutlined />, label: 'AI 样板' },
  { key: 'knowledge', icon: <DatabaseOutlined />, label: '知识库' },
  { key: 'integration', icon: <ApiOutlined />, label: 'MCP 工具' },
  { key: 'audit', icon: <AuditOutlined />, label: '审计留痕' },
];

interface AppShellProps {
  pageMap: Record<string, ReactNode>;
}

export function AppShell({ pageMap }: AppShellProps) {
  const [selectedKey, setSelectedKey] = useState<string[]>(['knowledge']);

  return (
    <Layout className="app-shell">
      <Sider width={232} className="app-sider">
        <div className="brand-block">
          <div className="brand-mark">国</div>
          <div>
            <Typography.Text className="brand-name">千铅国旺云平台</Typography.Text>
            <Typography.Text className="brand-sub">培训学习考试闭环</Typography.Text>
          </div>
        </div>
        <Menu
          className="app-menu"
          theme="light"
          mode="inline"
          selectedKeys={selectedKey}
          defaultOpenKeys={['question-bank']}
          onClick={({ key }) => setSelectedKey([key])}
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
        <Content className="app-content">
          {pageMap[selectedKey[0]] || pageMap['knowledge']}
        </Content>
      </Layout>
    </Layout>
  );
}