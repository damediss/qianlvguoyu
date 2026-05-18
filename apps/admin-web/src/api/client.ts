import axios from 'axios';
import { message } from 'antd';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse } from '../types/api';

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 20000,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const tenantId = useAuthStore.getState().tenantId;
  if (tenantId) {
    config.headers['X-Tenant-Id'] = String(tenantId);
  }
  config.headers['X-Request-Id'] = crypto.randomUUID();
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
    const isProxyFailure =
      error?.response?.status === 500
      && typeof error?.response?.data === 'string'
      && !error.response.data.trim();
    const msg = isProxyFailure
      ? '后端服务未启动或不可达，请先启动 platform-api'
      : error?.response?.data?.message || error?.message || '请求失败';
    message.error(msg);
    return Promise.reject(error);
  },
);

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}
