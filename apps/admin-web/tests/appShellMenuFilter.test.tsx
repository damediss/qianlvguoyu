/**
 * 下线菜单入口的侧栏过滤：命中 hiddenMenuCodes 的入口不渲染，
 * 子入口全部下线的目录（如“组卷计划”）也不留空壳。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppShell, resetTabsSessionRestore } from '../src/layouts/AppShell';
import { useAuthStore } from '../src/stores/authStore';
import { useNavStore } from '../src/stores/navStore';
import { useTabStore } from '../src/stores/tabStore';
import type { MenuVO } from '../src/types/api';

const api = vi.hoisted(() => ({
  fetchMenus: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../src/api/auth', () => ({
  fetchMenus: api.fetchMenus,
  logout: api.logout,
}));

function menu(
  code: string,
  name: string,
  path: string,
  menuType = 'menu',
  children: MenuVO[] = [],
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
    children,
  };
}

const TREE: MenuVO[] = [
  menu('competition_foundation', '竞赛管理', '/competition', 'directory', [
    menu('competition_event', '赛事创建', '/competition/events'),
    menu('competition_practical_exam', '实操比赛控制台', '/competition/practical-exam'),
    menu('competition_question_bank', '竞赛试题管理', '/competition/question-bank', 'directory', [
      menu('competition_theory_blueprint', '理论细目表', '/competition/question-bank/theory-blueprint'),
      menu('competition_theory_question', '理论试题', '/competition/question-bank/theory'),
      menu('competition_practical_blueprint', '实操细目表', '/competition/question-bank/practical-blueprint'),
      menu('competition_practical_question', '实操试题', '/competition/question-bank/practical'),
      menu('competition_simple_question', '简易试题', '/competition/question-bank/simple', 'directory', [
        menu('competition_simple_theory_question', '简易理论试题', '/competition/question-bank/simple/theory'),
      ]),
    ]),
    menu('competition_make_exam_plan', '组卷计划', '/competition/make-exam-plans', 'directory', [
      menu('competition_theory_make_exam_plan', '理论组卷', '/competition/make-exam-plans'),
      menu('competition_practical_make_exam_plan', '实操组卷', '/competition/make-exam-plans/practical'),
    ]),
  ]),
];

function pageMap() {
  return {
    competition_event: <div>赛事创建内容</div>,
    competition_simple_theory_question: <div>简易理论试题内容</div>,
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

/** 侧栏菜单容器（避免命中面包屑等处的同名文字） */
function sider() {
  return within(document.querySelector('.app-sider') as HTMLElement);
}

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  window.history.replaceState({}, '', '/');
  resetTabsSessionRestore();
  useAuthStore.setState({
    token: 'test-token',
    tenantId: 10159,
    currentUser: { displayName: '测试管理员', permissions: [], roles: [] } as never,
    permissionCodes: [],
    menuTree: [],
  });
  useTabStore.setState({
    tabs: [{ key: 'competition_event', title: '赛事创建', path: '/competition/events', closable: false }],
    activeKey: 'competition_event',
    visitedKeys: ['competition_event'],
    scrollPositions: {},
    refreshCounters: {},
    softPromptVisible: false,
  });
  useNavStore.setState({ selectedKey: 'competition_event' });
  api.fetchMenus.mockResolvedValue(TREE);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('下线菜单入口', () => {
  it('试题/细目表入口不渲染，未下线的同级入口保持可见', async () => {
    const user = userEvent.setup();
    renderShell();
    // 展开“竞赛管理”“竞赛试题管理”目录，让子项进入 DOM
    await user.click(await screen.findByText('竞赛管理'));
    await user.click(await screen.findByText('竞赛试题管理'));

    expect(sider().queryByText('理论细目表')).toBeNull();
    expect(sider().queryByText('实操细目表')).toBeNull();
    expect(sider().queryByText('理论试题')).toBeNull();
    expect(sider().queryByText('实操试题')).toBeNull();
    // 未在本次下线范围内的简易入口仍在
    expect(sider().getByText('简易试题')).toBeTruthy();
  });

  it('子入口全部下线的目录（组卷计划）不保留空壳', async () => {
    const user = userEvent.setup();
    renderShell();
    await user.click(await screen.findByText('竞赛管理'));
    await waitFor(() => {
      expect(sider().queryByText('组卷计划')).toBeNull();
    });
    expect(sider().queryByText('理论组卷')).toBeNull();
    expect(sider().queryByText('实操组卷')).toBeNull();
  });

  it('赛事创建等未下线入口正常展示，实操比赛控制台入口隐藏', async () => {
    const user = userEvent.setup();
    renderShell();
    await user.click(await screen.findByText('竞赛管理'));
    expect(await screen.findByText('赛事创建')).toBeTruthy();
    expect(sider().queryByText('实操比赛控制台')).toBeNull();
  });
});
