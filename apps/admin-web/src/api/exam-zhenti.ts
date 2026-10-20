import { apiClient, unwrap } from './client';
import type {
  ZhentiImportLog,
  ZhentiImportResult,
  ZhentiPage,
  ZhentiPreview,
  ZhentiQuery,
  ZhentiScopeBatchItem,
  ZhentiScopeInfo,
  ZhentiScopePlans,
  ZhentiUploadResult,
} from '../types/examZhenti';

const BASE = '/appzhenti/admin';

/** D1 管理端列表（records/total/page/size + 当前考试项目年份去重）。 */
export function pageZhentis(params: ZhentiQuery) {
  return unwrap<ZhentiPage>(apiClient.post(`${BASE}/list`, params));
}

/** D2 单个上传；多共享组计划必须显式选择 scope（后端 400 兜底）；409 冲突就地展示。 */
export function uploadZhenti(
  payload: {
    file: File;
    examId: string;
    title?: string;
    year?: string;
    scopeWorkTypeId?: number;
    scopeLevel?: number;
  },
  onProgress?: (percent: number) => void,
) {
  const form = new FormData();
  form.append('file', payload.file);
  form.append('examId', payload.examId);
  if (payload.title) form.append('title', payload.title);
  if (payload.year) form.append('year', payload.year);
  if (payload.scopeWorkTypeId != null) {
    form.append('scopeWorkTypeId', String(payload.scopeWorkTypeId));
  }
  if (payload.scopeLevel != null) {
    form.append('scopeLevel', String(payload.scopeLevel));
  }
  return unwrap<ZhentiUploadResult>(
    apiClient.post(`${BASE}/upload`, form, {
      silentError: true,
      onUploadProgress: (event) => {
        if (!onProgress) return;
        const total = event.total ?? payload.file.size;
        onProgress(total > 0 ? Math.min(100, Math.round((event.loaded / total) * 100)) : 0);
      },
    }),
  );
}

/** D3 批量导入（zip）。多共享组计划必须显式选择 scope。 */
export function batchImportZhentis(
  file: File,
  examId: string,
  scope?: { scopeWorkTypeId: number; scopeLevel: number },
  onProgress?: (percent: number) => void,
) {
  const form = new FormData();
  form.append('file', file);
  form.append('examId', examId);
  if (scope) {
    form.append('scopeWorkTypeId', String(scope.scopeWorkTypeId));
    form.append('scopeLevel', String(scope.scopeLevel));
  }
  return unwrap<ZhentiImportResult>(
    apiClient.post(`${BASE}/batchImport`, form, {
      silentError: true,
      onUploadProgress: (event) => {
        if (!onProgress) return;
        const total = event.total ?? file.size;
        onProgress(total > 0 ? Math.min(100, Math.round((event.loaded / total) * 100)) : 0);
      },
    }),
  );
}

/** D4 局部更新（重命名/改年份/排序/上下架/补录）。 */
export function updateZhenti(payload: {
  id: string;
  title?: string;
  year?: string;
  sort?: number;
  status?: number;
}) {
  return unwrap<ZhentiUploadResult>(apiClient.post(`${BASE}/update`, payload));
}

/** D5 软删。 */
export function deleteZhenti(id: string) {
  return unwrap<void>(apiClient.post(`${BASE}/delete`, { id }));
}

/** D6 导入记录：传 importId 轮询单条，传 examId 取历史分页。 */
export function getZhentiImportLog(params: {
  importId?: string;
  examId?: string;
  page?: number;
  pageSize?: number;
}) {
  return unwrap<ZhentiImportLog | { records: ZhentiImportLog[]; total: number; page: number; size: number }>(
    apiClient.post(`${BASE}/importLog`, params),
  );
}

/** D7 管理端预览签名（与 App 同签名规则）。 */
export function previewZhenti(id: string) {
  return unwrap<ZhentiPreview>(apiClient.post(`${BASE}/preview`, { id }));
}

/** 共享组信息：提示条与上传/导入弹窗的选择项。 */
export function fetchScopeInfo(examId: string) {
  return unwrap<ZhentiScopeInfo>(apiClient.post(`${BASE}/scopeInfo`, { examId }));
}

/** 「真题共享范围」页：计划绑定列表 + 课程选项。 */
export function fetchScopePlans(params: {
  page: number;
  pageSize: number;
  keyword?: string;
  filter?: string;
}) {
  return unwrap<ZhentiScopePlans>(
    apiClient.post(
      `${BASE}/scopePlans?page=${params.page}&pageSize=${params.pageSize}`
        + (params.keyword ? `&keyword=${encodeURIComponent(params.keyword)}` : '')
        + (params.filter ? `&filter=${encodeURIComponent(params.filter)}` : ''),
    ),
  );
}

/** 「真题共享范围」页：批量保存课程绑定（后端重推导工种/等级）。 */
export function scopeBatchSave(items: ZhentiScopeBatchItem[]) {
  return unwrap<void>(apiClient.post(`${BASE}/scopeBatchSave`, { items }));
}
