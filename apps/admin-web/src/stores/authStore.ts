import { create } from 'zustand';
import type { CurrentUserVO } from '../types/api';

interface AuthState {
  token?: string;
  currentUser?: CurrentUserVO;
  setToken: (token?: string) => void;
  setCurrentUser: (user?: CurrentUserVO) => void;
}

const TOKEN_KEY = 'guoyu_admin_access_token';

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem(TOKEN_KEY) || undefined,
  currentUser: undefined,
  setToken: (token) => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
    set({ token });
  },
  setCurrentUser: (currentUser) => set({ currentUser }),
}));
