import * as Icons from '@ant-design/icons';
import { Avatar, Dropdown, Input, Layout, Menu, Space, Tooltip, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchMenus, logout } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import { useNavStore } from '../stores/navStore';
import type { MenuVO } from '../types/api';
import { iconFromName } from '../utils/icons';

const { Header, Sider, Content } = Layout;

/**
 * Render a menu label and only attach the hover Tooltip when the text is
 * actually truncated (i.e. the inner span would show an ellipsis). Untruncated
 * labels render as plain text so we don't display the full name twice.
 */
function TruncatedLabel({ text }: { text: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const [truncated, setTruncated] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const update = () => setTruncated(el.scrollWidth > el.clientWidth);
    update();
    // Re-check when the sidebar is resized or fonts load.
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, [text]);

  const label = (
    <span ref={ref} className="app-menu-label">
      {text}
    </span>
  );

  if (!truncated) {
    return label;
  }

  return (
    <Tooltip title={text} placement="right" mouseEnterDelay={0.2}>
      {label}
    </Tooltip>
  );
}

/**
 * Recursively build Ant Design Menu items from the backend menu tree.
 *
 * The label is rendered through `TruncatedLabel` so the hover Tooltip only
 * appears for menu names that overflow into an ellipsis.
 */
function buildMenuItems(
  tree: MenuVO[],
): { key: string; icon?: ReactNode; label: ReactNode; title: string; children?: unknown[] }[] {
  return tree
    .map((m) => {
      const item: {
        key: string;
        icon?: ReactNode;
        label: ReactNode;
        title: string;
        children?: unknown[];
      } = {
        key: m.code,
        icon: iconFromName(m.icon),
        label: <TruncatedLabel text={m.name} />,
        title: m.name,
      };
      if (m.children && m.children.length > 0) {
        item.children = buildMenuItems(m.children);
      }
      return item;
    });
}

function findMenuNode(tree: MenuVO[], code: string): MenuVO | null {
  for (const node of tree) {
    if (node.code === code) return node;
    if (node.children?.length) {
      const found = findMenuNode(node.children, code);
      if (found) return found;
    }
  }
  return null;
}

function findAncestorCodes(tree: MenuVO[], targetCode: string, trail: string[] = []): string[] | null {
  for (const node of tree) {
    if (node.code === targetCode) {
      return trail;
    }
    if (node.children?.length) {
      const found = findAncestorCodes(node.children, targetCode, [...trail, node.code]);
      if (found !== null) {
        return found;
      }
    }
  }
  return null;
}

interface AppShellProps {
  pageMap: Record<string, ReactNode>;
}

export function AppShell({ pageMap }: AppShellProps) {
  const token = useAuthStore((s) => s.token);
  const menuTree = useAuthStore((s) => s.menuTree);
  const setMenuTree = useAuthStore((s) => s.setMenuTree);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const currentUser = useAuthStore((s) => s.currentUser);
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

  useEffect(() => {
    if (!menuTree.length) return;
    const autoOpenCodes = ['company_demand', 'training'].filter((code) => {
      const node = findMenuNode(menuTree, code);
      return Boolean(node?.children?.length);
    });
    if (autoOpenCodes.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...autoOpenCodes])]);
    }
  }, [menuTree]);

  useEffect(() => {
    if (!menuTree.length || !selectedKey) return;
    const ancestors = findAncestorCodes(menuTree, selectedKey);
    if (ancestors && ancestors.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...ancestors])]);
    }
  }, [menuTree, selectedKey]);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 忽略后端错误，前端强制清除登录态
    } finally {
      clearAuth();
    }
  };

  const handleOpenAssessor = async () => {
    try {
      await logout();
    } catch {
      // 切换入口时仍强制清理旧登录态，避免管理账号误入平板端
    } finally {
      clearAuth();
      window.location.assign('/assessor-scoring');
    }
  };

  const userDropdownItems = [
    {
      key: 'assessor',
      icon: <Icons.TabletOutlined />,
      label: '平板考评端',
      onClick: handleOpenAssessor,
    },
    {
      key: 'logout',
      icon: <Icons.LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ];

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
          onClick={({ key }) => {
            const node = findMenuNode(menuTree, key);
            if (node?.children && node.children.length > 0) {
              return;
            }
            setSelectedKey(key);
          }}
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
            <Dropdown menu={{ items: userDropdownItems }} placement="bottomRight" trigger={['click']}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<Icons.UserOutlined />} />
                <div className="user-meta">
                  <span>{currentUser?.displayName ?? '平台管理员'}</span>
                  <small>demo tenant</small>
                </div>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content className="app-content">
          {pageMap[selectedKey] || pageMap['knowledge']}
        </Content>
      </Layout>
    </Layout>
  );
}
