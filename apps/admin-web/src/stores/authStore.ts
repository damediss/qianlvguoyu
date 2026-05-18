import { create } from 'zustand';
import type { CurrentUserVO } from '../types/api';

interface AuthState {
  token?: string;
  tenantId?: number;
  currentUser?: CurrentUserVO;
  permissionCodes: string[];
  setToken: (token?: string) => void;
  setTenantId: (tenantId?: number) => void;
  setCurrentUser: (user?: CurrentUserVO) => void;
  hasPermission: (code: string) => boolean;
}

const TOKEN_KEY = 'guoyu_admin_access_token';
const TENANT_KEY = 'guoyu_admin_tenant_id';

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem(TOKEN_KEY) || undefined,
  tenantId: Number(localStorage.getItem(TENANT_KEY)) || undefined,
  currentUser: undefined,
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
  hasPermission: (code: string) => {
    return get().permissionCodes.includes(code);
  },
}));
