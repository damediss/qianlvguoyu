import axios from 'axios';
import { message } from 'antd';
import JSONBig from 'json-bigint';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse } from '../types/api';

// Use json-bigint to handle large integers (snowflake IDs) without precision loss.
// Numbers exceeding Number.MAX_SAFE_INTEGER are stored as strings.
const jsonBig = JSONBig({ storeAsString: true });

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 20000,
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        return jsonBig.parse(data);
      } catch {
        return data;
      }
    }
    return data;
  }],
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
  config.headers['X-Request-Id'] = generateUUID();
  // FormData 必须由浏览器自动设置 multipart boundary，手动设 Content-Type 会导致文件体为空
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
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
    if (error?.response?.status === 401) {
      useAuthStore.getState().setToken(undefined);
      useAuthStore.getState().setCurrentUser(undefined);
      return Promise.reject(error);
    }

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