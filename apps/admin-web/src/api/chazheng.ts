import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ChazhengUser,
  ChazhengUserQuery,
  CertCheckBatchProgress,
  CertProgress,
} from '../types/chazheng';

export function pageUsers(params: ChazhengUserQuery) {
  return unwrap<PageResponse<ChazhengUser>>(apiClient.get('/chazheng/users', { params }));
}

export function getYmdList() {
  return unwrap<string[]>(apiClient.get('/chazheng/users/ymd'));
}

export function importUsers(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return unwrap<{ imported: number; ymd: string; message: string }>(
    apiClient.post('/chazheng/users/import', formData),
  );
}

export function triggerCertCheckByYmd(
  ymd: string,
  sourceFilename?: string,
  onlyUnsuccessful?: boolean,
) {
  return unwrap<{ batchId: number; total: number; message: string }>(
    apiClient.post(`/chazheng/users/import/${encodeURIComponent(ymd)}/cert`, null, {
      params: {
        sourceFilename: sourceFilename || undefined,
        onlyUnsuccessful: onlyUnsuccessful || undefined,
      },
    }),
  );
}

export function certCheck(id: number) {
  return unwrap<ChazhengUser>(apiClient.post(`/chazheng/users/${id}/cert`));
}

export function getCertProgress() {
  return unwrap<CertProgress>(apiClient.get('/chazheng/users/cert/progress'));
}

/** 本查证批次进度（按批查，用于「重新查证」后的进度弹窗） */
export function getCertBatchProgress(batchId: number) {
  return unwrap<CertCheckBatchProgress>(
    apiClient.get(`/chazheng/cert/batch/${batchId}/progress`),
  );
}


export function getEnterprises() {
  return unwrap<number[]>(apiClient.get('/chazheng/enterprises'));
}

export function downloadTemplate() {
  return apiClient.get('/chazheng/template', { responseType: 'blob' });
}

export function exportUsers(ymd?: string, ids?: number[]) {
  return apiClient.post('/chazheng/users/export', null, {
    params: { ymd: ymd || undefined, ids: ids?.join(',') || undefined },
    responseType: 'blob',
  });
}