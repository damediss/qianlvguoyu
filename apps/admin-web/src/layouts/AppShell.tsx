import * as Icons from '@ant-design/icons';
import { Avatar, Button, Input, Layout, Menu, Space, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchMenus } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import { useNavStore } from '../stores/navStore';
import type { MenuVO } from '../types/api';

const { Header, Sider, Content } = Layout;

/** Map icon string name (e.g. "BookOutlined") to a React component at runtime. */
function iconFromName(name?: string | null): ReactNode {
  if (!name) return null;
  const IconComp = (Icons as Record<string, React.ComponentType>)[name];
  return IconComp ? <IconComp /> : null;
}

/** Recursively build Ant Design Menu items from the backend menu tree. */
function buildMenuItems(tree: MenuVO[]): { key: string; icon?: ReactNode; label: string; children?: unknown[] }[] {
  return tree
    .map((m) => {
      const item: { key: string; icon?: ReactNode; label: string; children?: unknown[] } = {
        key: m.code,
        icon: iconFromName(m.icon),
        label: m.name,
      };
      if (m.children && m.children.length > 0) {
        item.children = buildMenuItems(m.children);
      }
      return item;
    });
}

interface AppShellProps {
  pageMap: Record<string, ReactNode>;
}

export function AppShell({ pageMap }: AppShellProps) {
  const token = useAuthStore((s) => s.token);
  const menuTree = useAuthStore((s) => s.menuTree);
  const setMenuTree = useAuthStore((s) => s.setMenuTree);
  const selectedKey = useNavStore((s) => s.selectedKey);
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  const menusQuery = useQuery({
    queryKey: ['menus', token],
    queryFn: fetchMenus,
    enabled: Boolean(token),
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (menusQuery.data) {
      setMenuTree(menusQuery.data);
    }
  }, [menusQuery.data, setMenuTree]);

  const menuItems = buildMenuItems(menuTree);

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
          selectedKeys={[selectedKey]}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          onClick={({ key }) => setSelectedKey(key)}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Input
            className="top-search"
            prefix={<Icons.SearchOutlined />}
            placeholder="搜索课程、资料、任务"
            variant="filled"
          />
          <Space size={12}>
            <Button>操作日志</Button>
            <Avatar icon={<Icons.UserOutlined />} />
            <div className="user-meta">
              <span>平台管理员</span>
              <small>demo tenant</small>
            </div>
          </Space>
        </Header>
        <Content className="app-content">
          {pageMap[selectedKey] || pageMap['knowledge']}
        </Content>
      </Layout>
    </Layout>
  );
}
