import { apiClient, unwrap } from './client';
import type { CurrentUserVO, LoginReq, LoginVO, MenuVO } from '../types/api';

export function login(req: LoginReq) {
  return unwrap<LoginVO>(apiClient.post('/auth/login', req));
}

export function currentUser() {
  return unwrap<CurrentUserVO>(apiClient.get('/auth/me'));
}

export function fetchMenus() {
  return unwrap<MenuVO[]>(apiClient.get('/auth/menus'));
}

export function logout() {
  return apiClient.post('/auth/logout');
}

export interface TenantOption {
  code: string;
  name: string;
}

export function fetchTenants() {
  return unwrap<TenantOption[]>(apiClient.get('/auth/tenants'));
}
