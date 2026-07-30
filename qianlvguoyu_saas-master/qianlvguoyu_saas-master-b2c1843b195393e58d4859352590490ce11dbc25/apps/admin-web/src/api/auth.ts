import { apiClient, unwrap } from './client';
import type { CurrentUserVO, LoginReq, LoginVO } from '../types/api';

export function login(req: LoginReq) {
  return unwrap<LoginVO>(apiClient.post('/auth/login', req));
}

export function currentUser() {
  return unwrap<CurrentUserVO>(apiClient.get('/auth/me'));
}
