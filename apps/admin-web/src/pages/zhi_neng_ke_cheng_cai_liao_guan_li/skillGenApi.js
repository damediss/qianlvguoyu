/**
 * 操作技能试题生成管理接口封装 —— 方案 v1.1 §T5
 * 权限码 exam:skill:blueprint:* / exam:skill:gen:* / exam:skill:draft:*
 */
import { apiClient, unwrap } from '../../api/client';
// ------------------------------------------------------------------
// 细目表批次
// ------------------------------------------------------------------
export const importSkillBlueprint = (file, workTypeId, level) => {
    const form = new FormData();
    form.append('file', file);
    form.append('workTypeId', String(workTypeId));
    form.append('level', String(level));
    return unwrap(apiClient.post('/exam/skill/blueprint/import', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
    }));
};
export const listSkillBlueprints = (params) => unwrap(apiClient.get('/exam/skill/blueprint/page', { params }));
export const getSkillBlueprintTree = (id) => unwrap(apiClient.get(`/exam/skill/blueprint/${id}/tree`));
export const deleteSkillBlueprint = (id) => unwrap(apiClient.delete(`/exam/skill/blueprint/${id}`));
export const downloadSkillBlueprintSource = async (id) => {
    const resp = await apiClient.get(`/exam/skill/blueprint/${id}/source-file`, {
        responseType: 'blob',
        timeout: 120000,
    });
    return resp.data;
};
// ------------------------------------------------------------------
// 生成批次
// ------------------------------------------------------------------
export const submitSkillGenBatch = (blueprintId, modelConfigId) => unwrap(apiClient.post('/exam/skill/gen/submit', { blueprintId, modelConfigId }));
export const listSkillGenBatches = (params) => unwrap(apiClient.get('/exam/skill/gen/page', { params }));
export const getSkillGenProgress = (batchId) => unwrap(apiClient.get(`/exam/skill/gen/${batchId}/progress`));
export const pauseSkillGenBatch = (batchId) => unwrap(apiClient.post(`/exam/skill/gen/${batchId}/pause`));
export const resumeSkillGenBatch = (batchId) => unwrap(apiClient.post(`/exam/skill/gen/${batchId}/resume`));
export const retrySkillGenBatch = (batchId) => unwrap(apiClient.post(`/exam/skill/gen/${batchId}/retry-failed`));
export const listSkillModelConfigs = () => unwrap(apiClient.get('/exam/skill/gen/model-configs'));
export const listSkillDrafts = (params) => unwrap(apiClient.get('/exam/skill/draft/page', { params }));
export const editSkillDraft = (id, patch) => unwrap(apiClient.put(`/exam/skill/draft/${id}`, patch));
export const reviewSkillDrafts = (draftIds, approve) => unwrap(apiClient.post('/exam/skill/draft/review', { draftIds, approve }));
export const publishSkillDrafts = (batchId, taskMode, taskId) => unwrap(apiClient.post('/exam/skill/draft/publish', { batchId, taskMode, taskId }));
/** 入库预览：本次可入库数、累计进度 vs 等级下限、缺口（闸门 UI） */
export const getSkillPublishPreview = (batchId) => unwrap(apiClient.get('/exam/skill/draft/publish-preview', { params: { batchId } }));
/**
 * 导出批次试题表：返回文件字节 + 服务端文件名（Content-Disposition filename*）。
 * 文件名规则由后端统一维护（工种(等级)_操作技能试题_批次{短号}.xlsx），前端不再自行拼装。
 */
export const exportSkillDrafts = async (batchId) => {
    const resp = await apiClient.get('/exam/skill/draft/export', {
        params: { batchId },
        responseType: 'blob',
        timeout: 120000,
    });
    let filename = '';
    const disposition = String(resp.headers?.['content-disposition'] ?? '');
    const m = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
    if (m) {
        try {
            filename = decodeURIComponent(m[1].trim());
        }
        catch {
            filename = m[1].trim();
        }
    }
    return { blob: resp.data, filename };
};
