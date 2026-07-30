import { create } from 'zustand';
import type { CurrentUserVO, MenuVO } from '../types/api';
import { isJwtExpired } from '../utils/jwt';
import { resolveTenantId } from '../utils/tenant';

interface AuthState {
  token?: string;
  tenantId?: number;
  currentUser?: CurrentUserVO;
  menus: MenuVO[];
  permissionCodes: string[];
  setToken: (token?: string) => void;
  setTenantId: (tenantId?: number | string) => void;
  setCurrentUser: (user?: CurrentUserVO) => void;
  setMenus: (menus: MenuVO[]) => void;
  setSession: (session: { token: string; tenantId?: number | string; user: CurrentUserVO }) => void;
  hasPermission: (code: string) => boolean;
}

const TOKEN_KEY = 'guoyu_admin_access_token';
const TENANT_KEY = 'guoyu_admin_tenant_id';
const SUPER_ADMIN_ROLE = 'PLATFORM_ADMIN';
const SUPER_ADMIN_USERNAME = 'admin';

function isSuperAdmin(user?: CurrentUserVO): boolean {
  if (!user) {
    return false;
  }
  return user.username === SUPER_ADMIN_USERNAME
    || user.roles?.includes(SUPER_ADMIN_ROLE);
}

function readStoredToken(): string | undefined {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (!raw) {
    return undefined;
  }
  if (isJwtExpired(raw)) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TENANT_KEY);
    return undefined;
  }
  return raw;
}

const initialToken = readStoredToken();

export const useAuthStore = create<AuthState>((set, get) => ({
  token: initialToken,
  tenantId: initialToken ? resolveTenantId(localStorage.getItem(TENANT_KEY)) : undefined,
  currentUser: undefined,
  menus: [],
  permissionCodes: [],
  setToken: (token) => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
    set({ token });
  },
  setTenantId: (tenantId) => {
    const resolved = resolveTenantId(tenantId);
    if (resolved) {
      localStorage.setItem(TENANT_KEY, String(resolved));
    } else {
      localStorage.removeItem(TENANT_KEY);
    }
    set({ tenantId: resolved });
  },
  setCurrentUser: (currentUser) => {
    if (!currentUser) {
      set({ currentUser: undefined, permissionCodes: [] });
      return;
    }
    const tenantId = resolveTenantId(currentUser.tenantId, get().tenantId);
    if (tenantId) {
      localStorage.setItem(TENANT_KEY, String(tenantId));
    }
    set({
      currentUser,
      permissionCodes: currentUser.permissions ?? [],
      tenantId,
    });
  },
  setMenus: (menus) => set({ menus }),
  setSession: ({ token, tenantId, user }) => {
    localStorage.setItem(TOKEN_KEY, token);
    const resolvedTenantId = resolveTenantId(tenantId, user.tenantId);
    if (resolvedTenantId) {
      localStorage.setItem(TENANT_KEY, String(resolvedTenantId));
    } else {
      localStorage.removeItem(TENANT_KEY);
    }
    set({
      token,
      tenantId: resolvedTenantId,
      currentUser: user,
      permissionCodes: user.permissions ?? [],
      menus: [],
    });
  },
  hasPermission: (code: string) => {
    const state = get();
    if (isSuperAdmin(state.currentUser)) {
      return true;
    }
    return state.permissionCodes.includes(code);
  },
}));
