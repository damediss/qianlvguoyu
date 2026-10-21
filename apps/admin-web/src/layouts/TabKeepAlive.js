import { jsx as _jsx } from "react/jsx-runtime";
import { createContext, useContext, useRef } from 'react';
import { FIXED_TAB_KEY } from '../stores/tabStore';
/**
 * 标签保活上下文（《admin-web标签页外壳改造方案》§3.6）。
 * 两层 Provider 均由 AppShell 挂载：
 * - TabActiveProvider：外壳级，值 = effectiveActiveKey（§3.7 优先级链），
 *   隐藏路由 shell 传入 selectedMenuKeyOverride，普通态传 tabStore.activeKey。
 * - TabPaneActiveProvider：面板级，包住每个保活面板，值 = 该面板是否可见。
 *
 * 页面接入约定（AGENTS.md）：轮询一律 `const active = usePaneActive()` 后
 * `refetchInterval: active ? N : false`；禁止裸写 setInterval 轮询。
 */
const TabActiveContext = createContext(FIXED_TAB_KEY);
const PaneActiveContext = createContext(true);
export function TabActiveProvider({ value, children }) {
    return _jsx(TabActiveContext.Provider, { value: value, children: children });
}
export function TabPaneActiveProvider({ active, children }) {
    return _jsx(PaneActiveContext.Provider, { value: active, children: children });
}
/** 当前生效激活标签 key（AppShell/隐藏路由 shell 注入的 effectiveActiveKey） */
export function useTabActiveKey() {
    return useContext(TabActiveContext);
}
/** 指定标签是否为当前激活标签（scope 校验用，§3.8） */
export function useIsTabActive(tabKey) {
    return useContext(TabActiveContext) === tabKey;
}
/** 本面板是否可见（保活面板在隐藏标签内为 false）——轮询/定时器门禁用 */
export function usePaneActive() {
    return useContext(PaneActiveContext);
}
/**
 * paneActive 的 ref 镜像：长生命周期 setInterval 闭包读 ref，避免捕获过期渲染的布尔值
 * （§3.6 乙组：计时继续、跳过 setState 的实现基础）
 */
export function usePaneActiveRef(active) {
    const ref = useRef(active);
    ref.current = active;
    return ref;
}
