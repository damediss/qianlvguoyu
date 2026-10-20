import { apiClient, unwrap } from './client';
export function pageUsers(params) {
    return unwrap(apiClient.get('/chazheng/users', { params }));
}
export function getYmdList() {
    return unwrap(apiClient.get('/chazheng/users/ymd'));
}
export function importUsers(file) {
    const formData = new FormData();
    formData.append('file', file);
    return unwrap(apiClient.post('/chazheng/users/import', formData));
}
export function triggerCertCheckByYmd(ymd, sourceFilename, onlyUnsuccessful) {
    return unwrap(apiClient.post(`/chazheng/users/import/${encodeURIComponent(ymd)}/cert`, null, {
        params: {
            sourceFilename: sourceFilename || undefined,
            onlyUnsuccessful: onlyUnsuccessful || undefined,
        },
    }));
}
export function certCheck(id) {
    return unwrap(apiClient.post(`/chazheng/users/${id}/cert`));
}
export function getCertProgress() {
    return unwrap(apiClient.get('/chazheng/users/cert/progress'));
}
/** 本查证批次进度（按批查，用于「重新查证」后的进度弹窗） */
export function getCertBatchProgress(batchId) {
    return unwrap(apiClient.get(`/chazheng/cert/batch/${batchId}/progress`));
}
export function getEnterprises() {
    return unwrap(apiClient.get('/chazheng/enterprises'));
}
export function downloadTemplate() {
    return apiClient.get('/chazheng/template', { responseType: 'blob' });
}
export function exportUsers(ymd, ids) {
    return apiClient.post('/chazheng/users/export', null, {
        params: { ymd: ymd || undefined, ids: ids?.join(',') || undefined },
        responseType: 'blob',
    });
}
