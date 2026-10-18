/** 历年真题（管理端 + App 契约）类型。id 与 examId 一律字符串（雪花 id，防 JS 精度丢失）。 */

export interface ZhentiItem {
  id: string;
  examId: string;
  title: string;
  year: string | null;
  fileSize: number;
  sort: number;
  /** 1 上架 / 0 下架 */
  status: number;
  /** 1 待完善（缺年份或工种等级） */
  metaPending: number;
  createdBy: string | null;
  createdById: string | null;
  createdAt: string;
}

export interface ZhentiPage {
  records: ZhentiItem[];
  total: number;
  page: number;
  size: number;
  years: string[];
}

export interface ZhentiUploadResult {
  id: string;
  title: string;
  year: string | null;
  metaPending: number;
}

export interface ZhentiImportFileResult {
  fileName: string;
  ok: boolean;
  id?: string;
  title?: string;
  year?: string | null;
  reason?: string;
  /** 文件名解析不出年份/工种等级：已入库，可原地补录 */
  pending?: boolean;
}

export interface ZhentiImportResult {
  mode: 'sync' | 'async';
  importId?: string;
  results?: ZhentiImportFileResult[];
}

export interface ZhentiImportLog {
  importId: string;
  status: 'running' | 'done' | 'failed' | string;
  total: number;
  finished: number;
  results: ZhentiImportFileResult[];
}

export interface ZhentiPreview {
  viewerUrl: string;
  title: string;
}

export interface ZhentiQuery {
  examId: string;
  page: number;
  pageSize: number;
  keyword?: string;
  year?: string;
  status?: number;
  metaPending?: number;
}
