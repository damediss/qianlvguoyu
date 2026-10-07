import { createContext, useCallback, useContext, useEffect, useMemo, useRef, type ReactNode } from 'react';
import { useTabActiveKey } from '../layouts/TabKeepAlive';

/**
 * 未保存拦截注册表（《admin-web标签页外壳改造方案》§3.8 / Q6）。
 * v1.1 前为单槽 guardRef（覆盖写、多标签串台）；现改为 Map<scope, Guard>，
 * scope = 页面所在标签 key（由 TabActiveContext 提供的 effectiveActiveKey），
 * 从而"关哪个标签问哪个页面"。全保活下这是硬前置。
 */
type Guard = (() => boolean | Promise<boolean>) | null;

interface NavigationBlockerContextValue {
  /** scope 省略时 = 任一已注册 guard 拦截（对齐 beforeunload 语义） */
  confirmNavigation: (scope?: string) => Promise<boolean>;
  registerGuard: (guard: Guard, scope?: string) => () => void;
}

const NavigationBlockerContext = createContext<NavigationBlockerContextValue | null>(null);

const DEFAULT_GUARD_SCOPE = 'default';

export function NavigationBlockerProvider({ children }: { children: ReactNode }) {
  const guardsRef = useRef<Map<string, Guard>>(new Map());
  const registerGuard = useCallback((guard: Guard, scope: string = DEFAULT_GUARD_SCOPE) => {
    const guards = guardsRef.current;
    if (guard) {
      guards.set(scope, guard);
    } else {
      guards.delete(scope);
    }
    return () => {
      if (guards.get(scope) === guard) guards.delete(scope);
    };
  }, []);
  const confirmNavigation = useCallback(async (scope?: string) => {
    if (scope != null) {
      const guard = guardsRef.current.get(scope);
      if (!guard) return true;
      return Boolean(await guard());
    }
    for (const guard of guardsRef.current.values()) {
      if (guard && !(await guard())) return false;
    }
    return true;
  }, []);
  const value = useMemo(() => ({ confirmNavigation, registerGuard }), [confirmNavigation, registerGuard]);

  useEffect(() => {
    const beforeUnload = (event: BeforeUnloadEvent) => {
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

  return <NavigationBlockerContext.Provider value={value}>{children}</NavigationBlockerContext.Provider>;
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
      confirmNavigation: async (_scope?: string) => true,
      registerGuard: (_guard: Guard, _scope?: string) => () => {},
    };
  }
  return {
    confirmNavigation: (scope?: string) => context.confirmNavigation(scope ?? activeScope),
    registerGuard: (guard: Guard, scope?: string) => context.registerGuard(guard, scope ?? activeScope),
  };
}

/** 既有页面级 dirty hook：文案入参化（v1.1 D4，去掉硬编码"赛段编排"） */
export function useUnsavedChangesBlocker(dirty: boolean, message?: string) {
  const { registerGuard } = useNavigationBlocker();
  useEffect(() => {
    if (!dirty) return undefined;
    const guard = () => window.confirm(message || '当前页面有未保存的更改，确定要离开吗？');
    return registerGuard(guard);
  }, [dirty, message, registerGuard]);
}
