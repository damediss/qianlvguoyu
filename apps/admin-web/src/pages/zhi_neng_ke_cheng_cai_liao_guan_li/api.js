/**
 * 细目表管理接口封装 —— 《职业技能标准细目表生成功能实现方案》v1.2 §11
 * 权限码 ximubiaoguanli:view / generate / export
 */
import { apiClient, unwrap } from '../../api/client';
const BASE = '/exam/standard';
// ------------------------------------------------------------------
// 任务
// ------------------------------------------------------------------
export async function uploadStandardDocx(file) {
    const form = new FormData();
    form.append('file', file);
    return unwrap(apiClient.post(`${BASE}/tasks`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        // 大文件 docx 解析耗时较长，放宽到 180s（超时后后端仍会完成导入，前端需能等到结果）
        timeout: 180000,
    }));
}
export async function fetchTaskPage(params) {
    return unwrap(apiClient.get(`${BASE}/tasks`, { params }));
}
export async function fetchTask(taskId) {
    return unwrap(apiClient.get(`${BASE}/tasks/${taskId}`));
}
export async function triggerSummarize(taskId, req) {
    await apiClient.post(`${BASE}/tasks/${taskId}/summarize`, req);
}
export async function reviewDone(taskId) {
    return unwrap(apiClient.post(`${BASE}/tasks/${taskId}/review-done`));
}
export async function deleteTask(taskId) {
    await apiClient.delete(`${BASE}/tasks/${taskId}`);
}
// ------------------------------------------------------------------
// 树节点
// ------------------------------------------------------------------
export async function fetchTree(taskId, params) {
    return unwrap(apiClient.get(`${BASE}/tasks/${taskId}/tree`, { params }));
}
export async function updateNode(nodeId, req) {
    return unwrap(apiClient.put(`${BASE}/nodes/${nodeId}`, req));
}
export async function createNode(req) {
    return unwrap(apiClient.post(`${BASE}/nodes`, req));
}
export async function deleteNode(nodeId) {
    await apiClient.delete(`${BASE}/nodes/${nodeId}`);
}
export async function moveNode(nodeId, direction) {
    await apiClient.post(`${BASE}/nodes/${nodeId}/move`, null, { params: { direction } });
}
export async function resummarizeNode(nodeId, dimension) {
    return unwrap(apiClient.post(`${BASE}/nodes/${nodeId}/resummarize`, null, {
        params: { dimension },
        timeout: 120000,
    }));
}
export async function downloadExport(taskId, kind, params) {
    const resp = await apiClient.get(`${BASE}/tasks/${taskId}/export/${kind}`, {
        params,
        responseType: 'blob',
        timeout: 120000,
    });
    const blob = resp.data;
    // 后端业务错误会以 JSON 返回，需识别（导出前置校验 400 等）
    if (blob.type.includes('application/json')) {
        const text = await blob.text();
        try {
            const json = JSON.parse(text);
            throw new Error(json.message || '导出失败');
        }
        catch (e) {
            if (e instanceof SyntaxError) {
                throw new Error('导出失败');
            }
            throw e;
        }
    }
    const disposition = resp.headers?.['content-disposition'] || '';
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/);
    const fileName = match ? decodeURIComponent(match[1]) : `export.${kind === 'sql' ? 'sql' : 'xlsx'}`;
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
/** 下载上传的原始 docx 原件（走平台文件模块，COS/本地存储自动适配） */
export async function downloadOriginalDocx(fileId) {
    const resp = await apiClient.get(`/files/${fileId}/download`, {
        responseType: 'blob',
        timeout: 120000,
    });
    const blob = resp.data;
    if (blob.type.includes('application/json')) {
        const text = await blob.text();
        try {
            const json = JSON.parse(text);
            throw new Error(json.message || '下载失败');
        }
        catch (e) {
            if (e instanceof SyntaxError) {
                throw new Error('下载失败');
            }
            throw e;
        }
    }
    const disposition = resp.headers?.['content-disposition'] || '';
    const match = disposition.match(/filename\*=UTF-8''([^;]+)/);
    const fileName = match ? decodeURIComponent(match[1]) : 'standard.docx';
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
