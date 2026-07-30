import {
  ApiOutlined,
  ApartmentOutlined,
  AuditOutlined,
  BankOutlined,
  BookOutlined,
  BranchesOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  FilePptOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  IdcardOutlined,
  ProjectOutlined,
  ReadOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  SearchOutlined,
  SettingOutlined,
  SolutionOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Input, Layout, Menu, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useAuthStore } from '../stores/authStore';
import type { MenuVO } from '../types/api';

const { Header, Sider, Content } = Layout;

const fallbackMenuItems = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: '管理概览' },
  { key: 'system', icon: <SettingOutlined />, label: '系统管理' },
  {
    key: 'question-bank',
    icon: <BookOutlined />,
    label: '题库管理',
    children: [
      { key: 'exam-work-type', icon: <ExperimentOutlined />, label: '工种管理' },
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
  {
    key: 'certification',
    icon: <SafetyCertificateOutlined />,
    label: '认定管理',
    children: [
      { key: 'certification-project', icon: <ProjectOutlined />, label: '任务管理' },
      {
        key: 'certification-info',
        icon: <DatabaseOutlined />,
        label: '信息管理',
        children: [
          { key: 'certification-info-assessor', icon: <IdcardOutlined />, label: '考评员信息' },
          { key: 'certification-info-site', icon: <BankOutlined />, label: '考场信息' },
          { key: 'certification-info-equipment', icon: <DatabaseOutlined />, label: '考试设备' },
          { key: 'certification-info-exam-staff', icon: <TeamOutlined />, label: '考务信息' },
        ],
      },
      { key: 'certification-preparation', icon: <ScheduleOutlined />, label: '认定筹备' },
      { key: 'certification-archive', icon: <FolderOpenOutlined />, label: '认定归档' },
      { key: 'certification-task-archive', icon: <FileTextOutlined />, label: '任务档案' },
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
  const backendMenus = useAuthStore((state) => state.menus);
  const menuItems = useMemo(
    () => (backendMenus.length > 0 ? filterBackendMenus(backendMenus).map(toMenuItem) : fallbackMenuItems),
    [backendMenus],
  );

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
          defaultOpenKeys={['question-bank', 'training', 'certification', 'certification-info']}
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

function filterBackendMenus(menus: MenuVO[]): MenuVO[] {
  return menus
    .filter((menu) => !isHiddenMenu(menu))
    .map((menu) => ({
      ...menu,
      children: menu.children?.length ? filterBackendMenus(menu.children) : menu.children,
    }));
}

function isHiddenMenu(menu: MenuVO) {
  const key = menuKey(menu);
  return key === 'industrial'
    || menu.code === 'industrial'
    || key === 'certification-pre-exam'
    || menu.code === 'certification_pre_exam';
}

function toMenuItem(menu: MenuVO): NonNullable<MenuProps['items']>[number] {
  return {
    key: menuKey(menu),
    icon: iconOf(menu.icon),
    label: menu.name,
    children: menu.children?.length ? menu.children.map(toMenuItem) : undefined,
  };
}

function menuKey(menu: MenuVO) {
  return (menu.code || menu.component || String(menu.id)).replaceAll('_', '-');
}

function iconOf(icon?: string) {
  const icons: Record<string, ReactNode> = {
    ApiOutlined: <ApiOutlined />,
    ApartmentOutlined: <ApartmentOutlined />,
    AuditOutlined: <AuditOutlined />,
    BankOutlined: <BankOutlined />,
    BookOutlined: <BookOutlined />,
    BranchesOutlined: <BranchesOutlined />,
    ClusterOutlined: <ClusterOutlined />,
    DatabaseOutlined: <DatabaseOutlined />,
    DashboardOutlined: <DashboardOutlined />,
    ExperimentOutlined: <ExperimentOutlined />,
    FileTextOutlined: <FileTextOutlined />,
    FilePptOutlined: <FilePptOutlined />,
    FolderOpenOutlined: <FolderOpenOutlined />,
    IdcardOutlined: <IdcardOutlined />,
    ProjectOutlined: <ProjectOutlined />,
    ReadOutlined: <ReadOutlined />,
    RobotOutlined: <RobotOutlined />,
    SafetyCertificateOutlined: <SafetyCertificateOutlined />,
    SafetyOutlined: <SafetyOutlined />,
    ScheduleOutlined: <ScheduleOutlined />,
    SearchOutlined: <SearchOutlined />,
    SettingOutlined: <SettingOutlined />,
    SolutionOutlined: <SolutionOutlined />,
    TeamOutlined: <TeamOutlined />,
    TrophyOutlined: <TrophyOutlined />,
    UserOutlined: <UserOutlined />,
  };
  return icon ? icons[icon] || <DatabaseOutlined /> : <DatabaseOutlined />;
}
