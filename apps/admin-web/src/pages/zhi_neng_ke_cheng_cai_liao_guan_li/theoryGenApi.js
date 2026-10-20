/**
 * 理论知识试题生成管理接口封装 —— 方案 v1.2 §T5
 * 权限码 exam:theory:blueprint:* / exam:theory:gen:* / exam:theory:draft:*
 */
import { apiClient, unwrap } from '../../api/client';
// ------------------------------------------------------------------
// 细目表批次
// ------------------------------------------------------------------
export const importBlueprint = (file, workTypeId, level) => {
    const form = new FormData();
    form.append('file', file);
    form.append('workTypeId', String(workTypeId));
    form.append('level', String(level));
    return unwrap(apiClient.post('/exam/theory/blueprint/import', form));
};
export const listBlueprints = (params) => unwrap(apiClient.get('/exam/theory/blueprint/page', { params }));
export const getBlueprintTree = (id) => unwrap(apiClient.get(`/exam/theory/blueprint/${id}/tree`));
export const deleteBlueprint = (id) => unwrap(apiClient.delete(`/exam/theory/blueprint/${id}`));
/** 下载导入时的源细目表文件（/files/{id}/download 流式返回，文件名取批次 sourceFileName） */
export const downloadBlueprintSource = async (fileId) => {
    const resp = await apiClient.get(`/files/${fileId}/download`, {
        responseType: 'blob',
        timeout: 120000,
    });
    return resp.data;
};
// ------------------------------------------------------------------
// 生成批次
// ------------------------------------------------------------------
export const submitGenBatch = (blueprintId, modelConfigId, quota) => unwrap(apiClient.post('/exam/theory/gen/submit', { blueprintId, modelConfigId, quota }));
export const listGenBatches = (params) => unwrap(apiClient.get('/exam/theory/gen/page', { params }));
export const getGenProgress = (batchId) => unwrap(apiClient.get(`/exam/theory/gen/${batchId}/progress`));
export const pauseGenBatch = (batchId) => unwrap(apiClient.post(`/exam/theory/gen/${batchId}/pause`));
export const resumeGenBatch = (batchId) => unwrap(apiClient.post(`/exam/theory/gen/${batchId}/resume`));
export const retryGenBatch = (batchId) => unwrap(apiClient.post(`/exam/theory/gen/${batchId}/retry-failed`));
/** 启用中的模型配置（生成任务下拉，默认选中 theory-gen-glm） */
export const listTheoryModelConfigs = () => unwrap(apiClient.get('/exam/theory/gen/model-configs'));
export const listDrafts = (params) => unwrap(apiClient.get('/exam/theory/draft/page', { params }));
export const editDraft = (id, patch) => unwrap(apiClient.put(`/exam/theory/draft/${id}`, patch));
export const reviewDrafts = (draftIds, approve) => unwrap(apiClient.post('/exam/theory/draft/review', { draftIds, approve }));
export const publishDrafts = (batchId, taskMode, taskId, subTaskId, confirmedBelowQuota) => unwrap(apiClient.post('/exam/theory/draft/publish', {
    batchId, taskMode, taskId, subTaskId, confirmedBelowQuota
}));
export const exportDrafts = async (batchId) => {
    // 大批次（上千行）经公网导出可能超过全局 25s 超时，此处单独放宽
    const resp = await apiClient.get(`/exam/theory/draft/export`, {
        params: { batchId },
        responseType: 'blob',
        timeout: 120000,
    });
    return resp.data;
};
