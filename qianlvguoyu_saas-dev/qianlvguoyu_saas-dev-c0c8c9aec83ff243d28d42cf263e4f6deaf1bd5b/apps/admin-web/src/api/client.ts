import axios from 'axios';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse } from '../types/api';
import { appMessage } from '../utils/appMessage';
import { resolveTenantId } from '../utils/tenant';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 20000,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const { tenantId, currentUser } = useAuthStore.getState();
  const resolvedTenantId = resolveTenantId(tenantId, currentUser?.tenantId);
  if (resolvedTenantId) {
    config.headers['X-Tenant-Id'] = String(resolvedTenantId);
  }
  config.headers['X-Request-Id'] = generateUUID();
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload && payload.code && payload.code !== '0') {
      throw new Error(payload.message || '请求失败');
    }
    return response;
  },
  (error) => {
    if (error?.config?.responseType === 'blob' && axios.isAxiosError(error)) {
      return Promise.reject(error);
    }
    if (error?.response?.status === 401) {
      const store = useAuthStore.getState();
      store.setToken(undefined);
      store.setTenantId(undefined);
      store.setCurrentUser(undefined);
      return Promise.reject(error);
    }

    const isProxyFailure =
      error?.response?.status === 500
      && typeof error?.response?.data === 'string'
      && !error.response.data.trim();
    const msg = isProxyFailure
      ? '后端服务未启动或不可达，请先启动 platform-api'
      : error?.response?.data?.message || error?.message || '请求失败';
    appMessage.error(msg);
    return Promise.reject(new Error(msg));
  },
);

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}

function generateUUID(): string {
  try {
    return crypto.randomUUID();
  } catch {
    const arr = new Uint8Array(16);
    crypto.getRandomValues(arr);
    arr[6] = (arr[6] & 0x0f) | 0x40;
    arr[8] = (arr[8] & 0x3f) | 0x80;
    const hex = Array.from(arr, (b) => b.toString(16).padStart(2, '0'));
    return [
      hex.slice(0, 4).join(''),
      hex.slice(4, 6).join(''),
      hex.slice(6, 8).join(''),
      hex.slice(8, 10).join(''),
      hex.slice(10, 16).join(''),
    ].join('-');
  }
}
