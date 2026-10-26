/**
 * 侧栏目录默认折叠（竞赛管理）：
 * 进入系统时的标签恢复/深链不应把 竞赛管理 撑开；只有用户从侧栏点过菜单后，
 * 切换菜单才自动展开选中页祖先。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, cleanup, render, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppShell, resetTabsSessionRestore } from '../src/layouts/AppShell';
import { useAuthStore } from '../src/stores/authStore';
import { useNavStore } from '../src/stores/navStore';
import { useTabStore } from '../src/stores/tabStore';
import type { MenuVO } from '../src/types/api';

const api = vi.hoisted(() => ({ fetchMenus: vi.fn(), logout: vi.fn() }));
vi.mock('../src/api/auth', () => ({ fetchMenus: api.fetchMenus, logout: api.logout }));

function menu(
  code: string,
  name: string,
  path: string,
  menuType = 'menu',
  children: MenuVO[] = [],
  expanded = 0,
): MenuVO {
  return {
    id: code.length * 100 + path.length,
    parentId: 0,
    menuType,
    code,
    name,
    path,
    component: '',
    permissionCode: '',
    icon: '',
    sortOrder: 1,
    expanded,
    children,
  };
}

const TREE: MenuVO[] = [
  menu('knowledge', '管理概览', '/knowledge'),
  menu('competition_foundation', '竞赛管理', '/competition', 'directory', [
    menu('competition_event', '赛事创建', '/competition/events'),
    menu('competition_player', '选手管理', '/competition/player-management', 'directory', [
      menu('competition_player_info', '选手信息', '/competition/players'),
    ], 1),
  ], 0),
];

function pageMap() {
  return {
    knowledge: <div>管理概览内容</div>,
    competition_event: <div>赛事创建内容</div>,
    competition_player_info: <div>选手信息内容</div>,
  } as Record<string, React.ReactNode>;
}

function renderShell() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AppShell pageMap={pageMap()} />
    </QueryClientProvider>,
  );
}

function sider() {
  return within(document.querySelector('.app-sider') as HTMLElement);
}

async function competitionSubmenu() {
  const title = await waitFor(() => sider().findByText('竞赛管理'));
  return title.closest('li.ant-menu-submenu') as HTMLElement;
}

function storeCompetitionTab() {
  localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
    version: 1,
    tabs: [
      { key: 'knowledge', title: '管理概览', path: '/knowledge', closable: false },
      { key: 'competition_event', title: '赛事创建', path: '/competition/events', closable: true },
    ],
    activeKey: 'competition_event',
    scrollPositions: {},
  }));
}

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  window.history.replaceState({}, '', '/knowledge');
  resetTabsSessionRestore();
  useAuthStore.setState({
    token: 'test-token',
    tenantId: 10159,
    currentUser: { displayName: '测试管理员', permissions: [], roles: [] } as never,
    permissionCodes: [],
    menuTree: [],
  });
  useTabStore.setState({
    tabs: [{ key: 'knowledge', title: '管理概览', path: '/knowledge', closable: false }],
    activeKey: 'knowledge',
    visitedKeys: ['knowledge'],
    scrollPositions: {},
    refreshCounters: {},
    softPromptVisible: false,
  });
  useNavStore.setState({ selectedKey: 'knowledge' });
  api.fetchMenus.mockResolvedValue(TREE);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('竞赛管理默认折叠', () => {
  it('恢复上次的竞赛页标签进入时，竞赛管理保持折叠', async () => {
    window.history.replaceState({}, '', '/');
    storeCompetitionTab();
    renderShell();
    const li = await competitionSubmenu();
    // 等待标签恢复完成（activeKey 回到上次的竞赛页）
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('competition_event');
    });
    expect(li.classList.contains('ant-menu-submenu-open')).toBe(false);
  });

  it('进入时由竞赛工作区（非侧栏点击）打开竞赛标签，竞赛管理仍保持折叠', async () => {
    renderShell();
    const li = await competitionSubmenu();
    // 模拟 main.tsx 竞赛覆盖态在进入时打开标签：不走侧栏点击链路
    await act(async () => {
      useTabStore.getState().openTab('competition_event');
    });
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('competition_event');
    });
    expect(li.classList.contains('ant-menu-submenu-open')).toBe(false);
  });

  it('展开竞赛管理后，其子目录仍默认折叠，点击后才展开', async () => {
    storeCompetitionTab();
    const user = userEvent.setup();
    renderShell();
    const li = await competitionSubmenu();
    await user.click(await waitFor(() => sider().findByText('竞赛管理')));
    await waitFor(() => {
      expect(li.classList.contains('ant-menu-submenu-open')).toBe(true);
    });
    // 后端 expanded=1 的子目录不随父目录自动展开
    const playerTitle = await waitFor(() => sider().findByText('选手管理'));
    const playerLi = playerTitle.closest('li.ant-menu-submenu') as HTMLElement;
    expect(playerLi.classList.contains('ant-menu-submenu-open')).toBe(false);
    await user.click(playerTitle);
    await waitFor(() => {
      expect(playerLi.classList.contains('ant-menu-submenu-open')).toBe(true);
    });
  });
});
