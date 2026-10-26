import * as Icons from '@ant-design/icons';
import {
  Avatar, Breadcrumb, Dropdown, Layout, Menu, Spin, Tabs, Tooltip, Space, Typography, notification,
} from 'antd';
import type { MenuProps } from 'antd';
import { DndContext, MeasuringStrategy, PointerSensor, closestCenter, useSensor, useSensors } from '@dnd-kit/core';
import type { DragEndEvent } from '@dnd-kit/core';
import { SortableContext, horizontalListSortingStrategy, useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useIsFetching, useQuery } from '@tanstack/react-query';
import { Fragment, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchMenus, logout } from '../api/auth';
import { apiClient } from '../api/client';
import { isTabsFeatureEnabled } from '../config/tabsFeature';
import { useNavigationBlocker } from '../hooks/useUnsavedChangesBlocker';
import { findMenuCodeByPath } from '../routes/competitionRoute';
import { isHiddenRoutePath } from '../routes/hiddenRoutes';
import { useAuthStore } from '../stores/authStore';
import { useNavStore } from '../stores/navStore';
import { FIXED_TAB_TITLE, SOFT_PROMPT_LIMIT, useTabStore } from '../stores/tabStore';
import type { TabItem } from '../stores/tabStore';
import type { MenuVO } from '../types/api';
import { SidebarToggleIcon } from '../components/SidebarToggleIcon';
import { iconFromName } from '../utils/icons';
import { TabActiveProvider, TabPaneActiveProvider } from './TabKeepAlive';

const { Header, Sider, Content } = Layout;

const SIDEBAR_COLLAPSED_KEY = 'guoyu_admin_sidebar_collapsed';

type AppMenuItem = {
  key: string;
  icon?: ReactNode;
  label: ReactNode;
  title: string;
  disabled?: boolean;
  children?: AppMenuItem[];
};

// 按业务要求下线的独立菜单入口：只从侧栏/标签入口移除，页面与后端接口保留（URL 仍可直达）。
const hiddenMenuCodes = new Set([
  'competition_theory_exam',
  'competition_theory_blueprint',
  'competition_theory_question',
  'competition_practical_blueprint',
  'competition_practical_question',
  'competition_theory_make_exam_plan',
  'competition_practical_make_exam_plan',
  'competition_practical_exam',
]);

// 默认折叠的目录（含子目录）：不按后端 expanded 配置自动展开，由用户点击展开。
const defaultCollapsedMenuCodes = new Set(['competition_foundation']);

/**
 * 剔除下线菜单：命中 hiddenMenuCodes 的节点连同子节点整棵移除；
 * 目录的子入口全部被移除时，目录本身也不再保留，避免侧栏出现空目录或禁用项。
 */
function filterHiddenMenuNodes(tree: MenuVO[]): MenuVO[] {
  return tree.flatMap((node) => {
    if (hiddenMenuCodes.has(node.code)) return [];
    if (!node.children?.length) return [node];
    const children = filterHiddenMenuNodes(node.children);
    return children.length ? [{ ...node, children }] : [];
  });
}

function menuDisplayName(menu: MenuVO) {
  // Keep the renamed competition entry visible while an existing session still
  // has the previous backend menu payload cached.
  if (menu.code === 'competition_event') return '赛事创建';
  if (menu.code === 'competition_simple_practical_question') return '实操评分项';
  return menu.name;
}

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
      const name = menuDisplayName(m);
      const item: AppMenuItem = {
        key: m.code,
        icon: iconFromName(m.icon),
        label: <TruncatedLabel text={name} />,
        title: name,
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

function safeScrollToTop(el: HTMLElement | null) {
  if (!el || typeof el.scrollTo !== 'function') return;
  el.scrollTo({ top: 0 });
}

/**
 * 可拖拽标签（T8 拖拽排序）：dnd-kit sortable 挂在标签内容上，
 * PointerSensor distance=6 与点击切换互不干扰；固定标签禁用拖拽。
 *
 * 关键：sortable 的位移/过渡必须套到 antd 渲染的 `.ant-tabs-tab` 卡片外壳上——
 * 只 transform 文字 span 时卡片纹丝不动，拖拽过程零视觉反馈（评审后用户反馈）。
 * 测量节点仍是文字 span（同排等高，delta 一致），样式通过 closest 同步到卡片。
 */
function DraggableTabLabel({
  tab,
  isActive,
  refreshing,
  onClose,
  onBeginSwitch,
}: {
  tab: TabItem;
  isActive: boolean;
  refreshing: boolean;
  onClose: (key: string) => void;
  onBeginSwitch: () => void;
}) {
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: tab.key,
    disabled: !tab.closable,
  });
  const labelRef = useRef<HTMLSpanElement | null>(null);
  const tabCardRef = useRef<HTMLElement | null>(null);

  // 把 sortable 的 transform/transition/拖拽态同步到 .ant-tabs-tab 卡片外壳
  useEffect(() => {
    let card = tabCardRef.current;
    // antd 重渲染可能重建 tab 节点：失联时按 closest 重新解析
    if (!card || !card.isConnected) {
      card = (labelRef.current?.closest('.ant-tabs-tab') as HTMLElement | null) ?? null;
      tabCardRef.current = card;
    }
    if (!card) return;
    card.style.transform = CSS.Translate.toString(transform) || '';
    card.style.transition = transition || '';
    if (isDragging) {
      card.classList.add('app-tab-dragging');
    } else {
      card.classList.remove('app-tab-dragging');
    }
  }, [transform, transition, isDragging]);

  // 卸载时清理卡片上的内联样式，避免残留位移
  useEffect(() => () => {
    const card = tabCardRef.current;
    if (card) {
      card.style.transform = '';
      card.style.transition = '';
      card.classList.remove('app-tab-dragging');
    }
  }, []);

  return (
    <span
      ref={(node) => {
        labelRef.current = node;
        setNodeRef(node);
      }}
      className="app-tab-label"
      {...attributes}
      onAuxClick={(event) => {
        // 中键关闭（Q7）：onAuxClick button===1；mousedown 已 preventDefault 抑制自动滚动
        if (event.button === 1 && tab.closable) {
          event.preventDefault();
          onClose(tab.key);
        }
      }}
      onMouseDown={(event) => {
        if (event.button === 1) event.preventDefault();
      }}
    >
      <span
        ref={setActivatorNodeRef}
        {...listeners}
        className="app-tab-label-text"
        style={{ display: 'inline-flex', alignItems: 'center', gap: 6, cursor: tab.closable ? 'grab' : undefined }}
      >
        <Dropdown
          trigger={['contextMenu']}
          menu={{
            items: [
              { key: 'refresh', label: '刷新标签页', onClick: () => useTabStore.getState().refreshTab(tab.key) },
              { key: 'close', label: '关闭标签页', disabled: !tab.closable, onClick: () => onClose(tab.key) },
              { key: 'closeOthers', label: '关闭其他标签页', disabled: !tab.closable, onClick: () => { onBeginSwitch(); useTabStore.getState().closeOthers(tab.key); } },
              { key: 'closeRight', label: '关闭右侧标签页', onClick: () => { onBeginSwitch(); useTabStore.getState().closeRight(tab.key); } },
              { key: 'closeAll', label: '全部关闭', onClick: () => { onBeginSwitch(); useTabStore.getState().closeAll(); } },
            ],
          }}
        >
          <span>{tab.title}</span>
        </Dropdown>
        {isActive && (
          <Tooltip title="刷新标签页">
            <Icons.ReloadOutlined
              className={`app-tab-refresh${refreshing ? ' app-tab-refresh-spinning' : ''}`}
              onClick={(event) => {
                event.stopPropagation();
                useTabStore.getState().refreshTab(tab.key);
              }}
            />
          </Tooltip>
        )}
      </span>
    </span>
  );
}

/** 一次页面加载只做一次恢复（跨隐藏路由 shell 的 AppShell 重挂载不重复执行） */
let tabsRestoredForSession = false;

/** 测试/灰度重开场景复位恢复标志（正常页面生命周期内无需调用） */
export function resetTabsSessionRestore() {
  tabsRestoredForSession = false;
}

interface AppShellProps {
  pageMap: Record<string, ReactNode>;
  contentOverride?: ReactNode;
  selectedMenuKeyOverride?: string;
  onMenuNavigate?: (node: MenuVO | null) => void;
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
  // 侧栏折叠：单组件自用状态（AGENTS.md 规则 2 修订），localStorage 持久化，刷新后恢复
  const [sidebarCollapsed, setSidebarCollapsed] = useState(
    () => localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1',
  );
  const toggleSidebar = useCallback(() => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, next ? '1' : '0');
      return next;
    });
    // 等 Sider 0.2s 过渡结束后广播 resize，驱动 ECharts 等监听 window resize 的组件重排
    window.setTimeout(() => window.dispatchEvent(new Event('resize')), 250);
  }, []);
  const tabsEnabled = isTabsFeatureEnabled();
  const tabs = useTabStore((s) => s.tabs);
  const activeKey = useTabStore((s) => s.activeKey);
  const visitedKeys = useTabStore((s) => s.visitedKeys);
  const refreshCounters = useTabStore((s) => s.refreshCounters);
  const softPromptVisible = useTabStore((s) => s.softPromptVisible);
  const activateTab = useTabStore((s) => s.activateTab);
  const dismissSoftPrompt = useTabStore((s) => s.dismissSoftPrompt);
  const { confirmNavigation } = useNavigationBlocker();
  const contentOverrideActive = Boolean(contentOverride);
  // §3.7 优先级链：覆盖态取 shell 指定的菜单码，普通态取 tabStore.activeKey
  const effectiveActiveKey = contentOverrideActive && selectedMenuKeyOverride
    ? selectedMenuKeyOverride
    : activeKey;
  const legacyActiveKey = selectedMenuKeyOverride || selectedKey;
  const visibleMenuTree = useMemo(() => filterHiddenMenuNodes(menuTree), [menuTree]);
  const prevEffectiveKeyRef = useRef<string | null>(null);
  const effectiveKeyRef = useRef(effectiveActiveKey);
  const userNavRef = useRef(false);
  // 用户是否从侧栏点过菜单：进入系统时的标签恢复/深链也会改变 activeKey，
  // 但不应据此展开侧栏目录（默认折叠），只有用户点过菜单后才恢复祖先自动展开。
  const userMenuNavRef = useRef(false);
  const appliedDefaultExpandedRef = useRef(false);
  const contentRef = useRef<HTMLElement>(null);

  useEffect(() => {
    effectiveKeyRef.current = effectiveActiveKey;
  }, [effectiveActiveKey]);

  // §3.2 租户切换：清空上一租户的标签并允许重新恢复新租户的存储
  const tenantId = useAuthStore((s) => s.tenantId);
  const prevTenantRef = useRef(tenantId);
  useEffect(() => {
    if (prevTenantRef.current !== undefined && tenantId !== prevTenantRef.current) {
      useTabStore.getState().clearTabs();
      tabsRestoredForSession = false;
    }
    prevTenantRef.current = tenantId;
  }, [tenantId]);

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

  // ---- 标签页：恢复 + 深链直达（§3.5 / §3.4，一次页面加载一次） ----
  useEffect(() => {
    if (!tabsEnabled || tabsRestoredForSession || !menuTree.length) return;
    tabsRestoredForSession = true;
    const store = useTabStore.getState();
    // 次要问题②：用 visibleMenuTree（与菜单渲染同口径，剔除 hiddenMenuCodes），被隐藏的菜单码不恢复成标签
    store.restoreFromStorage(visibleMenuTree, (key) => Boolean(pageMap[key]));
    // 深链直达：URL 解析到菜单码且未命中隐藏路由 → 优先于存储的 activeKey（§3.4）
    const deepLinkCode = findMenuCodeByPath(menuTree, window.location.pathname);
    if (
      deepLinkCode
      && pageMap[deepLinkCode]
      && !isHiddenRoutePath(window.location.pathname)
      && deepLinkCode !== useTabStore.getState().activeKey
    ) {
      store.openTab(deepLinkCode);
    }
  }, [tabsEnabled, menuTree, pageMap]);

  // ---- 标签页：监听浏览器前进/后退（popstate → 反查开标签；§3.4 反向） ----
  const [browserPathname, setBrowserPathname] = useState(() => window.location.pathname);
  useEffect(() => {
    const syncPathname = () => setBrowserPathname(window.location.pathname);
    window.addEventListener('popstate', syncPathname);
    return () => window.removeEventListener('popstate', syncPathname);
  }, []);
  useEffect(() => {
    if (!tabsEnabled || !menuTree.length) return;
    // 命中隐藏路由：tabStore 不介入，交给既有 shell 的覆盖机制（§3.4 反向 1）
    if (isHiddenRoutePath(browserPathname) || browserPathname === '/') return;
    const code = findMenuCodeByPath(menuTree, browserPathname);
    if (!code || !pageMap[code]) return; // 反向 3：找不到 → 保持现状
    const state = useTabStore.getState();
    if (state.tabs.some((tab) => tab.key === code)) {
      if (state.activeKey !== code) {
        state.saveScroll(effectiveKeyRef.current, contentRef.current?.scrollTop ?? 0);
        state.activateTab(code); // pathname 已等于该 path，activateTab 不会重复 pushState
      }
    } else {
      state.openTab(code); // 反向 2：未开 → 开新标签
    }
  }, [tabsEnabled, browserPathname, menuTree, pageMap]);

  // ---- 标签页：滚动位置持续采集（§3.3，覆盖 navStore 委托跳转等全部切换路径） ----
  useEffect(() => {
    const el = contentRef.current;
    if (!el || !tabsEnabled) return;
    let raf = 0;
    const onScroll = () => {
      if (raf) return;
      raf = window.requestAnimationFrame(() => {
        raf = 0;
        useTabStore.getState().saveScroll(effectiveKeyRef.current, el.scrollTop);
      });
    };
    el.addEventListener('scroll', onScroll);
    return () => {
      el.removeEventListener('scroll', onScroll);
      if (raf) window.cancelAnimationFrame(raf);
    };
  }, [tabsEnabled]);

  // ---- 标签页：激活后恢复滚动位置（§3.3 时序：双 rAF + 最多 3 次重试，失败不回写 0） ----
  useEffect(() => {
    if (!tabsEnabled) return;
    const el = contentRef.current;
    if (!el) return;
    const pos = useTabStore.getState().scrollPositions[effectiveActiveKey];
    if (pos == null) {
      // 首次打开的标签（无记录）→ 回顶
      safeScrollToTop(el);
      return;
    }
    let attempts = 0;
    let rafA = 0;
    let rafB = 0;
    const tryRestore = () => {
      if (el.scrollHeight >= pos + el.clientHeight) {
        if (typeof el.scrollTo === 'function') el.scrollTo({ top: pos });
        return;
      }
      if (attempts < 3) {
        attempts += 1;
        rafA = window.requestAnimationFrame(() => {
          rafB = window.requestAnimationFrame(tryRestore);
        });
      }
      // 放弃时不写回 0，保留原值供下次切回重试
    };
    rafA = window.requestAnimationFrame(() => {
      rafB = window.requestAnimationFrame(tryRestore);
    });
    return () => {
      window.cancelAnimationFrame(rafA);
      window.cancelAnimationFrame(rafB);
    };
  }, [tabsEnabled, effectiveActiveKey]);

  // ---- 标签页：刷新反馈（§3.9 补强）----
  // 重挂载由 refreshCounters 进入面板 key 完成（渲染层，见下方 pane 渲染）；数据新鲜度
  // 由 react-query refetchOnMount（staleTime=0）保证。这里补可见反馈：面板加载遮罩 +
  // 滚回顶部 + ⟳ 旋转——否则重挂载后缓存瞬时回填，数据没变化时用户看到"点了没反应"。
  const isFetching = useIsFetching();
  const [refreshingPane, setRefreshingPane] = useState<string | null>(null);
  const prevRefreshCountersRef = useRef(refreshCounters);
  useEffect(() => {
    const prev = prevRefreshCountersRef.current;
    prevRefreshCountersRef.current = refreshCounters;
    const changedKey = Object.keys(refreshCounters)
      .find((key) => refreshCounters[key] !== (prev[key] || 0));
    if (!changedKey) return;
    setRefreshingPane(changedKey);
    if (changedKey === effectiveKeyRef.current) {
      safeScrollToTop(contentRef.current);
    }
  }, [refreshCounters]);
  // 遮罩收口：全站请求平息后再撤（最少可见 350ms 防闪烁）；8s 硬上限兜底，
  // 防止隐藏标签的长查询把遮罩挂死
  useEffect(() => {
    if (!refreshingPane) return;
    if (isFetching > 0) return;
    const timer = window.setTimeout(() => setRefreshingPane(null), 350);
    return () => window.clearTimeout(timer);
  }, [refreshingPane, isFetching]);
  useEffect(() => {
    if (!refreshingPane) return;
    const cap = window.setTimeout(() => setRefreshingPane(null), 8000);
    return () => window.clearTimeout(cap);
  }, [refreshingPane]);

  // ---- 标签页：document.title 联动 ----
  useEffect(() => {
    if (!tabsEnabled) return;
    const active = tabs.find((tab) => tab.key === effectiveActiveKey);
    document.title = `${active?.title || FIXED_TAB_TITLE} - 千铝国昱云平台`;
  }, [tabsEnabled, effectiveActiveKey, tabs]);

  // ---- 标签页：>10 软提示（Q10/v1.3，非阻塞，同会话仅一次） ----
  useEffect(() => {
    if (!softPromptVisible) return;
    notification.warning({
      message: '标签页较多',
      description: `当前已打开超过 ${SOFT_PROMPT_LIMIT} 个标签页，可能影响性能，建议关闭不使用的标签页。`,
      placement: 'bottomRight',
      duration: 4.5,
    });
    dismissSoftPrompt();
  }, [softPromptVisible, dismissSoftPrompt]);

  // 进入系统时不按当前选中页强制展开祖先（认定管理等保持折叠）；
  // 仅在用户切换菜单后，才自动展开选中页的祖先路径。
  // 标签页恢复与深链在进入时同样会改变 activeKey —— 那是"进入系统"，不是用户切换菜单，
  // 不能拿它展开侧栏，否则竞赛管理等目录会随上次停留的页面默认展开。
  useEffect(() => {
    if (!menuTree.length || !effectiveActiveKey) return;
    const prevKey = prevEffectiveKeyRef.current;
    prevEffectiveKeyRef.current = effectiveActiveKey;
    if (prevKey === null || prevKey === effectiveActiveKey) {
      return;
    }
    if (!userMenuNavRef.current) {
      return;
    }
    const ancestors = findAncestorCodes(visibleMenuTree, effectiveActiveKey);
    if (ancestors && ancestors.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...ancestors])]);
    }
  }, [effectiveActiveKey, visibleMenuTree]);

  // 根据菜单配置的 expanded 字段初始化默认展开的目录（仅首次）。
  // 竞赛管理整组默认折叠：其自身及子目录不沿用后端 expanded=1 自动展开，
  // 避免恢复标签进入时竞赛菜单被整棵树撑开；后端菜单配置保持不变。
  useEffect(() => {
    if (!visibleMenuTree.length || appliedDefaultExpandedRef.current) return;
    const collectExpanded = (
      nodes: MenuVO[],
      insideDefaultCollapsed = false,
    ): string[] => nodes.reduce((keys: string[], node) => {
      const defaultCollapsed = insideDefaultCollapsed || defaultCollapsedMenuCodes.has(node.code);
      if (!defaultCollapsed && node.menuType === 'directory' && node.expanded === 1) {
        keys.push(node.code);
      }
      if (node.children?.length) {
        keys.push(...collectExpanded(node.children, defaultCollapsed));
      }
      return keys;
    }, []);
    const expandedKeys = collectExpanded(visibleMenuTree);
    appliedDefaultExpandedRef.current = true;
    if (expandedKeys.length > 0) {
      setOpenKeys((prev) => [...new Set([...prev, ...expandedKeys])]);
    }
  }, [visibleMenuTree]);

  // 目录自动下钻（§3.5 加守卫）：仅"用户点击链路"写入的 key 才允许重定向，
  // restore/深链写入的 key 即便命中空目录也只做高亮，不自动拉起子页标签。
  useEffect(() => {
    if (!menuTree.length || !selectedKey) return;
    if (!userNavRef.current) return;
    userNavRef.current = false;
    const node = findMenuNode(visibleMenuTree, selectedKey);
    if (!node || pageMap[node.code] || node.menuType === 'external') return;
    const target = findFirstNavigableNode(node.children || [], pageMap);
    if (target && target.code !== selectedKey && target.menuType !== 'external') {
      setSelectedKey(target.code);
    }
  }, [visibleMenuTree, pageMap, selectedKey, setSelectedKey]);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 忽略后端错误，前端强制清除登录态
    } finally {
      // §3.2 清退时机：仅显式登出清标签；401 被动失效不清（见 useAuthStore.clearAuth 不挂 clearTabs）
      useTabStore.getState().clearTabs();
      clearAuth();
    }
  };

  const handleOpenAssessor = async () => {
    try {
      await logout();
    } catch {
      // 切换入口时仍强制清理旧登录态，避免管理账号误入平板端
    } finally {
      useTabStore.getState().clearTabs();
      clearAuth();
      window.location.assign('/assessor-scoring');
    }
  };

  const handleOpenDefense = async () => {
    try {
      await logout();
    } catch {
      // 切换入口时仍强制清理旧登录态，避免管理账号误入综合答辩端
    } finally {
      useTabStore.getState().clearTabs();
      clearAuth();
      window.location.assign('/assessor-defense');
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
      key: 'defense',
      icon: <Icons.MobileOutlined />,
      label: '考评员综合答辩端',
      onClick: handleOpenDefense,
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
      if (tabsEnabled) {
        // §3.5 下钻守卫：仅用户点击链路允许目录重定向；切标签前保存滚动位置（§3.3）
        userNavRef.current = true;
        useTabStore.getState().saveScroll(effectiveKeyRef.current, contentRef.current?.scrollTop ?? 0);
      }
      setSelectedKey(target.code);
    }
  }, [openExternalMenu, pageMap, setSelectedKey, tabsEnabled]);

  const handleMenuNavigate = useCallback((node: MenuVO | null) => {
    // 只有侧栏点击链路才允许展开选中页祖先（区别于标签恢复/深链）
    userMenuNavRef.current = true;
    onMenuNavigate?.(node);
    navigateToNode(node);
  }, [navigateToNode, onMenuNavigate]);

  // ---- 标签页：关闭（§3.8 守卫：关哪个标签问哪个页面） ----
  const handleTabClose = useCallback(async (key: string) => {
    const tab = useTabStore.getState().tabs.find((item) => item.key === key);
    if (!tab?.closable) return;
    if (!(await confirmNavigation(key))) return;
    useTabStore.getState().saveScroll(effectiveKeyRef.current, contentRef.current?.scrollTop ?? 0);
    useTabStore.getState().closeTab(key);
  }, [confirmNavigation]);

  const beginTabSwitch = useCallback(() => {
    useTabStore.getState().saveScroll(effectiveKeyRef.current, contentRef.current?.scrollTop ?? 0);
  }, []);

  const menuItems = useMemo(
    () => buildMenuItems(visibleMenuTree, pageMap),
    [visibleMenuTree, pageMap],
  );

  const breadcrumbItems = useMemo(() => {
    const path = findMenuPath(visibleMenuTree, effectiveActiveKey) || [];
    return path.map((node) => ({ title: menuDisplayName(node) }));
  }, [effectiveActiveKey, visibleMenuTree]);

  // ---- 标签页：TabBar 标签渲染（可拖拽 + 右键菜单 + 中键关闭 + 激活态刷新按钮） ----
  const tabBarItems = useMemo(
    () => tabs.map((tab) => ({
      key: tab.key,
      closable: tab.closable,
      label: (
        <DraggableTabLabel
          tab={tab}
          isActive={tab.key === effectiveActiveKey}
          refreshing={refreshingPane === tab.key}
          onClose={(key) => { void handleTabClose(key); }}
          onBeginSwitch={beginTabSwitch}
        />
      ),
    })),
    [tabs, effectiveActiveKey, refreshingPane, handleTabClose, beginTabSwitch],
  );

  // 拖拽排序（T8）：PointerSensor distance=6 与点击切换互不干扰；
  // dragging 态驱动全局 grabbing 光标（拖拽视觉反馈，v1.4 用户反馈补强）
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));
  const [tabDragActive, setTabDragActive] = useState(false);
  const handleDragEnd = useCallback((event: DragEndEvent) => {
    setTabDragActive(false);
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const toIndex = useTabStore.getState().tabs.findIndex((tab) => tab.key === over.id);
    if (toIndex < 0) return;
    beginTabSwitch();
    useTabStore.getState().reorderTabs(String(active.id), toIndex);
  }, [beginTabSwitch]);

  const menuHighlightKey = tabsEnabled ? effectiveActiveKey : legacyActiveKey;

  return (
    <Layout className="app-shell">
      <Sider width={270} collapsedWidth={72} collapsed={sidebarCollapsed} trigger={null} className="app-sider">
        <div className={`brand-block${sidebarCollapsed ? ' brand-block-collapsed' : ''}`}>
          <div className="brand-mark">国</div>
          {!sidebarCollapsed && (
            <div className="brand-texts">
              <Typography.Text className="brand-name">千铝国昱云平台</Typography.Text>
              <Typography.Text className="brand-sub">培训学习考试闭环</Typography.Text>
            </div>
          )}
          <Tooltip title={sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'} placement="right">
            <button
              type="button"
              className="sider-toggle-btn"
              aria-label={sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'}
              onClick={toggleSidebar}
            >
              <SidebarToggleIcon collapsed={sidebarCollapsed} />
            </button>
          </Tooltip>
        </div>
        <Menu
          className="app-menu"
          theme="light"
          mode="inline"
          selectedKeys={[menuHighlightKey]}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          onClick={({ key }) => {
            handleMenuNavigate(findMenuNode(visibleMenuTree, String(key)));
          }}
          items={menuItems as MenuProps['items']}
        />
      </Sider>
      <Layout className="app-main">
        {/* v1.4：Chrome 同款单行头部——标签条（左，弹性）+ 平台管理员下拉（右，固定）。
            搜索栏已删（Q1-A：原组件无交互逻辑纯占位）。灰度 off 时头部仅保留用户下拉。 */}
        <Header className={tabsEnabled ? 'app-header app-header-tabs' : 'app-header app-header-plain'}>
          {tabsEnabled && (
            <div className={`app-tabbar-strip${tabDragActive ? ' app-tabbar-dragging' : ''}`}>
              <DndContext
                sensors={sensors}
                collisionDetection={closestCenter}
                measuring={{ droppable: { strategy: MeasuringStrategy.Always } }}
                onDragStart={() => setTabDragActive(true)}
                onDragEnd={handleDragEnd}
                onDragCancel={() => setTabDragActive(false)}
              >
                <SortableContext items={tabs.map((tab) => tab.key)} strategy={horizontalListSortingStrategy}>
                  <Tabs
                    className="app-tabbar-tabs"
                    type="editable-card"
                    hideAdd
                    activeKey={effectiveActiveKey}
                    onChange={(key) => {
                      if (key === effectiveActiveKey) return;
                      beginTabSwitch();
                      activateTab(key);
                    }}
                    onEdit={(targetKey, action) => {
                      if (action === 'remove') {
                        void handleTabClose(String(targetKey));
                      }
                    }}
                    items={tabBarItems}
                  />
                </SortableContext>
              </DndContext>
            </div>
          )}
          <Space size={12} className="app-header-user">
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
        <Content className="app-content" ref={contentRef}>
          <TabActiveProvider value={menuHighlightKey}>
            {!hideBreadcrumb && breadcrumbItems.length > 0 && (
              <Breadcrumb className="app-breadcrumb" items={breadcrumbItems} />
            )}
            <div className="app-page-body">
              <Suspense fallback={<Spin size="large" style={{ display: 'block', margin: '120px auto' }} />}>
              {tabsEnabled ? (
                contentOverride ? (
                  // 覆盖态标签（隐藏路由）同样支持"刷新标签页"：按刷新计数重挂载覆盖内容
                  <div className="app-tab-pane" data-tab-key={effectiveActiveKey}>
                    {refreshingPane === effectiveActiveKey && (
                      <div className="app-tab-pane-refreshing" aria-hidden="true"><Spin /></div>
                    )}
                    <Fragment key={refreshCounters[effectiveActiveKey] || 0}>{contentOverride}</Fragment>
                  </div>
                ) : tabs.map((tab) => {
                  if (!visitedKeys.includes(tab.key)) return null; // 首次激活才挂载（forceRender 不开）
                  const paneActive = tab.key === effectiveActiveKey && !contentOverrideActive;
                  return (
                    <TabPaneActiveProvider key={tab.key} active={paneActive}>
                      <div
                        className="app-tab-pane"
                        data-tab-key={tab.key}
                        style={{ display: paneActive ? undefined : 'none' }}
                      >
                        {refreshingPane === tab.key && (
                          <div className="app-tab-pane-refreshing" aria-hidden="true"><Spin /></div>
                        )}
                        <div key={`${tab.key}:${refreshCounters[tab.key] || 0}`}>{pageMap[tab.key]}</div>
                      </div>
                    </TabPaneActiveProvider>
                  );
                })
              ) : (
                contentOverride || pageMap[selectedKey] || pageMap['knowledge']
              )}
              </Suspense>
            </div>
          </TabActiveProvider>
        </Content>
      </Layout>
    </Layout>
  );
}
