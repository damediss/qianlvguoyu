import axios from 'axios';
import { message } from 'antd';
import JSONBig from 'json-bigint';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse } from '../types/api';

declare module 'axios' {
  export interface AxiosRequestConfig {
    silentError?: boolean;
  }
}

// Use json-bigint to handle large integers (snowflake IDs) without precision loss.
// Numbers exceeding Number.MAX_SAFE_INTEGER are stored as strings.
const jsonBig = JSONBig({ storeAsString: true });

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 25000,
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
  // 仅在已登录（有 token）时附带 X-Tenant-Id；登录前的 /auth/* 请求不应带可能过期的租户头，
  // 否则租户归一等变更后浏览器残留的旧 tenantId 会让登录查询被多租户拦截器过滤 → 401。
  if (token && tenantId) {
    config.headers['X-Tenant-Id'] = String(tenantId);
  }
  config.headers['X-Request-Id'] = generateUUID();
  // FormData 必须由浏览器自动设置 multipart boundary
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
    delete config.headers['content-type'];
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    // blob / arraybuffer 下载不能按 JSON ApiResponse 解析，否则图片预览会异常
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer'
      || (typeof Blob !== 'undefined' && response.data instanceof Blob)) {
      return response;
    }
    const payload = response.data as ApiResponse<unknown>;
    if (payload && payload.code && payload.code !== '0') {
      throw new Error(payload.message || '请求失败');
    }
    return response;
  },
  async (error) => {
    if (error?.response?.status === 401) {
      useAuthStore.getState().clearAuth();
      const url = String(error?.config?.url ?? '');
      if (url.includes('/auth/login') && !error?.config?.silentError) {
        message.error(error?.response?.data?.message || '用户名或密码错误');
      }
      return Promise.reject(error);
    }

    const isProxyFailure =
      error?.response?.status === 500
      && typeof error?.response?.data === 'string'
      && !error.response.data.trim();

    let msg: string;
    if (isProxyFailure) {
      msg = '后端服务未启动或不可达，请先启动 platform-api';
    } else if (error?.response?.data instanceof Blob) {
      // responseType: 'blob' 的接口出错时，响应体是 Blob，需要解析出 JSON 错误信息
      msg = await parseBlobErrorMessage(error.response.data)
        || error?.message || '请求失败';
    } else {
      msg = error?.response?.data?.message || error?.message || '请求失败';
    }
    if (!error?.config?.silentError) {
      message.error(msg);
    }
    return Promise.reject(error);
  },
);

export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}

/** Blob 类型的错误响应体中提取后端返回的 message 字段 */
async function parseBlobErrorMessage(blob: Blob): Promise<string | null> {
  // 非 JSON 类型且体积较大时，大概率是正常的二进制内容而非错误
  if (!blob.type.includes('json') && blob.size > 512) return null;
  try {
    const text = await blob.text();
    const json = JSON.parse(text) as { message?: string };
    return json.message || null;
  } catch {
    return null;
  }
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
