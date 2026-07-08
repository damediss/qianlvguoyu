import { apiClient, unwrap } from './client';
import type { CaptchaGenerateVO, CaptchaVerifyVO, CurrentUserVO, LoginReq, LoginVO, MenuVO } from '../types/api';

export function login(req: LoginReq) {
  return unwrap<LoginVO>(apiClient.post('/auth/login', req));
}

export function currentUser() {
  return unwrap<CurrentUserVO>(apiClient.get('/auth/me', { silentError: true }));
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

export function generateCaptcha() {
  return unwrap<CaptchaGenerateVO>(apiClient.get('/auth/captcha/generate'));
}

export function verifyCaptcha(captchaId: string, x: number) {
  return unwrap<CaptchaVerifyVO>(apiClient.post('/auth/captcha/verify', { captchaId, x }));
}
