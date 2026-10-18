import { apiClient, unwrap } from './client';
import type {
  ZhentiImportLog,
  ZhentiImportResult,
  ZhentiPage,
  ZhentiPreview,
  ZhentiQuery,
  ZhentiUploadResult,
} from '../types/examZhenti';

const BASE = '/appzhenti/admin';

/** D1 管理端列表（records/total/page/size + 当前考试项目年份去重）。 */
export function pageZhentis(params: ZhentiQuery) {
  return unwrap<ZhentiPage>(apiClient.post(`${BASE}/list`, params));
}

/** D2 单个上传；409 冲突由调用方就地展示（silentError 关闭全局 toast）。 */
export function uploadZhenti(
  payload: { file: File; examId: string; title?: string; year?: string },
  onProgress?: (percent: number) => void,
) {
  const form = new FormData();
  form.append('file', payload.file);
  form.append('examId', payload.examId);
  if (payload.title) form.append('title', payload.title);
  if (payload.year) form.append('year', payload.year);
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

/** D3 批量导入（zip）。 */
export function batchImportZhentis(
  file: File,
  examId: string,
  onProgress?: (percent: number) => void,
) {
  const form = new FormData();
  form.append('file', file);
  form.append('examId', examId);
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
