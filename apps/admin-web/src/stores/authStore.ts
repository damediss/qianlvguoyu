import { create } from 'zustand';
import type { CurrentUserVO, MenuVO } from '../types/api';

interface AuthState {
  token?: string;
  tenantId?: number;
  currentUser?: CurrentUserVO;
  permissionCodes: string[];
  menuTree: MenuVO[];
  setToken: (token?: string) => void;
  setTenantId: (tenantId?: number) => void;
  setCurrentUser: (user?: CurrentUserVO) => void;
  setMenuTree: (tree: MenuVO[]) => void;
  hasPermission: (code: string) => boolean;
  clearAuth: () => void;
}

const TOKEN_KEY = 'guoyu_admin_access_token';
const TENANT_KEY = 'guoyu_admin_tenant_id';

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem(TOKEN_KEY) || undefined,
  tenantId: Number(localStorage.getItem(TENANT_KEY)) || undefined,
  currentUser: undefined,
  permissionCodes: [],
  menuTree: [],
  setToken: (token) => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
    set({ token });
  },
  setTenantId: (tenantId) => {
    if (tenantId) {
      localStorage.setItem(TENANT_KEY, String(tenantId));
    } else {
      localStorage.removeItem(TENANT_KEY);
    }
    set({ tenantId });
  },
  setCurrentUser: (currentUser) => {
    set({
      currentUser,
      permissionCodes: currentUser?.permissions ?? [],
    });
  },
  setMenuTree: (menuTree) => {
    set({ menuTree });
  },
  hasPermission: (code: string) => {
    return get().permissionCodes.includes(code);
  },
  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TENANT_KEY);
    set({ token: undefined, tenantId: undefined, currentUser: undefined, permissionCodes: [], menuTree: [] });
  },
}));
