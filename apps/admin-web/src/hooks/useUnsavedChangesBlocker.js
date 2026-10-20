import { jsx as _jsx } from "react/jsx-runtime";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef } from 'react';
import { useTabActiveKey } from '../layouts/TabKeepAlive';
const NavigationBlockerContext = createContext(null);
const DEFAULT_GUARD_SCOPE = 'default';
export function NavigationBlockerProvider({ children }) {
    const guardsRef = useRef(new Map());
    const registerGuard = useCallback((guard, scope = DEFAULT_GUARD_SCOPE) => {
        const guards = guardsRef.current;
        if (guard) {
            guards.set(scope, guard);
        }
        else {
            guards.delete(scope);
        }
        return () => {
            if (guards.get(scope) === guard)
                guards.delete(scope);
        };
    }, []);
    const confirmNavigation = useCallback(async (scope) => {
        if (scope != null) {
            const guard = guardsRef.current.get(scope);
            if (!guard)
                return true;
            return Boolean(await guard());
        }
        for (const guard of guardsRef.current.values()) {
            if (guard && !(await guard()))
                return false;
        }
        return true;
    }, []);
    const value = useMemo(() => ({ confirmNavigation, registerGuard }), [confirmNavigation, registerGuard]);
    useEffect(() => {
        const beforeUnload = (event) => {
            for (const guard of guardsRef.current.values()) {
                if (guard) {
                    event.preventDefault();
                    event.returnValue = '';
                    return;
                }
            }
        };
        window.addEventListener('beforeunload', beforeUnload);
        return () => window.removeEventListener('beforeunload', beforeUnload);
    }, []);
    return _jsx(NavigationBlockerContext.Provider, { value: value, children: children });
}
/**
 * 页面侧入口：registerGuard / confirmNavigation 不显式传 scope 时，
 * 自动落到调用方所在标签的 key（TabActiveContext），保证多标签互不串台。
 */
export function useNavigationBlocker() {
    const context = useContext(NavigationBlockerContext);
    const activeScope = useTabActiveKey();
    if (!context) {
        return {
            confirmNavigation: async (_scope) => true,
            registerGuard: (_guard, _scope) => () => { },
        };
    }
    return {
        confirmNavigation: (scope) => context.confirmNavigation(scope ?? activeScope),
        registerGuard: (guard, scope) => context.registerGuard(guard, scope ?? activeScope),
    };
}
/** 既有页面级 dirty hook：文案入参化（v1.1 D4，去掉硬编码"赛段编排"） */
export function useUnsavedChangesBlocker(dirty, message) {
    const { registerGuard } = useNavigationBlocker();
    useEffect(() => {
        if (!dirty)
            return undefined;
        const guard = () => window.confirm(message || '当前页面有未保存的更改，确定要离开吗？');
        return registerGuard(guard);
    }, [dirty, message, registerGuard]);
}
