/**
 * 细目表管理接口封装 —— 《职业技能标准细目表生成功能实现方案》v1.2 §11
 * 权限码 ximubiaoguanli:view / generate / export
 */
import { apiClient, unwrap } from '../../api/client';
import type { PageResponse } from '../../types/api';

const BASE = '/exam/standard';

// ------------------------------------------------------------------
// 类型
// ------------------------------------------------------------------

export interface StandardTask {
  id: string;
  workTypeTitle: string;
  seriesNo: string | null;
  version: string | null;
  fileId: string | null;
  status: 'uploaded' | 'parsed' | 'llm_running' | 'reviewing' | 'confirmed' | 'exported';
  directions: string[];
  levels: Array<{ levelCode: number; levelName: string }>;
  leafTotal: number;
  leafPending: number;
  parseWarnings: string | null;
  llmModelId: string | null;
  llmFinishedAt: string | null;
  createdAt: string | null;
}

export interface StandardNode {
  id: string;
  taskId: string;
  parentId: string;
  nodeKey: string;
  nodeLevel: number;
  sortNumber: number;
  dimension: 'theory' | 'skill';
  titleRaw: string | null;
  title: string;
  important: string | null;
  weight: number | null;
  relatedKnowledge: string | null;
  skillRequirement: string | null;
  source: 'extract' | 'llm' | 'manual';
  reviewStatus: 'pending' | 'approved' | 'modified';
  direction: string;
  levelCode: number;
  needLlm: number;
}

export interface NodeUpdateReq {
  title?: string;
  important?: string;
  weight?: number;
  sortNumber?: number;
}

export interface NodeCreateReq {
  taskId: string;
  parentId: string;
  title: string;
  important?: string;
  dimension?: 'theory' | 'skill';
}

// ------------------------------------------------------------------
// 任务
// ------------------------------------------------------------------

export async function uploadStandardDocx(file: File): Promise<StandardTask> {
  const form = new FormData();
  form.append('file', file);
  return unwrap(apiClient.post(`${BASE}/tasks`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  }));
}

export async function fetchTaskPage(params: {
  page: number;
  pageSize: number;
  keyword?: string;
}): Promise<PageResponse<StandardTask>> {
  return unwrap(apiClient.get(`${BASE}/tasks`, { params }));
}

export async function fetchTask(taskId: string): Promise<StandardTask> {
  return unwrap(apiClient.get(`${BASE}/tasks/${taskId}`));
}

export async function triggerSummarize(taskId: string): Promise<void> {
  await apiClient.post(`${BASE}/tasks/${taskId}/summarize`);
}

export async function reviewDone(taskId: string): Promise<StandardTask> {
  return unwrap(apiClient.post(`${BASE}/tasks/${taskId}/review-done`));
}

export async function deleteTask(taskId: string): Promise<void> {
  await apiClient.delete(`${BASE}/tasks/${taskId}`);
}

// ------------------------------------------------------------------
// 树节点
// ------------------------------------------------------------------

export async function fetchTree(taskId: string, params: {
  levelCode?: number;
  direction?: string;
  dimension?: 'theory' | 'skill';
}): Promise<StandardNode[]> {
  return unwrap(apiClient.get(`${BASE}/tasks/${taskId}/tree`, { params }));
}

export async function updateNode(nodeId: string, req: NodeUpdateReq): Promise<StandardNode> {
  return unwrap(apiClient.put(`${BASE}/nodes/${nodeId}`, req));
}

export async function createNode(req: NodeCreateReq): Promise<StandardNode> {
  return unwrap(apiClient.post(`${BASE}/nodes`, req));
}

export async function deleteNode(nodeId: string): Promise<void> {
  await apiClient.delete(`${BASE}/nodes/${nodeId}`);
}

export async function moveNode(nodeId: string, direction: 'up' | 'down'): Promise<void> {
  await apiClient.post(`${BASE}/nodes/${nodeId}/move`, null, { params: { direction } });
}

export async function resummarizeNode(nodeId: string, dimension: 'theory' | 'skill'): Promise<StandardNode[]> {
  return unwrap(apiClient.post(`${BASE}/nodes/${nodeId}/resummarize`, null, {
    params: { dimension },
    timeout: 120000,
  }));
}

// ------------------------------------------------------------------
// 导出（blob 下载，坑：axios blob 数据在 .data）
// ------------------------------------------------------------------

export type ExportKind = 'excel' | 'skill-excel' | 'sql';

export async function downloadExport(taskId: string, kind: ExportKind, params?: {
  levelCode?: number;
  direction?: string;
  mode?: 'create' | 'reuse';
}): Promise<void> {
  const resp = await apiClient.get(`${BASE}/tasks/${taskId}/export/${kind}`, {
    params,
    responseType: 'blob',
    timeout: 120000,
  });
  const blob: Blob = resp.data;
  // 后端业务错误会以 JSON 返回，需识别（导出前置校验 400 等）
  if (blob.type.includes('application/json')) {
    const text = await blob.text();
    try {
      const json = JSON.parse(text);
      throw new Error(json.message || '导出失败');
    } catch (e) {
      if (e instanceof SyntaxError) {
        throw new Error('导出失败');
      }
      throw e;
    }
  }
  const disposition: string = resp.headers?.['content-disposition'] || '';
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
