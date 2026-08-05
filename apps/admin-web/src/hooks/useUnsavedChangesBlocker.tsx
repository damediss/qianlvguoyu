import { createContext, useCallback, useContext, useEffect, useMemo, useRef, type ReactNode } from 'react';

type Guard = (() => boolean | Promise<boolean>) | null;

interface NavigationBlockerContextValue {
  confirmNavigation: () => Promise<boolean>;
  registerGuard: (guard: Guard) => () => void;
}

const NavigationBlockerContext = createContext<NavigationBlockerContextValue | null>(null);

export function NavigationBlockerProvider({ children }: { children: ReactNode }) {
  const guardRef = useRef<Guard>(null);
  const registerGuard = useCallback((guard: Guard) => {
    guardRef.current = guard;
    return () => {
      if (guardRef.current === guard) guardRef.current = null;
    };
  }, []);
  const confirmNavigation = useCallback(async () => {
    const guard = guardRef.current;
    if (!guard) return true;
    return Boolean(await guard());
  }, []);
  const value = useMemo(() => ({ confirmNavigation, registerGuard }), [confirmNavigation, registerGuard]);

  useEffect(() => {
    const beforeUnload = (event: BeforeUnloadEvent) => {
      if (!guardRef.current) return;
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', beforeUnload);
    return () => window.removeEventListener('beforeunload', beforeUnload);
  }, []);

  return <NavigationBlockerContext.Provider value={value}>{children}</NavigationBlockerContext.Provider>;
}

export function useNavigationBlocker() {
  const context = useContext(NavigationBlockerContext);
  if (!context) {
    return { confirmNavigation: async () => true, registerGuard: () => () => {} };
  }
  return context;
}

export function useUnsavedChangesBlocker(dirty: boolean) {
  const { registerGuard } = useNavigationBlocker();
  useEffect(() => {
    if (!dirty) return undefined;
    const guard = () => window.confirm('当前页面有未保存的赛段编排，确定要离开吗？');
    return registerGuard(guard);
  }, [dirty, registerGuard]);
}
