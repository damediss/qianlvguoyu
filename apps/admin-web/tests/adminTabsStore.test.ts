/**
 * admin-web 标签页数据层单测（《admin-web标签页外壳改造方案》§5.1 UT-01~20）。
 * 覆盖：tabStore 开/关/固定标签/别名归一/恢复校验/滚动/overridePath/软提示/租户隔离/排序、
 * navStore 委托、hiddenRoutes 与 findMenuCodeByPath 反查。
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../src/stores/authStore';
import { useNavStore } from '../src/stores/navStore';
import { FIXED_TAB_KEY, useTabStore } from '../src/stores/tabStore';
import type { TabItem } from '../src/stores/tabStore';
import { findMenuCodeByPath } from '../src/routes/competitionRoute';
import { isHiddenRoutePath } from '../src/routes/hiddenRoutes';
import type { MenuVO } from '../src/types/api';

function menu(code: string, name: string, path: string, menuType = 'menu'): MenuVO {
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
    children: [],
  };
}

const TREE: MenuVO[] = [
  menu('knowledge', '管理概览', '/knowledge'),
  menu('knowledge_fix', '知识归属修正', '/question-bank/knowledge-fix'),
  menu('certification_dashboard', '认定数据面板', '/certification/dashboard'),
  menu('system_user', '用户管理', '/system/users'),
  menu('competition_event', '赛事创建', '/competition/events'),
  // 真实存在、含连字符的菜单码（不得被 kebab→snake 误归一，§3.2）
  menu('train_people-list', '花名册', '/training/people-list'),
  // D1 回归用：菜单 path 为单数 /training/exam-list，不命中隐藏匹配器（正是 D1 的坑位）
  menu('train_exam-list', '考试计划', '/training/exam-list'),
];

function fixedTabItem(): TabItem {
  return { key: FIXED_TAB_KEY, title: '管理概览', path: '/knowledge', closable: false };
}

function resetStores(tenantId: number | undefined = 10159) {
  localStorage.clear();
  sessionStorage.clear();
  useAuthStore.setState({
    menuTree: TREE,
    tenantId,
    token: 'test-token',
    currentUser: undefined,
    permissionCodes: [],
  });
  useTabStore.setState({
    tabs: [fixedTabItem()],
    activeKey: FIXED_TAB_KEY,
    visitedKeys: [FIXED_TAB_KEY],
    scrollPositions: {},
    refreshCounters: {},
    softPromptVisible: false,
  });
  window.history.replaceState({}, '', '/');
}

beforeEach(() => {
  resetStores();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('tabStore 开/关与固定标签（UT-01~05）', () => {
  it('UT-01 openTab 新码追加标签并激活', () => {
    useTabStore.getState().openTab('knowledge_fix');
    const { tabs, activeKey } = useTabStore.getState();
    expect(tabs.map((t) => t.key)).toEqual(['knowledge', 'knowledge_fix']);
    expect(activeKey).toBe('knowledge_fix');
    expect(tabs[1].title).toBe('知识归属修正');
    expect(tabs[1].path).toBe('/question-bank/knowledge-fix');
    expect(tabs[1].closable).toBe(true);
  });

  it('UT-02 openTab 已存在码仅激活不重复追加（幂等，含 StrictMode 双调用）', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('knowledge_fix');
    const { tabs, activeKey } = useTabStore.getState();
    expect(tabs).toHaveLength(2);
    expect(activeKey).toBe('knowledge_fix');
  });

  it('UT-03 固定首标签 knowledge 恒在且不可关闭', () => {
    const { tabs } = useTabStore.getState();
    expect(tabs[0]).toMatchObject({ key: 'knowledge', closable: false, title: '管理概览' });
    useTabStore.getState().closeTab('knowledge');
    expect(useTabStore.getState().tabs[0].key).toBe('knowledge');
  });

  it('UT-04 closeTab 关闭激活标签时激活位 fallback 左邻→固定', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('certification_dashboard');
    useTabStore.getState().openTab('system_user');
    // 关闭激活的最后一个 → 落到左邻
    useTabStore.getState().closeTab('system_user');
    expect(useTabStore.getState().activeKey).toBe('certification_dashboard');
    // 再关激活的中间标签 → 落到左邻 knowledge_fix
    useTabStore.getState().closeTab('certification_dashboard');
    expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
    // 关闭激活的第一个可关标签 → 左邻即固定标签
    useTabStore.getState().closeTab('knowledge_fix');
    expect(useTabStore.getState().activeKey).toBe('knowledge');
  });

  it('UT-05 closeOthers/closeRight/closeAll 均保留固定标签', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('certification_dashboard');
    useTabStore.getState().openTab('system_user');

    useTabStore.getState().closeOthers('certification_dashboard');
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge', 'certification_dashboard']);
    expect(useTabStore.getState().activeKey).toBe('certification_dashboard');

    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('system_user');
    useTabStore.getState().closeRight('certification_dashboard');
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge', 'certification_dashboard']);

    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().closeAll();
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge']);
    expect(useTabStore.getState().activeKey).toBe('knowledge');
  });
});

describe('tabStore 别名归一（UT-06）', () => {
  it('kebab 别名归一到主菜单码；真实连字符菜单码不被误并；未知码原样保留并 warn', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    // kebab → snake
    useTabStore.getState().openTab('certification-dashboard');
    expect(useTabStore.getState().activeKey).toBe('certification_dashboard');
    // 真实连字符菜单码不被误归一
    useTabStore.getState().openTab('train_people-list');
    expect(useTabStore.getState().tabs.some((t) => t.key === 'train_people-list')).toBe(true);
    // 未知码：按原样开标签 + console.warn
    useTabStore.getState().openTab('ghost_page');
    expect(useTabStore.getState().tabs.some((t) => t.key === 'ghost_page')).toBe(true);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('按原样打开'), 'ghost_page');
  });
});

describe('tabStore 恢复校验（UT-07）与滚动（UT-08）', () => {
  it('UT-07 restoreFromStorage 剔除失效码、activeKey 越界回退固定标签', () => {
    localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
      version: 1,
      tabs: [
        fixedTabItem(),
        { key: 'knowledge_fix', title: '知识归属修正', path: '/question-bank/knowledge-fix', closable: true },
        { key: 'ghost_x', title: '已删菜单', path: '/ghost', closable: true },
      ],
      activeKey: 'ghost_x',
      scrollPositions: { knowledge_fix: 120 },
    }));
    useTabStore.getState().restoreFromStorage(TREE, (key) => key === 'knowledge' || key === 'knowledge_fix');
    const { tabs, activeKey, scrollPositions } = useTabStore.getState();
    expect(tabs.map((t) => t.key)).toEqual(['knowledge', 'knowledge_fix']);
    expect(activeKey).toBe('knowledge');
    expect(scrollPositions.knowledge_fix).toBe(120);
  });

  it('UT-07b 全部失效回落固定标签；isValidKey 不放行的码同样剔除', () => {
    localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
      version: 1,
      tabs: [
        fixedTabItem(),
        { key: 'knowledge_fix', title: 'x', path: '/x', closable: true },
      ],
      activeKey: 'knowledge_fix',
      scrollPositions: {},
    }));
    useTabStore.getState().restoreFromStorage(TREE, () => false);
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge']);
    expect(useTabStore.getState().activeKey).toBe('knowledge');
  });

  it('UT-08 scrollPositions 存取；关标签清理对应滚动记录', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().saveScroll('knowledge_fix', 240);
    expect(useTabStore.getState().scrollPositions.knowledge_fix).toBe(240);
    useTabStore.getState().closeTab('knowledge_fix');
    expect(useTabStore.getState().scrollPositions.knowledge_fix).toBeUndefined();
  });
});

describe('tabStore overridePath 与 URL 联动（UT-09/11/13）', () => {
  it('UT-11 激活普通菜单写 URL；隐藏路由菜单与空 path 不写', () => {
    useTabStore.getState().openTab('knowledge_fix');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');

    // 隐藏路由菜单（竞赛）：未进覆盖态前不写 URL（§3.4 正向排除）
    useTabStore.getState().openTab('competition_event');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');

    // 空 path 菜单不写 URL
    useTabStore.getState().openTab('ghost_page');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');
  });

  it('UT-09/13 overridePath 激活 → pushState + 派发 popstate（覆盖态回退）', () => {
    const popstateSpy = vi.spyOn(window, 'dispatchEvent');
    useTabStore.getState().openTab('competition_event');
    // 隐藏路由 path：openTab 新建时也不写 URL（§3.4）
    expect(window.location.pathname).toBe('/');
    useTabStore.getState().setOverridePath('competition_event', '/competition/theory-exams?eventId=9');
    // 切到别的标签，再点回覆盖态标签 → pushState + popstate（覆盖态回退，§3.7）
    useTabStore.getState().openTab('knowledge_fix');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');
    useTabStore.getState().activateTab('competition_event');
    expect(window.location.pathname).toBe('/competition/theory-exams');
    expect(window.location.search).toBe('?eventId=9');
    expect(popstateSpy.mock.calls.some(([event]) => event?.type === 'popstate')).toBe(true);
    // 同一 URL 重复激活不重复 pushState 历史
    const lengthBefore = window.history.length;
    useTabStore.getState().activateTab('competition_event');
    expect(window.history.length).toBe(lengthBefore);
  });
});

describe('tabStore 软提示（UT-10，>10）', () => {
  it('打开第 11 个标签触发一次，同会话不重复', () => {
    const codes = [
      'knowledge_fix', 'certification_dashboard', 'system_user', 'competition_event', 'train_people-list',
      'ghost_1', 'ghost_2', 'ghost_3', 'ghost_4', 'ghost_5',
    ];
    codes.forEach((code) => useTabStore.getState().openTab(code));
    // 前 10 个标签（含固定）不提示
    expect(useTabStore.getState().tabs.length).toBe(11);
    expect(useTabStore.getState().softPromptVisible).toBe(true);
    expect(sessionStorage.getItem('guoyu_admin_tabs_soft_prompt')).toBe('1');
    useTabStore.getState().dismissSoftPrompt();
    expect(useTabStore.getState().softPromptVisible).toBe(false);
    // 关掉一个再开新的（仍 >10）：同会话不再提示
    useTabStore.getState().closeTab('ghost_5');
    useTabStore.getState().openTab('ghost_6');
    expect(useTabStore.getState().tabs.length).toBe(11);
    expect(useTabStore.getState().softPromptVisible).toBe(false);
  });

  it('第 10 个标签不触发', () => {
    const codes = [
      'knowledge_fix', 'certification_dashboard', 'system_user', 'competition_event',
      'ghost_1', 'ghost_2', 'ghost_3', 'ghost_4', 'ghost_5',
    ];
    codes.forEach((code) => useTabStore.getState().openTab(code));
    expect(useTabStore.getState().tabs.length).toBe(10);
    expect(useTabStore.getState().softPromptVisible).toBe(false);
  });
});

describe('clearTabs 与 401 无关（UT-16）', () => {
  it('clearAuth 不清标签；clearTabs 复位为固定标签', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useAuthStore.getState().clearAuth();
    expect(useTabStore.getState().tabs).toHaveLength(2); // 401 被动失效路径不清标签（§3.2）
    useAuthStore.setState({ menuTree: TREE, token: 't2' });
    useTabStore.getState().clearTabs();
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge']);
    expect(useTabStore.getState().activeKey).toBe('knowledge');
  });
});

describe('persist 租户隔离（UT-17）', () => {
  it('persist key 并入 tenantId，互不读写', () => {
    useAuthStore.setState({ tenantId: 1 });
    useTabStore.getState().openTab('knowledge_fix');
    expect(localStorage.getItem('guoyu_admin_tabs:1')).not.toBeNull();
    expect(localStorage.getItem('guoyu_admin_tabs:2')).toBeNull();

    // 切到乙租户：读取 :2 为空 → 仅固定标签
    useAuthStore.setState({ tenantId: 2 });
    useTabStore.getState().restoreFromStorage(TREE, (key) => TREE.some((m) => m.code === key));
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge']);
    useTabStore.getState().openTab('system_user');
    expect(localStorage.getItem('guoyu_admin_tabs:2')).not.toBeNull();
  });
});

describe('navStore 委托（UT-19）', () => {
  it('setSelectedKey 委托 openTab 并镜像归一后的激活 key', () => {
    useNavStore.getState().setSelectedKey('certification-dashboard');
    expect(useTabStore.getState().activeKey).toBe('certification_dashboard');
    expect(useNavStore.getState().selectedKey).toBe('certification_dashboard');
  });
});

describe('reorderTabs（UT-20）', () => {
  it('拖拽排序持久化且固定标签恒在首位', () => {
    useTabStore.getState().openTab('knowledge_fix');
    useTabStore.getState().openTab('certification_dashboard');
    useTabStore.getState().openTab('system_user');
    useTabStore.getState().reorderTabs('system_user', 1);
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge', 'system_user', 'knowledge_fix', 'certification_dashboard']);
    // 目标位 0 被钳制到 1（固定标签不可被挤掉）
    useTabStore.getState().reorderTabs('certification_dashboard', 0);
    expect(useTabStore.getState().tabs[0].key).toBe('knowledge');
    expect(useTabStore.getState().tabs[1].key).toBe('certification_dashboard');
  });
});

describe('覆盖态标签原子入口 openOverrideTab（D1 回归）', () => {
  it('创建标签全程不触碰 URL：直达隐藏路由 URL 不被菜单 path 改写', () => {
    // 模拟直达 /training/exam-lists/123/review-sync-stats 后 shell 的接线
    window.history.replaceState({}, '', '/training/exam-lists/123/review-sync-stats');
    const popstateSpy = vi.spyOn(window, 'dispatchEvent');
    useTabStore.getState().openOverrideTab('train_exam-list', undefined, '/training/exam-lists/123/review-sync-stats');
    // URL 原样保留（菜单 path /training/exam-list 不命中隐藏匹配器，旧实现会在这里改写 URL）
    expect(window.location.pathname).toBe('/training/exam-lists/123/review-sync-stats');
    expect(useTabStore.getState().activeKey).toBe('train_exam-list');
    const tab = useTabStore.getState().tabs.find((t) => t.key === 'train_exam-list')!;
    expect(tab.overridePath).toBe('/training/exam-lists/123/review-sync-stats');
    expect(tab.title).toBe('考试计划');
    // 不派发 popstate（App() 已在该 shell，无需重选）
    expect(popstateSpy.mock.calls.some(([event]) => event?.type === 'popstate')).toBe(false);
  });

  it('再次进入更新 overridePath 并激活，不重复开标签、不触碰 URL', () => {
    useTabStore.getState().openOverrideTab('train_exam-list', undefined, '/training/exam-lists/123/review-sync-stats');
    const countBefore = useTabStore.getState().tabs.length;
    useTabStore.getState().openOverrideTab('train_exam-list', undefined, '/training/exam-lists/456/review-sync-stats');
    expect(useTabStore.getState().tabs.length).toBe(countBefore);
    expect(useTabStore.getState().tabs.find((t) => t.key === 'train_exam-list')!.overridePath)
      .toBe('/training/exam-lists/456/review-sync-stats');
    expect(useTabStore.getState().activeKey).toBe('train_exam-list');
  });

  it('覆盖态回退链路完整：overridePath 写入后点回标签 → URL 恢复 + popstate', () => {
    window.history.replaceState({}, '', '/training/exam-lists/123/review-sync-stats');
    useTabStore.getState().openOverrideTab('train_exam-list', undefined, '/training/exam-lists/123/review-sync-stats');
    // 切到普通标签（URL 变菜单 path），再点回覆盖态标签
    useTabStore.getState().openTab('knowledge_fix');
    expect(window.location.pathname).toBe('/question-bank/knowledge-fix');
    const popstateSpy = vi.spyOn(window, 'dispatchEvent');
    useTabStore.getState().activateTab('train_exam-list');
    expect(window.location.pathname).toBe('/training/exam-lists/123/review-sync-stats');
    expect(popstateSpy.mock.calls.some(([event]) => event?.type === 'popstate')).toBe(true);
  });
});

describe('openTab 既有标签同步 title/path（次要①回归）', () => {
  it('菜单改名/补 path 后重开同步最新值，不重复开标签', () => {
    useTabStore.getState().openTab('knowledge_fix');
    const countBefore = useTabStore.getState().tabs.length;
    useTabStore.getState().openTab('knowledge_fix', '知识归属修正（新）', '/question-bank/knowledge-fix-v2');
    expect(useTabStore.getState().tabs.length).toBe(countBefore);
    const tab = useTabStore.getState().tabs.find((t) => t.key === 'knowledge_fix')!;
    expect(tab.title).toBe('知识归属修正（新）');
    expect(tab.path).toBe('/question-bank/knowledge-fix-v2');
  });
});

describe('恢复校验与 visitedKeys（D3 + 次要②回归）', () => {
  it('UT-D3 恢复后仅激活标签入 visitedKeys，其余等首次激活再挂载', () => {
    localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
      version: 1,
      tabs: [
        fixedTabItem(),
        { key: 'knowledge_fix', title: '知识归属修正', path: '/question-bank/knowledge-fix', closable: true },
        { key: 'system_user', title: '用户管理', path: '/system/users', closable: true },
      ],
      activeKey: 'system_user',
      scrollPositions: {},
    }));
    useTabStore.getState().restoreFromStorage(TREE, (key) => TREE.some((m) => m.code === key));
    expect(useTabStore.getState().tabs).toHaveLength(3);
    expect(useTabStore.getState().visitedKeys).toEqual(['system_user']); // 而非全部 3 个
  });

  it('UT-minor2 调用方传过滤后的树：树中存在但被判 hidden 的码不恢复成标签', () => {
    const treeWithHidden = [...TREE, menu('competition_theory_exam', '理论监考', '/competition/theory-exam')];
    localStorage.setItem('guoyu_admin_tabs:10159', JSON.stringify({
      version: 1,
      tabs: [
        fixedTabItem(),
        { key: 'competition_theory_exam', title: '理论监考', path: '/competition/theory-exam', closable: true },
      ],
      activeKey: 'competition_theory_exam',
      scrollPositions: {},
    }));
    // AppShell 实际传 visibleMenuTree（剔除 hiddenMenuCodes），这里模拟该过滤
    const visibleTree = treeWithHidden.filter((node) => node.code !== 'competition_theory_exam');
    useTabStore.getState().restoreFromStorage(visibleTree, (key) => visibleTree.some((m) => m.code === key));
    expect(useTabStore.getState().tabs.map((t) => t.key)).toEqual(['knowledge']);
    expect(useTabStore.getState().activeKey).toBe('knowledge');
  });
});

describe('hiddenRoutes 与 findMenuCodeByPath（UT-11/12 数据源）', () => {
  it('isHiddenRoutePath 覆盖附录 A 全部匹配器', () => {
    expect(isHiddenRoutePath('/competition')).toBe(true);
    expect(isHiddenRoutePath('/competition/events/123')).toBe(true);
    expect(isHiddenRoutePath('/training/exam-lists/5/review-sync-stats')).toBe(true);
    expect(isHiddenRoutePath('/training/exam-lists/5/review-sync-stats/student/7')).toBe(true);
    expect(isHiddenRoutePath('/training/graduation/classes/12')).toBe(true);
    expect(isHiddenRoutePath('/training/graduation/classes/12/')).toBe(true);
    expect(isHiddenRoutePath('/certification/practical-collection/abc/manage')).toBe(true);
    expect(isHiddenRoutePath('/certification/practical-collection/abc/manage?examSessionId=1')).toBe(true);
    expect(isHiddenRoutePath('/knowledge')).toBe(false);
    expect(isHiddenRoutePath('/training/exam-lists')).toBe(false);
    expect(isHiddenRoutePath('/training/graduation/classes')).toBe(false);
    expect(isHiddenRoutePath('/')).toBe(false);
  });

  it('findMenuCodeByPath 精确优先、其次最长前缀', () => {
    expect(findMenuCodeByPath(TREE, '/question-bank/knowledge-fix')).toBe('knowledge_fix');
    expect(findMenuCodeByPath(TREE, '/question-bank/knowledge-fix/')).toBe('knowledge_fix');
    expect(findMenuCodeByPath(TREE, '/competition/events/123')).toBe('competition_event');
    expect(findMenuCodeByPath(TREE, '/system/users')).toBe('system_user');
    expect(findMenuCodeByPath(TREE, '/unknown/path')).toBeUndefined();
    expect(findMenuCodeByPath(TREE, '/')).toBeUndefined();
  });
});

describe('旧存储迁移（§3.5/Q8）', () => {
  it('无 tabs 存储时读取 guoyu_admin_selected_menu 作为初始激活标签，且不删除该 key', () => {
    localStorage.setItem('guoyu_admin_selected_menu', 'knowledge_fix');
    useTabStore.getState().restoreFromStorage(TREE, (key) => TREE.some((m) => m.code === key));
    expect(useTabStore.getState().activeKey).toBe('knowledge_fix');
    expect(localStorage.getItem('guoyu_admin_selected_menu')).toBe('knowledge_fix');
  });
});
