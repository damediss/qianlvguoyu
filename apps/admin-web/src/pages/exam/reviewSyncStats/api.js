/**
 * AI 同步学统计接口封装 —— 《AI同步学统计功能设计方案》v1.1 §5.2
 * 管理端只读接口，权限码 exam:review-sync:stats:view / export
 */
import { apiClient, unwrap } from '../../../api/client';
const BASE = '/exam/review-sync/stats';
// ------------------------------------------------------------------
// API
// ------------------------------------------------------------------
export function fetchStatsOverview(q) {
    return unwrap(apiClient.get(`${BASE}/overview`, { params: q }));
}
export function fetchDailyTrend(q) {
    return unwrap(apiClient.get(`${BASE}/daily-trend`, { params: q }));
}
export function fetchWrongTrend(q) {
    return unwrap(apiClient.get(`${BASE}/wrong-trend`, { params: q }));
}
export function fetchKnowledgeMastery(examId, platformUserId) {
    return unwrap(apiClient.get(`${BASE}/knowledge-mastery`, { params: { examId, platformUserId } }));
}
export function fetchStudents(params) {
    return unwrap(apiClient.get(`${BASE}/students`, { params }));
}
export function fetchStudentDetail(planId) {
    return unwrap(apiClient.get(`${BASE}/students/${planId}`));
}
/** 学员明细 Excel 导出：返回 blob（含 5000 行上限的后端拦截） */
export async function exportStudents(params) {
    const resp = await apiClient.get(`${BASE}/students/export`, {
        params,
        responseType: 'blob',
        timeout: 120000,
    });
    const blob = resp.data;
    // 业务异常（如超上限）以 JSON 形式返回，需转错误
    if (blob.type.includes('json')) {
        const text = await blob.text();
        try {
            const parsed = JSON.parse(text);
            throw new Error(parsed.message || '导出失败');
        }
        catch (e) {
            if (e instanceof SyntaxError) {
                throw new Error('导出失败');
            }
            throw e;
        }
    }
    return blob;
}
/** 触发浏览器下载 */
export function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
