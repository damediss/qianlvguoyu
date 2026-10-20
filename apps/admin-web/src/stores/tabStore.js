import { create } from 'zustand';
import { useAuthStore } from './authStore';
import { isHiddenRoutePath } from '../routes/hiddenRoutes';
export const FIXED_TAB_KEY = 'knowledge';
export const FIXED_TAB_TITLE = '管理概览';
/** 软提示阈值（Q10/v1.3）：打开第 11 个标签时提示一次 */
export const SOFT_PROMPT_LIMIT = 10;
const SOFT_PROMPT_SESSION_KEY = 'guoyu_admin_tabs_soft_prompt';
const PERSIST_VERSION = 1;
function tabsStorageKey() {
    const tenantId = useAuthStore.getState().tenantId;
    return `guoyu_admin_tabs:${tenantId ?? 0}`;
}
function readPersisted() {
    try {
        const raw = localStorage.getItem(tabsStorageKey());
        if (!raw)
            return null;
        const parsed = JSON.parse(raw);
        if (!parsed || parsed.version !== PERSIST_VERSION || !Array.isArray(parsed.tabs))
            return null;
        return parsed;
    }
    catch {
        return null;
    }
}
function findMenuNode(tree, code) {
    for (const node of tree) {
        if (node.code === code)
            return node;
        if (node.children?.length) {
            const found = findMenuNode(node.children, code);
            if (found)
                return found;
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
export function normalizeTabKey(key, tree) {
    const direct = findMenuNode(tree, key);
    if (direct)
        return { key, node: direct };
    const snakeKey = key.replace(/-/g, '_');
    const snakeNode = snakeKey !== key ? findMenuNode(tree, snakeKey) : null;
    if (snakeNode)
        return { key: snakeKey, node: snakeNode };
    console.warn('[tabStore] 标签 key 未命中菜单树（含 kebab→snake 归一），按原样打开：', key);
    return { key, node: null };
}
function fixedTab(tree = []) {
    const node = findMenuNode(tree, FIXED_TAB_KEY);
    return {
        key: FIXED_TAB_KEY,
        title: node?.name || FIXED_TAB_TITLE,
        path: node?.path || '',
        closable: false,
    };
}
/** 保证固定标签存在、唯一且在首位 */
function ensureFixedFirst(tabs, tree) {
    const withoutFixed = tabs.filter((tab) => tab.key !== FIXED_TAB_KEY);
    return [fixedTab(tree), ...withoutFixed];
}
/** 软提示同会话（browser session）仅一次 */
function shouldRaiseSoftPrompt(tabCount) {
    if (tabCount <= SOFT_PROMPT_LIMIT)
        return false;
    try {
        if (sessionStorage.getItem(SOFT_PROMPT_SESSION_KEY))
            return false;
        sessionStorage.setItem(SOFT_PROMPT_SESSION_KEY, '1');
    }
    catch {
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
function applyTabUrl(tab) {
    if (typeof window === 'undefined')
        return;
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
function persist(state) {
    try {
        const payload = {
            version: PERSIST_VERSION,
            tabs: state.tabs,
            activeKey: state.activeKey,
            scrollPositions: state.scrollPositions,
        };
        localStorage.setItem(tabsStorageKey(), JSON.stringify(payload));
    }
    catch {
        // 配额满/隐私模式等场景静默降级：标签仍可用，仅不恢复
    }
}
export const useTabStore = create((set, get) => ({
    tabs: [fixedTab()],
    activeKey: FIXED_TAB_KEY,
    visitedKeys: [FIXED_TAB_KEY],
    scrollPositions: {},
    refreshCounters: {},
    softPromptVisible: false,
    openTab: (key, title, path) => {
        const tree = useAuthStore.getState().menuTree;
        const normalized = normalizeTabKey(key, tree);
        const tabKey = normalized.key;
        const existing = get().tabs.find((tab) => tab.key === tabKey);
        if (existing) {
            // 既有标签：菜单改名/补 path 后同步最新值（次要问题①），再激活
            const merged = {
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
        const tab = {
            key: tabKey,
            title: title || node?.name || tabKey,
            path: path ?? node?.path ?? '',
            closable: tabKey !== FIXED_TAB_KEY,
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
            const merged = { ...existing, overridePath, title: title || existing.title };
            set({
                tabs: get().tabs.map((tab) => (tab.key === tabKey ? merged : tab)),
                activeKey: tabKey,
                visitedKeys: get().visitedKeys.includes(tabKey) ? get().visitedKeys : [...get().visitedKeys, tabKey],
            });
            persist(get());
            return;
        }
        const tab = {
            key: tabKey,
            title: title || node?.name || tabKey,
            path: node?.path ?? '',
            closable: tabKey !== FIXED_TAB_KEY,
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
        if (!tab || activeKey === key)
            return;
        set({
            activeKey: key,
            visitedKeys: get().visitedKeys.includes(key) ? get().visitedKeys : [...get().visitedKeys, key],
        });
        applyTabUrl(tab);
        persist(get());
    },
    closeTab: (key) => {
        const { tabs, activeKey, scrollPositions } = get();
        const index = tabs.findIndex((tab) => tab.key === key);
        if (index < 0 || !tabs[index].closable)
            return;
        const next = tabs.filter((tab) => tab.key !== key);
        // 激活位 fallback：左邻 → 右邻 → 固定 tab（§3.2）
        let nextActive = activeKey;
        if (activeKey === key) {
            nextActive = next[index - 1]?.key ?? next[index]?.key ?? FIXED_TAB_KEY;
        }
        const nextScrolls = { ...scrollPositions };
        delete nextScrolls[key];
        set({ tabs: next, activeKey: nextActive, scrollPositions: nextScrolls });
        persist(get());
    },
    closeOthers: (key) => {
        const tree = useAuthStore.getState().menuTree;
        const target = get().tabs.find((tab) => tab.key === key);
        if (!target)
            return;
        const next = [fixedTab(tree), ...get().tabs.filter((tab) => tab.closable && tab.key === key)];
        const nextScrolls = {};
        next.forEach((tab) => {
            if (get().scrollPositions[tab.key] != null)
                nextScrolls[tab.key] = get().scrollPositions[tab.key];
        });
        set({ tabs: next, activeKey: key, scrollPositions: nextScrolls });
        persist(get());
    },
    closeRight: (key) => {
        const index = get().tabs.findIndex((tab) => tab.key === key);
        if (index < 0)
            return;
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
        set({ tabs: [fixedTab(tree)], activeKey: FIXED_TAB_KEY, scrollPositions: {} });
        persist(get());
    },
    reorderTabs: (fromKey, toIndex) => {
        const tabs = [...get().tabs];
        const fromIndex = tabs.findIndex((tab) => tab.key === fromKey);
        if (fromIndex < 0)
            return;
        const clamped = Math.max(1, Math.min(toIndex, tabs.length - 1)); // 固定标签恒在首位
        if (clamped === fromIndex)
            return;
        const [moved] = tabs.splice(fromIndex, 1);
        tabs.splice(clamped, 0, moved);
        set({ tabs });
        persist(get());
    },
    saveScroll: (key, top) => {
        if (!key)
            return;
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
        set({ tabs: [fixedTab(tree)], activeKey: FIXED_TAB_KEY, visitedKeys: [FIXED_TAB_KEY], scrollPositions: {} });
        persist(get());
    },
    restoreFromStorage: (tree, isValidKey) => {
        const persisted = readPersisted();
        if (!persisted) {
            // 该租户无存储（首次加载或换租户）：复位为本租户的初始态，避免串到上一租户的内存标签（§3.2）
            set({
                tabs: [fixedTab(tree)],
                activeKey: FIXED_TAB_KEY,
                visitedKeys: [FIXED_TAB_KEY],
                scrollPositions: {},
            });
            // 旧存储迁移（§3.5/Q8）：guoyu_admin_selected_menu 的值作为首个激活标签读取，不删除该 key
            try {
                const legacy = localStorage.getItem('guoyu_admin_selected_menu');
                if (legacy && legacy !== FIXED_TAB_KEY && isValidKey(legacy) && findMenuNode(tree, legacy)) {
                    get().openTab(legacy);
                }
            }
            catch {
                // 忽略迁移异常
            }
            return;
        }
        const valid = persisted.tabs.filter((tab) => tab.key === FIXED_TAB_KEY || (isValidKey(tab.key) && Boolean(findMenuNode(tree, tab.key))));
        const tabs = ensureFixedFirst(valid.length ? valid : [fixedTab(tree)], tree);
        const activeKey = tabs.some((tab) => tab.key === persisted.activeKey) ? persisted.activeKey : FIXED_TAB_KEY;
        set({
            tabs,
            activeKey,
            // D3：仅激活标签入 visitedKeys，其余等首次激活再挂载（§3.3 首次激活才挂载，控制首屏成本）
            visitedKeys: [activeKey],
            scrollPositions: persisted.scrollPositions || {},
        });
    },
}));
