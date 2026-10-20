import { apiClient, unwrap } from './client';
import { parseContentDispositionFilename } from '../utils/download';
const base = '/certification/practical-collection';
const collectionRequestConfig = { timeout: 120000 };
export function getCollectionTasks(params) {
    return unwrap(apiClient.get(`${base}/tasks`, { params, ...collectionRequestConfig }));
}
export function getCollectionWorkbench(taskId, examSessionId) {
    return unwrap(apiClient.get(`${base}/workbench/${taskId}`, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export function initializeCollection(taskId, examSessionId) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/initialize`, undefined, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export function generateQrCodes(taskId, examSessionId) {
    return unwrap(apiClient.post(`${base}/tasks/${taskId}/qrcodes/generate`, undefined, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export async function exportQrCodes(taskId, examSessionId) {
    const response = await apiClient.get(`${base}/tasks/${taskId}/qrcodes/export`, {
        ...collectionRequestConfig,
        params: { examSessionId },
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response)
        || `practical-collection-qrcodes-${taskId}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    return { filename };
}
export function matchByToken(params) {
    return unwrap(apiClient.post(`${base}/match-by-token`, params, collectionRequestConfig));
}
export function scanMatchCurrent(taskId, token, examSessionId) {
    return unwrap(apiClient.post(`${base}/workbench/${taskId}/scan-match-current`, { token }, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export function openCandidateCollection(taskId, candidateId, examSessionId) {
    return unwrap(apiClient.post(`${base}/workbench/${taskId}/candidates/${candidateId}/open`, undefined, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export function cancelCurrentCollection(taskId, candidateId, examSessionId) {
    return unwrap(apiClient.post(`${base}/workbench/${taskId}/candidates/${candidateId}/cancel-current`, undefined, { ...collectionRequestConfig, params: { examSessionId } }));
}
export function searchCandidate(params) {
    return unwrap(apiClient.post(`${base}/search-candidate`, params, collectionRequestConfig));
}
export function joinQueue(params) {
    return unwrap(apiClient.post(`${base}/queue/join`, params, collectionRequestConfig));
}
export function joinQueueBatch(params) {
    return unwrap(apiClient.post(`${base}/queue/join-batch`, params, collectionRequestConfig));
}
export async function exportCollectionForms(taskId, candidateIds, examSessionId) {
    const response = await apiClient.post(`${base}/tasks/${taskId}/collection-forms/export`, {
        examSessionId,
        candidateIds,
    }, {
        ...collectionRequestConfig,
        responseType: 'blob',
    });
    const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const filename = parseContentDispositionFilename(response) || `职业技能等级认定座次签到表_${taskId}.xlsx`;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    return { filename };
}
export function exportCollectionPdf(examTaskId, candidateIds, examSessionId) {
    return unwrap(apiClient.post('/certification/collection/export/pdf', {
        examTaskId,
        examSessionId,
        candidateIds,
    }, collectionRequestConfig));
}
export function removeQueue(params) {
    return unwrap(apiClient.post(`${base}/queue/remove`, params, collectionRequestConfig));
}
export function startNext(params) {
    return unwrap(apiClient.post(`${base}/queue/start-next`, params, collectionRequestConfig));
}
export function submitFingerprint(params) {
    return unwrap(apiClient.post(`${base}/fingerprint/submit`, params, collectionRequestConfig));
}
export function submitSignature(params) {
    return unwrap(apiClient.post(`${base}/signature/submit`, params, collectionRequestConfig));
}
export function completeCollection(params) {
    return unwrap(apiClient.post(`${base}/complete`, params, collectionRequestConfig));
}
export function markPendingRetry(params) {
    return unwrap(apiClient.post(`${base}/mark-pending-retry`, params, collectionRequestConfig));
}
export function markInfoError(params) {
    return unwrap(apiClient.post(`${base}/mark-info-error`, params, collectionRequestConfig));
}
export function exceptionRelease(params) {
    return unwrap(apiClient.post(`${base}/exception-release`, params, collectionRequestConfig));
}
export function getCollectionDetail(taskId, candidateId, examSessionId) {
    return unwrap(apiClient.get(`${base}/detail/${taskId}/${candidateId}`, {
        ...collectionRequestConfig,
        params: { examSessionId },
    }));
}
export function getCollectionExceptions(params) {
    return unwrap(apiClient.get(`${base}/exceptions`, { params, ...collectionRequestConfig }));
}
