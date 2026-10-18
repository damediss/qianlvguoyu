import type { ZhentiImportFileResult, ZhentiItem, ZhentiQuery } from '../../types/examZhenti';

export const MAX_PDF_BYTES = 50 * 1024 * 1024;
export const MAX_ZIP_BYTES = 200 * 1024 * 1024;

export type StatusFilter = 'all' | '1' | '0' | 'pending';

export interface QueryInput {
  examId?: string;
  page: number;
  pageSize: number;
  keyword?: string;
  year?: string;
  statusFilter: StatusFilter;
}

/** 文件大小格式化：B/KB/MB，KB/MB 保留 1 位小数；空值显示 "-"。 */
export function formatFileSize(bytes?: number | null): string {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes) || bytes < 0) {
    return '-';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** 组装 D1 入参：未选考试项目返回 null；空筛选值不落参。 */
export function buildZhentiQuery(input: QueryInput): ZhentiQuery | null {
  if (!input.examId) {
    return null;
  }
  const query: ZhentiQuery = {
    examId: input.examId,
    page: input.page,
    pageSize: input.pageSize,
  };
  const keyword = input.keyword?.trim();
  if (keyword) {
    query.keyword = keyword;
  }
  if (input.year) {
    query.year = input.year;
  }
  if (input.statusFilter === '1' || input.statusFilter === '0') {
    query.status = Number(input.statusFilter);
  } else if (input.statusFilter === 'pending') {
    query.metaPending = 1;
  }
  return query;
}

/** 上传前预检：扩展名 + 50MB；通过返回 null。 */
export function validatePdfFile(file: { name: string; size: number }): string | null {
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    return '仅支持 PDF 文件';
  }
  if (file.size <= 0) {
    return '文件为空，请重新选择';
  }
  if (file.size > MAX_PDF_BYTES) {
    return '单个 PDF 不能超过 50MB';
  }
  return null;
}

/** zip 预检：扩展名 + 200MB。 */
export function validateZipFile(file: { name: string; size: number }): string | null {
  if (!file.name.toLowerCase().endsWith('.zip')) {
    return '仅支持 zip 压缩包';
  }
  if (file.size <= 0) {
    return '压缩包为空，请重新选择';
  }
  if (file.size > MAX_ZIP_BYTES) {
    return 'zip 压缩包不能超过 200MB';
  }
  return null;
}

/** 状态标签：待完善优先于上下架。 */
export function zhentiStatusTag(item: Pick<ZhentiItem, 'status' | 'metaPending'>): {
  text: string;
  color: string;
} {
  if (item.metaPending === 1) {
    return { text: '待完善', color: 'orange' };
  }
  return item.status === 1
    ? { text: '上架', color: 'green' }
    : { text: '下架', color: 'default' };
}

/** 批量导入失败行是否可原地补录：已入库（有 id）才可补录；未入库需重新上传。 */
export function canRetryImportRow(row: ZhentiImportFileResult): boolean {
  return !row.ok && Boolean(row.id);
}

/** 从 axios 错误里取后端 msg（409 冲突提示直接用后端的“已存在相同文件：xxx”）。 */
export function extractErrorMessage(error: unknown, fallback = '操作失败'): string {
  const candidate = error as {
    response?: { data?: { message?: string } };
    message?: string;
  };
  return candidate?.response?.data?.message || candidate?.message || fallback;
}
