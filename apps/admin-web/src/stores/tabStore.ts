import { create } from 'zustand';
import { useAuthStore } from './authStore';
import type { MenuVO } from '../types/api';
import { isHiddenRoutePath } from '../routes/hiddenRoutes';

/**
 * 标签页数据层（《admin-web标签页外壳改造方案》§3.2）。
 * - 一个菜单码一个标签；tabs[0] 为首页（优先 knowledge，无权限则取菜单树首个页面）
 * - persist：localStorage `guoyu_admin_tabs:${tenantId}`（租户隔离）
 * - URL 联动：activateTab 时 pushState；隐藏路由标签走 overridePath + popstate（§3.4/§3.7）
 * - clearTabs 仅由显式登出/切换端入口调用（401 被动失效不清，见 §3.2 清退时机）
 */
export interface TabItem {
  key: string;
  title: string;
  path: string;
  closable: boolean;
  /** 覆盖态标签专用：隐藏路由完整 URL（含 query），§3.7 覆盖态回退的载体 */
  overridePath?: string;
}

/** 首选首页（有菜单权限时） */
export const PREFERRED_HOME_TAB_KEY = 'knowledge';
/** @deprecated 请使用 resolveHomeTabKey(menuTree)；保留导出兼容旧引用 */
export const FIXED_TAB_KEY = PREFERRED_HOME_TAB_KEY;
export const FIXED_TAB_TITLE = '管理概览';

function findFirstPageCode(tree: MenuVO[]): string | null {
  for (const node of tree) {
    if ((node.menuType === 'page' || node.menuType === 'menu') && node.code) {
      return node.code;
    }
    if (node.children?.length) {
      const found = findFirstPageCode(node.children);
      if (found) return found;
    }
  }
  return null;
}

/** 当前用户可用的首页菜单码：优先 knowledge，否则菜单树第一个页面 */
export function resolveHomeTabKey(tree: MenuVO[] = []): string {
  if (findMenuNode(tree, PREFERRED_HOME_TAB_KEY)) {
    return PREFERRED_HOME_TAB_KEY;
  }
  return findFirstPageCode(tree) || PREFERRED_HOME_TAB_KEY;
}

/** 软提示阈值（Q10/v1.3）：打开第 11 个标签时提示一次 */
export const SOFT_PROMPT_LIMIT = 10;
const SOFT_PROMPT_SESSION_KEY = 'guoyu_admin_tabs_soft_prompt';
const PERSIST_VERSION = 1;

function tabsStorageKey() {
  const tenantId = useAuthStore.getState().tenantId;
  return `guoyu_admin_tabs:${tenantId ?? 0}`;
}

interface PersistedTabs {
  version: number;
  tabs: TabItem[];
  activeKey: string;
  scrollPositions: Record<string, number>;
}

function readPersisted(): PersistedTabs | null {
  try {
    const raw = localStorage.getItem(tabsStorageKey());
    if (!raw) return null;
    const parsed = JSON.parse(raw) as PersistedTabs;
    if (!parsed || parsed.version !== PERSIST_VERSION || !Array.isArray(parsed.tabs)) return null;
    return parsed;
  } catch {
    return null;
  }
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

/**
 * 动态别名归一（§3.2，不建静态表避免腐化）：
 * 1. menuTree 命中该 code → 直接使用；
 * 2. 否则 kebab→snake（replace(/-/g,'_')）再查 → 归一到主菜单码；
 * 3. 都不存在 → 仍以原 code 开标签，console.warn 提供清理线索。
 */
export function normalizeTabKey(key: string, tree: MenuVO[]): { key: string; node: MenuVO | null } {
  const direct = findMenuNode(tree, key);
  if (direct) return { key, node: direct };
  const snakeKey = key.replace(/-/g, '_');
  const snakeNode = snakeKey !== key ? findMenuNode(tree, snakeKey) : null;
  if (snakeNode) return { key: snakeKey, node: snakeNode };
  console.warn('[tabStore] 标签 key 未命中菜单树（含 kebab→snake 归一），按原样打开：', key);
  return { key, node: null };
}

function fixedTab(tree: MenuVO[] = []): TabItem {
  const homeKey = resolveHomeTabKey(tree);
  const node = findMenuNode(tree, homeKey);
  return {
    key: homeKey,
    title: homeKey === PREFERRED_HOME_TAB_KEY
      ? (node?.name || FIXED_TAB_TITLE)
      : (node?.name || homeKey),
    path: node?.path || '',
    closable: false,
  };
}

/** 保证固定首页标签存在、唯一且在首位 */
function ensureFixedFirst(tabs: TabItem[], tree: MenuVO[]): TabItem[] {
  const home = fixedTab(tree);
  const withoutHome = tabs.filter((tab) => {
    if (tab.key === home.key) return false;
    // 用户无 knowledge 权限时，丢掉历史残留的 knowledge 固定页，避免 403
    if (
      tab.key === PREFERRED_HOME_TAB_KEY
      && home.key !== PREFERRED_HOME_TAB_KEY
      && !findMenuNode(tree, PREFERRED_HOME_TAB_KEY)
    ) {
      return false;
    }
    return true;
  });
  return [home, ...withoutHome];
}

/** 软提示同会话（browser session）仅一次 */
function shouldRaiseSoftPrompt(tabCount: number) {
  if (tabCount <= SOFT_PROMPT_LIMIT) return false;
  try {
    if (sessionStorage.getItem(SOFT_PROMPT_SESSION_KEY)) return false;
    sessionStorage.setItem(SOFT_PROMPT_SESSION_KEY, '1');
  } catch {
    return false;
  }
  return true;
}

/**
 * 标签激活时的 URL 联动（§3.4/§3.7）：
 * ① overridePath 存在（覆盖态标签）→ pushState + 派发 popstate（全站唯一显式派发点）；
 * ② 普通菜单 path → 仅 pushState（不派发，见 §3.4 D2 状态滞后说明）；
 * ③ path 为空或命中隐藏路由 → 不写 URL（仅 localStorage 身份）。
 */
function applyTabUrl(tab: TabItem) {
  if (typeof window === 'undefined') return;
  if (tab.overridePath) {
    window.history.pushState({}, '', tab.overridePath);
    window.dispatchEvent(new PopStateEvent('popstate'));
    return;
  }
  if (tab.path && !isHiddenRoutePath(tab.path)) {
    const targetPath = tab.path.split(/[?#]/, 1)[0];
    if (window.location.pathname !== targetPath) {
      window.history.pushState({}, '', tab.path);
    }
  }
}

interface TabState {
  tabs: TabItem[];
  activeKey: string;
  /** 已激活过的标签（首次激活才挂载面板，之后 keep-alive；放 store 以跨隐藏路由 shell 存活） */
  visitedKeys: string[];
  scrollPositions: Record<string, number>;
  /** 标签重挂载计数（标签"刷新"按钮，§3.9） */
  refreshCounters: Record<string, number>;
  softPromptVisible: boolean;
  openTab: (key: string, title?: string, path?: string) => void;
  /** 覆盖态标签原子入口（D1）：创建/更新携带 overridePath，全程不触碰 URL（§3.7） */
  openOverrideTab: (key: string, title: string | undefined, overridePath: string) => void;
  activateTab: (key: string) => void;
  closeTab: (key: string) => void;
  closeOthers: (key: string) => void;
  closeRight: (key: string) => void;
  closeAll: () => void;
  reorderTabs: (fromKey: string, toIndex: number) => void;
  saveScroll: (key: string, top: number) => void;
  setOverridePath: (key: string, overridePath?: string) => void;
  refreshTab: (key: string) => void;
  dismissSoftPrompt: () => void;
  clearTabs: () => void;
  /** menuTree 异步到达后执行：localStorage 校验恢复 + 旧 key 迁移 + 深链直达（§3.5） */
  restoreFromStorage: (tree: MenuVO[], isValidKey: (key: string) => boolean) => void;
}

function persist(state: Pick<TabState, 'tabs' | 'activeKey' | 'scrollPositions'>) {
  try {
    const payload: PersistedTabs = {
      version: PERSIST_VERSION,
      tabs: state.tabs,
      activeKey: state.activeKey,
      scrollPositions: state.scrollPositions,
    };
    localStorage.setItem(tabsStorageKey(), JSON.stringify(payload));
  } catch {
    // 配额满/隐私模式等场景静默降级：标签仍可用，仅不恢复
  }
}

export const useTabStore = create<TabState>((set, get) => ({
  tabs: [fixedTab()],
  activeKey: PREFERRED_HOME_TAB_KEY,
  visitedKeys: [PREFERRED_HOME_TAB_KEY],
  scrollPositions: {},
  refreshCounters: {},
  softPromptVisible: false,

  openTab: (key, title, path) => {
    const tree = useAuthStore.getState().menuTree;
    const homeKey = resolveHomeTabKey(tree);
    const normalized = normalizeTabKey(key, tree);
    const tabKey = normalized.key;
    const existing = get().tabs.find((tab) => tab.key === tabKey);
    if (existing) {
      // 既有标签：菜单改名/补 path 后同步最新值（次要问题①），再激活
      const merged: TabItem = {
        ...existing,
        title: title || existing.title,
        path: path ?? existing.path,
      };
      if (merged.title !== existing.title || merged.path !== existing.path) {
        set({ tabs: get().tabs.map((tab) => (tab.key === tabKey ? merged : tab)) });
      }
      get().activateTab(tabKey);
      return;
    }
    const node = normalized.node;
    const tab: TabItem = {
      key: tabKey,
      title: title || node?.name || tabKey,
      path: path ?? node?.path ?? '',
      closable: tabKey !== homeKey,
    };
    const tabs = ensureFixedFirst([...get().tabs, tab], tree);
    set({
      tabs,
      activeKey: tabKey,
      visitedKeys: get().visitedKeys.includes(tabKey) ? get().visitedKeys : [...get().visitedKeys, tabKey],
      softPromptVisible: get().softPromptVisible || shouldRaiseSoftPrompt(tabs.length),
    });
    applyTabUrl(tab);
    persist(get());
  },

  /**
   * 覆盖态标签的原子入口（D1 修复）：创建/更新携带 overridePath 的标签，
   * 全程不触碰 URL——进入覆盖态时当前 URL 就是目标 URL，绝不能用菜单 path 改写。
   * 现有标签只更新 overridePath 并激活（不重复 pushState）。
   */
  openOverrideTab: (key, title, overridePath) => {
    const tree = useAuthStore.getState().menuTree;
    const normalized = normalizeTabKey(key, tree);
    const tabKey = normalized.key;
    const node = normalized.node;
    const existing = get().tabs.find((tab) => tab.key === tabKey);
    if (existing) {
      const merged: TabItem = { ...existing, overridePath, title: title || existing.title };
      set({
        tabs: get().tabs.map((tab) => (tab.key === tabKey ? merged : tab)),
        activeKey: tabKey,
        visitedKeys: get().visitedKeys.includes(tabKey) ? get().visitedKeys : [...get().visitedKeys, tabKey],
      });
      persist(get());
      return;
    }
    const homeKey = resolveHomeTabKey(tree);
    const tab: TabItem = {
      key: tabKey,
      title: title || node?.name || tabKey,
      path: node?.path ?? '',
      closable: tabKey !== homeKey,
      overridePath,
    };
    set({
      tabs: ensureFixedFirst([...get().tabs, tab], tree),
      activeKey: tabKey,
      visitedKeys: get().visitedKeys.includes(tabKey) ? get().visitedKeys : [...get().visitedKeys, tabKey],
      softPromptVisible: get().softPromptVisible || shouldRaiseSoftPrompt(get().tabs.length + 1),
    });
    persist(get());
  },

  activateTab: (key) => {
    const { tabs, activeKey } = get();
    const tab = tabs.find((item) => item.key === key);
    if (!tab || activeKey === key) return;
    set({
      activeKey: key,
      visitedKeys: get().visitedKeys.includes(key) ? get().visitedKeys : [...get().visitedKeys, key],
    });
    applyTabUrl(tab);
    persist(get());
  },

  closeTab: (key) => {
    const tree = useAuthStore.getState().menuTree;
    const homeKey = resolveHomeTabKey(tree);
    const { tabs, activeKey, scrollPositions } = get();
    const index = tabs.findIndex((tab) => tab.key === key);
    if (index < 0 || !tabs[index].closable) return;
    const next = tabs.filter((tab) => tab.key !== key);
    // 激活位 fallback：左邻 → 右邻 → 固定首页（§3.2）
    let nextActive = activeKey;
    if (activeKey === key) {
      nextActive = next[index - 1]?.key ?? next[index]?.key ?? homeKey;
    }
    const nextScrolls = { ...scrollPositions };
    delete nextScrolls[key];
    set({ tabs: next, activeKey: nextActive, scrollPositions: nextScrolls });
    persist(get());
  },

  closeOthers: (key) => {
    const tree = useAuthStore.getState().menuTree;
    const target = get().tabs.find((tab) => tab.key === key);
    if (!target) return;
    const next = [fixedTab(tree), ...get().tabs.filter((tab) => tab.closable && tab.key === key)];
    const nextScrolls: Record<string, number> = {};
    next.forEach((tab) => {
      if (get().scrollPositions[tab.key] != null) nextScrolls[tab.key] = get().scrollPositions[tab.key];
    });
    set({ tabs: next, activeKey: key, scrollPositions: nextScrolls });
    persist(get());
  },

  closeRight: (key) => {
    const index = get().tabs.findIndex((tab) => tab.key === key);
    if (index < 0) return;
    const kept = get().tabs.slice(0, index + 1);
    const removed = get().tabs.slice(index + 1).map((tab) => tab.key);
    const nextScrolls = { ...get().scrollPositions };
    removed.forEach((removedKey) => delete nextScrolls[removedKey]);
    const activeKey = removed.includes(get().activeKey) ? key : get().activeKey;
    set({ tabs: kept, activeKey, scrollPositions: nextScrolls });
    persist(get());
  },

  closeAll: () => {
    const tree = useAuthStore.getState().menuTree;
    const home = fixedTab(tree);
    set({ tabs: [home], activeKey: home.key, scrollPositions: {} });
    persist(get());
  },

  reorderTabs: (fromKey, toIndex) => {
    const tabs = [...get().tabs];
    const fromIndex = tabs.findIndex((tab) => tab.key === fromKey);
    if (fromIndex < 0) return;
    const clamped = Math.max(1, Math.min(toIndex, tabs.length - 1)); // 固定标签恒在首位
    if (clamped === fromIndex) return;
    const [moved] = tabs.splice(fromIndex, 1);
    tabs.splice(clamped, 0, moved);
    set({ tabs });
    persist(get());
  },

  saveScroll: (key, top) => {
    if (!key) return;
    set({ scrollPositions: { ...get().scrollPositions, [key]: top } });
  },

  setOverridePath: (key, overridePath) => {
    const tabs = get().tabs.map((tab) => (tab.key === key ? { ...tab, overridePath } : tab));
    set({ tabs });
    persist(get());
  },

  refreshTab: (key) => {
    const counters = get().refreshCounters;
    set({ refreshCounters: { ...counters, [key]: (counters[key] || 0) + 1 } });
  },

  dismissSoftPrompt: () => set({ softPromptVisible: false }),

  clearTabs: () => {
    const tree = useAuthStore.getState().menuTree;
    const home = fixedTab(tree);
    set({ tabs: [home], activeKey: home.key, visitedKeys: [home.key], scrollPositions: {} });
    persist(get());
  },

  restoreFromStorage: (tree, isValidKey) => {
    const homeKey = resolveHomeTabKey(tree);
    const persisted = readPersisted();
    if (!persisted) {
      // 该租户无存储（首次加载或换租户）：复位为本租户的初始态，避免串到上一租户的内存标签（§3.2）
      const home = fixedTab(tree);
      set({
        tabs: [home],
        activeKey: home.key,
        visitedKeys: [home.key],
        scrollPositions: {},
      });
      // 旧存储迁移（§3.5/Q8）：guoyu_admin_selected_menu 的值作为首个激活标签读取，不删除该 key
      try {
        const legacy = localStorage.getItem('guoyu_admin_selected_menu');
        if (legacy && legacy !== homeKey && isValidKey(legacy) && findMenuNode(tree, legacy)) {
          get().openTab(legacy);
        }
      } catch {
        // 忽略迁移异常
      }
      return;
    }
    const valid = persisted.tabs.filter(
      (tab) => tab.key === homeKey || (isValidKey(tab.key) && Boolean(findMenuNode(tree, tab.key))),
    );
    const tabs = ensureFixedFirst(valid.length ? valid : [fixedTab(tree)], tree);
    const activeKey = tabs.some((tab) => tab.key === persisted.activeKey)
      ? persisted.activeKey
      : homeKey;
    set({
      tabs,
      activeKey,
      // D3：仅激活标签入 visitedKeys，其余等首次激活再挂载（§3.3 首次激活才挂载，控制首屏成本）
      visitedKeys: [activeKey],
      scrollPositions: persisted.scrollPositions || {},
    });
  },
}));
