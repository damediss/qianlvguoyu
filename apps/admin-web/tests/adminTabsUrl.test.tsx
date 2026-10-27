/**
 * admin-web 标签页 AppShell 集成单测（《admin-web标签页外壳改造方案》§5.1/5.2）。
 * 覆盖：菜单点击开标签（UT/RTL-01）、深链直达（§3.4）、localStorage 恢复校验（§3.5）、
 * popstate 反查（§3.4 反向）、隐藏路由不进标签（§3.4 正向排除）、目录下钻守卫（UT-18，§3.5）、
 * >10 软提示展示（RTL-11）、灰度开关 off 走旧分支（RTL-12）。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, cleanup, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
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

function menu(code: string, name: string, path: string, menuType = 'menu', children: MenuVO[] = []): MenuVO {
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
  menu('knowledge', '管理概览', '/knowledge'),
  menu('knowledge_fix', '知识归属修正', '/question-bank/knowledge-fix'),
  menu('system_user', '用户管理', '/system/users'),
  // 覆盖态载体码（隐藏路由 shell 复用同名菜单码，见 main.tsx ReviewSyncStatsShell）
  menu('kao_shi_ji_hua', '考试计划', '/kao-shi-ji-hua'),
  // 目录 + 子页（UT-18 下钻守卫）
  menu('cert_dir', '认定管理', '/certification', 'directory', [
    menu('certification_dashboard', '认定数据面板', '/certification/dashboard'),
  ]),
];

function pageMap() {
  return {
    knowledge: <div>管理概览内容</div>,
    knowledge_fix: <div>知识归属修正内容</div>,
    system_user: <div>用户管理内容</div>,
    kao_shi_ji_hua: <div>考试计划内容</div>,
    certification_dashboard: <div>认定数据面板内容</div>,
  } as Record<string, React.ReactNode>;
}

function resetStores() {
  localStorage.clear();
  sessionStorage.clear();
  useAuthStore.setState({
    token: 'test-token',
    tenantId: 10159,
    currentUser: { displayName: '测试管理员', permissions: [], roles: [] } as never,
    permissionCodes: [],
    menuTree: TREE,
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
  window.history.replaceState({}, '', '/');
  resetTabsSessionRestore();
}

function renderShell(props: { contentOverride?: React.ReactNode; selectedMenuKeyOverride?: string } = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AppShell pageMap={pageMap()} {...props} />
    </QueryClientProvider>,
  );
}

async function openTabByMenu(label: string) {
  const user = userEvent.setup();
  await user.click(await screen.findByTitle(label));
  await waitFor(() => {
    expect(useTabStore.getState().activeKey).toBe(
      TREE.flatMap((node) => [node, ...(node.children ?? [])]).find((node) => node.name === label)?.code,
    );
  });
}

beforeEach(() => {
  resetStores();
  api.fetchMenus.mockResolvedValue(TREE);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('AppShell 标签页集成', () => {
  it('菜单点击 → 新标签出现并激活，菜单高亮同步（RTL-01）', async () => {
    renderShell();
    await openTabByMenu('知识归属修正');
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge', 'knowledge_fix']);
    // 面板已挂载且可见
    const pane = document.querySelector('[data-tab-key="knowledge_fix"]') as HTMLElement;
    expect(pane).not.toBeNull();
    expect(pane.style.display).not.toBe('none');
    expect(screen.getByText('知识归属修正内容')).toBeTruthy();
    // URL 联动（§3.4 正向）
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');
    // 持久化
    expect(localStorage.getItem('guoyu_admin_tabs:10159')).toContain('knowledge_fix');
  });

  it('保活：切走再切回，面板不重挂载（RTL-02 存在性口径）', async () => {
    renderShell();
    await openTabByMenu('知识归属修正');
    const paneBefore = document.querySelector('[data-tab-key="knowledge_fix"]');
    await openTabByMenu('用户管理');
    // knowledge_fix 面板仍在 DOM（保活），仅隐藏
    const paneHidden = document.querySelector('[data-tab-key="knowledge_fix"]');
    expect(paneHidden).toBe(paneBefore);
    expect((paneHidden as HTMLElement).style.display).toBe('none');
    // 切回
    await act(async () => {
      useTabStore.getState().activateTab('knowledge_fix');
    });
    const paneBack = document.querySelector('[data-tab-key="knowledge_fix"]');
    expect(paneBack).toBe(paneBefore);
    expect((paneBack as HTMLElement).style.display).not.toBe('none');
  });

  it('深链直达：进入时 URL 解析到菜单码 → 打开并激活对应标签（§3.4）', async () => {
    window.history.replaceState({}, '', '/system/users');
    renderShell();
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('system_user');
    });
    expect(useTabStore.getState().tabs.some((t) => t.key === 'system_user')).toBe(true);
  });

  it('localStorage 恢复：失效码剔除、激活位还原（§3.5）', async () => {
    localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
      version: 1,
      tabs: [
        { key: 'knowledge', title: '管理概览', path: '/knowledge', closable: false },
        { key: 'knowledge_fix', title: '知识归属修正', path: '/question-bank/knowledge-fix', closable: true },
        { key: 'ghost_x', title: '已删菜单', path: '/ghost', closable: true },
      ],
      activeKey: 'knowledge_fix',
      scrollPositions: {},
    }));
    renderShell();
    await waitFor(() => {
      expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge', 'knowledge_fix']);
    });
    expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
  });

  it('popstate 反查：浏览器后退到菜单 path → 激活既有标签（§3.4 反向）', async () => {
    renderShell();
    await openTabByMenu('知识归属修正');
    await openTabByMenu('用户管理');
    expect(useTabStore.getState().activeKey).toBe('system_user');
    // 模拟浏览器后退到 knowledge_fix 的 URL
    await act(async () => {
      window.history.pushState({}, '', '/question-bank/knowledge-fix');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
    });
  });

  it('popstate 到未开标签的菜单 path → 开新标签（§3.4 反向 2）', async () => {
    renderShell();
    expect(useTabStore.getState().tabs).toHaveLength(1);
    await act(async () => {
      window.history.pushState({}, '', '/system/users');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('system_user');
    });
    expect(useTabStore.getState().tabs).toHaveLength(2);
  });

  it('隐藏路由 path 不进标签、不写 URL（§3.4 附录 A 双向排除）', async () => {
    renderShell();
    await openTabByMenu('知识归属修正');
    // 正向：激活带隐藏路由 path 的标签不写 URL
    await act(async () => {
      useTabStore.getState().openTab('competition_event');
    });
    expect(useTabStore.getState().activeKey).toBe('competition_event');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');
    // 反向：popstate 到隐藏路由 → tabStore 不介入（交给既有 shell，此处无 shell）
    await act(async () => {
      window.history.pushState({}, '', '/competition/events/123');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    expect(useTabStore.getState().activeKey).toBe('competition_event');
    expect(useTabStore.getState().tabs.some((t) => t.key === 'system_user')).toBe(false);
  });

  it('UT-18 下钻守卫：程序化写入的目录码不自动拉起子页标签（§3.5）', async () => {
    // 目录码（无 pageMap、非用户点击链路写入）：守卫缺失时，目录下钻 effect 会重定向到 certification_dashboard
    await act(async () => {
      useTabStore.getState().openTab('cert_dir');
    });
    renderShell();
    await waitFor(() => {
      expect(useAuthStore.getState().menuTree.length).toBeGreaterThan(0);
    });
    await waitFor(() => {
      // 恢复校验会剔除无法渲染的目录标签，但绝不应"下钻"出子页标签
      expect(useTabStore.getState().tabs.some((t) => t.key === 'certification_dashboard')).toBe(false);
    });
  });

  it('document.title 跟随激活标签（§3.3）', async () => {
    renderShell();
    await openTabByMenu('知识归属修正');
    await waitFor(() => {
      expect(document.title).toBe('知识归属修正 - 千铝国昱云平台');
    });
  });

  it('>10 软提示：打开第 11 个标签时展示非阻塞提示一次（RTL-11）', async () => {
    renderShell();
    await waitFor(() => {
      expect(useAuthStore.getState().menuTree.length).toBeGreaterThan(0);
    });
    const codes = ['knowledge_fix', 'system_user',
      'ghost_1', 'ghost_2', 'ghost_3', 'ghost_4', 'ghost_5', 'ghost_6', 'ghost_7', 'ghost_8'];
    await act(async () => {
      codes.forEach((code) => useTabStore.getState().openTab(code));
    });
    expect(useTabStore.getState().tabs.length).toBe(11);
    expect(useTabStore.getState().softPromptVisible).toBe(false); // AppShell 已消费并 dismiss
    // 提示横幅出现过（notification 渲染到 body）
    expect(document.body.textContent).toContain('标签页较多');
  });

  it('固定标签不可关闭：关闭图标不渲染（closable=false，RTL/Q4）', async () => {
    renderShell();
    await waitFor(() => {
      expect(useTabStore.getState().tabs.length).toBeGreaterThan(0);
    });
    const fixedTabNode = document.querySelector('.ant-tabs-tab[data-node-key="knowledge"]');
    expect(fixedTabNode).not.toBeNull();
    expect(fixedTabNode!.querySelector('.ant-tabs-tab-remove')).toBeNull();
  });
});

describe('覆盖态（隐藏路由 shell）标签切换（2026-10-27 用户反馈回归）', () => {
  const OVERRIDE_URL = '/training/exam-lists/9/review-sync-stats';

  /** 模拟 main.tsx 的隐藏路由外壳：active 为 true 时提供覆盖内容，onOverrideExit 时撤掉 */
  function OverrideHarness() {
    const [active, setActive] = useState(true);
    return (
      <AppShell
        pageMap={pageMap()}
        contentOverride={active ? <div>统计覆盖内容</div> : undefined}
        selectedMenuKeyOverride={active ? 'kao_shi_ji_hua' : undefined}
        onOverrideExit={() => setActive(false)}
      />
    );
  }

  /** 渲染覆盖态外壳并进入覆盖态：考试计划标签被 AI 同步学统计劫持 */
  async function enterOverride() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <OverrideHarness />
      </QueryClientProvider>,
    );
    await waitFor(() => {
      expect(useAuthStore.getState().menuTree.length).toBeGreaterThan(0);
    });
    await act(async () => {
      useTabStore.getState().openTab('knowledge_fix');
      useTabStore.getState().openOverrideTab('kao_shi_ji_hua', undefined, OVERRIDE_URL);
    });
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('kao_shi_ji_hua');
    });
    expect(screen.getByText('统计覆盖内容')).toBeTruthy();
  }

  async function clickTab(key: string) {
    const tabNode = document.querySelector(`.ant-tabs-tab[data-node-key="${key}"]`) as HTMLElement;
    expect(tabNode).not.toBeNull();
    const user = userEvent.setup();
    await user.click(tabNode.querySelector('.app-tab-label-text span') as HTMLElement);
  }

  it('覆盖态下点击其它标签 → 退出覆盖态、渲染目标面板（此前点击无反应）', async () => {
    await enterOverride();

    await clickTab('knowledge_fix');

    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
    });
    // 覆盖内容卸载，目标保活面板可见
    expect(screen.queryByText('统计覆盖内容')).toBeNull();
    const pane = document.querySelector('[data-tab-key="knowledge_fix"]') as HTMLElement;
    expect(pane.style.display).not.toBe('none');
    expect(screen.getByText('知识归属修正内容')).toBeTruthy();
    // 高亮跟随（此前会被 selectedMenuKeyOverride 钉死在考试计划）
    expect(document.querySelector('.ant-tabs-tab-active')?.getAttribute('data-node-key')).toBe('knowledge_fix');
  });

  it('点回考试计划标签 → 回普通菜单页（overridePath 已解除），不再跳回统计页', async () => {
    await enterOverride();
    await clickTab('knowledge_fix');
    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
    });

    await clickTab('kao_shi_ji_hua');

    await waitFor(() => {
      expect(useTabStore.getState().activeKey).toBe('kao_shi_ji_hua');
    });
    // 载体标签的 overridePath 已被清除 → URL 走菜单 path，而不是隐藏路由
    expect(window.location.pathname).toBe('/kao-shi-ji-hua');
    expect(screen.getByText('考试计划内容')).toBeTruthy();
    expect(screen.queryByText('统计覆盖内容')).toBeNull();
  });
});

describe('灰度开关（RTL-12，§7）', () => {
  it('off 时走旧渲染分支：无 TabBar、内容区单页', async () => {
    resetStores();
    localStorage.setItem('guoyu_admin_tabs_switch', 'off');
    renderShell();
    await screen.findByText('管理概览内容');
    expect(document.querySelector('.app-tabbar-strip')).toBeNull();
    // 旧分支内容区直接渲染 pageMap[selectedKey]
    expect(screen.getByText('管理概览内容')).toBeTruthy();
    localStorage.removeItem('guoyu_admin_tabs_switch');
  });
});
