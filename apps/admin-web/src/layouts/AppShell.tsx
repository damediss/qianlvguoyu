import * as Icons from '@ant-design/icons';
import { Avatar, Breadcrumb, Dropdown, Input, Layout, Menu, Space, Tooltip, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchMenus, logout } from '../api/auth';
import { apiClient } from '../api/client';
import { useAuthStore } from '../stores/authStore';
import { useNavStore } from '../stores/navStore';
import type { MenuVO } from '../types/api';
import { iconFromName } from '../utils/icons';

const { Header, Sider, Content } = Layout;

type AppMenuItem = {
  key: string;
  icon?: ReactNode;
  label: ReactNode;
  title: string;
  disabled?: boolean;
  children?: AppMenuItem[];
};

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
  pageMap: Record<string, ReactNode>,
): AppMenuItem[] {
  return tree
    .map((m) => {
      const hasChildren = Boolean(m.children && m.children.length > 0);
      const isEmptyDirectory = m.menuType === 'directory' && !hasChildren && !pageMap[m.code];
      const item: AppMenuItem = {
        key: m.code,
        icon: iconFromName(m.icon),
        label: <TruncatedLabel text={m.name} />,
        title: m.name,
        disabled: isEmptyDirectory,
      };
      if (hasChildren) {
        // inline 模式下点击目录本体即可展开/收起，无需再绑 onTitleClick 导航
        item.children = buildMenuItems(m.children, pageMap);
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

function findMenuPath(tree: MenuVO[], targetCode: string, trail: MenuVO[] = []): MenuVO[] | null {
  for (const node of tree) {
    const nextTrail = [...trail, node];
    if (node.code === targetCode) {
      return nextTrail;
    }
    if (node.children?.length) {
      const found = findMenuPath(node.children, targetCode, nextTrail);
      if (found) {
        return found;
      }
    }
  }
  return null;
}

function findFirstNavigableNode(tree: MenuVO[], pageMap: Record<string, ReactNode>): MenuVO | null {
  for (const node of tree) {
    if (node.menuType === 'external' || pageMap[node.code]) {
      return node;
    }
    if (node.children?.length) {
      const found = findFirstNavigableNode(node.children, pageMap);
      if (found) {
        return found;
      }
    }
  }
  return null;
}

interface AppShellProps {
  pageMap: Record<string, ReactNode>;
  contentOverride?: ReactNode;
  selectedMenuKeyOverride?: string;
  onMenuNavigate?: () => void;
  hideBreadcrumb?: boolean;
}

export function AppShell({
  pageMap,
  contentOverride,
  selectedMenuKeyOverride,
  onMenuNavigate,
  hideBreadcrumb = false,
}: AppShellProps) {
  const token = useAuthStore((s) => s.token);
  const menuTree = useAuthStore((s) => s.menuTree);
  const setMenuTree = useAuthStore((s) => s.setMenuTree);
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const currentUser = useAuthStore((s) => s.currentUser);
  const selectedKey = useNavStore((s) => s.selectedKey);
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const [openKeys, setOpenKeys] = useState<string[]>([]);
  const activeMenuKey = selectedMenuKeyOverride || selectedKey;
  const prevActiveMenuKeyRef = useRef<string | null>(null);
  const appliedDefaultExpandedRef = useRef(false);

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

  // 进入系统时不按当前选中页强制展开祖先（认定管理等保持折叠）；
  // 仅在用户切换菜单后，才自动展开选中页的祖先路径。
  useEffect(() => {
    if (!menuTree.length || !activeMenuKey) return;
    const prevKey = prevActiveMenuKeyRef.current;
    prevActiveMenuKeyRef.current = activeMenuKey;
    if (prevKey === null || prevKey === activeMenuKey) {
      return;
    }
    const ancestors = findAncestorCodes(menuTree, activeMenuKey);
    if (ancestors && ancestors.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...ancestors])]);
    }
  }, [activeMenuKey, menuTree]);

  // 根据菜单配置的 expanded 字段初始化默认展开的目录（仅首次）
  useEffect(() => {
    if (!menuTree.length || appliedDefaultExpandedRef.current) return;
    const collectExpanded = (nodes: MenuVO[]): string[] => {
      return nodes.reduce((keys: string[], node) => {
        if (node.menuType === 'directory' && node.expanded === 1) keys.push(node.code);
        if (node.children?.length) keys.push(...collectExpanded(node.children));
        return keys;
      }, []);
    };
    const expandedKeys = collectExpanded(menuTree);
    appliedDefaultExpandedRef.current = true;
    if (expandedKeys.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...expandedKeys])]);
    }
  }, [menuTree]);

  useEffect(() => {
    if (!menuTree.length || !selectedKey) return;
    const node = findMenuNode(menuTree, selectedKey);
    if (!node || pageMap[node.code] || node.menuType === 'external') return;
    const target = findFirstNavigableNode(node.children || [], pageMap);
    if (target && target.code !== selectedKey && target.menuType !== 'external') {
      setSelectedKey(target.code);
    }
  }, [menuTree, pageMap, selectedKey, setSelectedKey]);

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

  const openExternalMenu = useCallback((node: MenuVO) => {
    if (node.menuType !== 'external') {
      return;
    }
    apiClient.get<{ authorizeUrl: string }>('/sso/weknora')
      .then(r => {
        window.location.href = r.data.authorizeUrl;
      })
      .catch(() => {
        window.open('http://43.138.163.7:8088', '_blank');
      });
  }, []);

  const navigateToNode = useCallback((node: MenuVO | null) => {
    if (!node) {
      return;
    }
    const target = pageMap[node.code] || node.menuType === 'external'
      ? node
      : findFirstNavigableNode(node.children || [], pageMap);
    if (!target) {
      return;
    }
    if (target.menuType === 'external') {
      openExternalMenu(target);
      return;
    }
    if (pageMap[target.code]) {
      setSelectedKey(target.code);
    }
  }, [openExternalMenu, pageMap, setSelectedKey]);

  const handleMenuNavigate = useCallback((node: MenuVO | null) => {
    onMenuNavigate?.();
    navigateToNode(node);
  }, [navigateToNode, onMenuNavigate]);

  const menuItems = useMemo(
    () => buildMenuItems(menuTree, pageMap),
    [menuTree, pageMap],
  );

  const breadcrumbItems = useMemo(() => {
    const path = findMenuPath(menuTree, activeMenuKey) || [];
    return path.map((node) => ({ title: node.name }));
  }, [activeMenuKey, menuTree]);

  return (
    <Layout className="app-shell">
      <Sider width={270} className="app-sider">
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
          selectedKeys={[activeMenuKey]}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          onClick={({ key }) => {
            handleMenuNavigate(findMenuNode(menuTree, String(key)));
          }}
          items={menuItems as MenuProps['items']}
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
          {!hideBreadcrumb && breadcrumbItems.length > 0 && (
            <Breadcrumb className="app-breadcrumb" items={breadcrumbItems} />
          )}
          <div className="app-page-body">
            {contentOverride || pageMap[selectedKey] || pageMap['knowledge']}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
