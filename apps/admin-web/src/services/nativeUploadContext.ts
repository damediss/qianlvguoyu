import { apiClient } from '../api/client';
import { useAuthStore } from '../stores/authStore';

export interface NativeFileUploadPayload {
  uploadUrl: string;
  authorization: string;
  tenantId: string;
  visibility: 'private';
  accessPolicy: 'private';
  fileFieldName: 'file';
  accessPolicyFieldName: 'accessPolicy';
}

function resolveApiUrl(path: string): string {
  const baseUrl = String(apiClient.defaults.baseURL || '/api').replace(/\/$/, '');
  const cleanPath = path.replace(/^\//, '');
  return new URL(`${baseUrl}/${cleanPath}`, window.location.origin).toString();
}

export function buildNativeFileUploadPayload(): NativeFileUploadPayload {
  const { token, tenantId } = useAuthStore.getState();
  if (!token) {
    throw new Error('登录态已失效，请重新登录后再采集');
  }
  if (!tenantId) {
    throw new Error('缺少租户信息，请重新登录后再采集');
  }

  return {
    uploadUrl: resolveApiUrl('/files'),
    authorization: `Bearer ${token}`,
    tenantId: String(tenantId),
    visibility: 'private',
    accessPolicy: 'private',
    fileFieldName: 'file',
    accessPolicyFieldName: 'accessPolicy',
  };
}
