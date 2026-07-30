import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { ChazhengUser, ChazhengUserQuery, CertProgress } from '../types/chazheng';

export function pageUsers(params: ChazhengUserQuery) {
  return unwrap<PageResponse<ChazhengUser>>(apiClient.get('/chazheng/users', { params }));
}

export function getYmdList() {
  return unwrap<string[]>(apiClient.get('/chazheng/users/ymd'));
}

export function importUsers(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return unwrap<{ imported: number }>(
    apiClient.post('/chazheng/users/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  );
}

export function certCheck(id: number) {
  return unwrap<ChazhengUser>(apiClient.post(`/chazheng/users/${id}/cert`));
}

export function getCertProgress() {
  return unwrap<CertProgress>(apiClient.get('/chazheng/users/cert/progress'));
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