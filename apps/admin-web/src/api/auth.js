import { apiClient, unwrap } from './client';
export function login(req) {
    return unwrap(apiClient.post('/auth/login', req));
}
export function currentUser() {
    return unwrap(apiClient.get('/auth/me', { silentError: true }));
}
export function fetchMenus() {
    return unwrap(apiClient.get('/auth/menus'));
}
export function logout() {
    return apiClient.post('/auth/logout');
}
export function fetchTenants() {
    return unwrap(apiClient.get('/auth/tenants'));
}
export function generateCaptcha() {
    return unwrap(apiClient.get('/auth/captcha/generate'));
}
export function verifyCaptcha(captchaId, x) {
    return unwrap(apiClient.post('/auth/captcha/verify', { captchaId, x }));
}
