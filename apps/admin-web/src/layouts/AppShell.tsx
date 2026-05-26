import {
  ApiOutlined,
  AuditOutlined,
  BookOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  IdcardOutlined,
  FileTextOutlined,
  ProjectOutlined,
  ReadOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  SearchOutlined,
  SettingOutlined,
  SolutionOutlined,
  TableOutlined,
  TeamOutlined,
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
      {
        key: 'task-management',
        icon: <FileSearchOutlined />,
        label: '任务管理',
        children: [
          {
            key: 'dev-task',
            icon: <ProjectOutlined />,
            label: '开发任务',
            children: [
              { key: 'dev-task-detail', icon: <TableOutlined />, label: '细目表' },
            ],
          },
        ],
      },
      { key: 'exam-work-type', icon: <ExperimentOutlined />, label: '工种管理' },
      { key: 'exam-course', icon: <ReadOutlined />, label: '科目管理' },
      { key: 'exam-user-auth-course', icon: <SafetyOutlined />, label: '专家授权管理' },
    ],
  },
  {
    key: 'training',
    icon: <ScheduleOutlined />,
    label: '培训管理',
    children: [
      { key: 'requirements-management', icon: <FileTextOutlined />, label: '需求管理' },
      { key: 'training-program', icon: <SolutionOutlined />, label: '培训方案' },
      { key: 'training-info', icon: <BookOutlined />, label: '培训信息' },
      { key: 'training-class-people', icon: <TeamOutlined />, label: '班级人员' },
      { key: 'training-class-declare', icon: <AuditOutlined />, label: '开班申报' },
    ],
  },
  { key: 'training-exam', icon: <ScheduleOutlined />, label: '培训考试' },
  { key: 'workflow', icon: <BranchesOutlined />, label: '流程审批' },
  { key: 'ai', icon: <RobotOutlined />, label: 'AI 样板' },
  { key: 'knowledge', icon: <DatabaseOutlined />, label: '知识库' },
  { key: 'integration', icon: <ApiOutlined />, label: 'MCP 工具' },
  { key: 'audit', icon: <AuditOutlined />, label: '审计留痕' },
  {
    key: 'chazheng',
    icon: <SafetyCertificateOutlined />,
    label: '查证管理',
    children: [
      { key: 'chazheng-user-list', icon: <SearchOutlined />, label: '用户列表' },
      { key: "chazheng-cert-query", icon: <IdcardOutlined />, label: "证书查询" },
    ],
  },
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
            <Typography.Text className="brand-name">千铝国昱云平台</Typography.Text>
            <Typography.Text className="brand-sub">培训学习考试闭环</Typography.Text>
          </div>
        </div>
        <Menu
          className="app-menu"
          theme="light"
          mode="inline"
          selectedKeys={selectedKey}
          defaultOpenKeys={['question-bank', 'training']}
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