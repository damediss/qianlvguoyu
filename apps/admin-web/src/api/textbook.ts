/**
 * 教材生成接口封装 —— 《职业标准驱动教材生成·一期实施设计》v1.0 §3.4
 * 权限码 textbook:project:view / manage / export
 */
import type { AxiosResponse } from 'axios';
import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  OutlineNodeCreateReq,
  OutlineNodeUpdateReq,
  OutlineNodeVO,
  OutlineSummary,
  ParsePreviewVO,
  ProjectCreateReq,
  ProjectUpdateReq,
  TextbookProjectQuery,
  TextbookProjectVO,
} from '../types/textbook';

const BASE = '/textbook';

/** 同步解析预览（multipart 直传，不落库不走 file 模块），失败抛错由页面展示 Alert */
export function parsePreview(file: File): Promise<ParsePreviewVO> {
  const form = new FormData();
  form.append('file', file);
  return unwrap<ParsePreviewVO>(
    apiClient.post(`${BASE}/projects/parse-preview`, form, { timeout: 120000 }),
  );
}

/**
 * 创建工程并同步完成解析→骨架→配比。
 * parse-preview 不落库，正常路径随 multipart 重新上传原始 docx（后端存 file 模块后回填 fileId）；
 * 已有 fileId 时走 JSON 分支。
 */
export function createProject(req: ProjectCreateReq): Promise<TextbookProjectVO> {
  if (req.file) {
    const form = new FormData();
    form.append('file', req.file);
    form.append('direction', req.direction);
    form.append('levelCode', String(req.levelCode));
    form.append('functionNos', req.functionNos.join(','));
    if (req.trainingHours != null) form.append('trainingHours', String(req.trainingHours));
    if (req.wordsPerHour != null) form.append('wordsPerHour', String(req.wordsPerHour));
    return unwrap<TextbookProjectVO>(apiClient.post(`${BASE}/projects`, form, { timeout: 120000 }));
  }
  return unwrap<TextbookProjectVO>(apiClient.post(`${BASE}/projects`, req));
}

export function pageProjects(params: TextbookProjectQuery): Promise<PageResponse<TextbookProjectVO>> {
  return unwrap<PageResponse<TextbookProjectVO>>(apiClient.get(`${BASE}/projects`, { params }));
}

/** 详情含统计与合计校验 */
export function getProject(id: string): Promise<TextbookProjectVO> {
  return unwrap<TextbookProjectVO>(apiClient.get(`${BASE}/projects/${id}`));
}

export function updateProject(id: string, req: ProjectUpdateReq): Promise<TextbookProjectVO> {
  return unwrap<TextbookProjectVO>(apiClient.put(`${BASE}/projects/${id}`, req));
}

export function deleteProject(id: string): Promise<void> {
  return unwrap<void>(apiClient.delete(`${BASE}/projects/${id}`));
}

/** 重建骨架+配比，返回最新合计校验 */
export function rebuildOutline(id: string): Promise<OutlineSummary> {
  return unwrap<OutlineSummary>(apiClient.post(`${BASE}/projects/${id}/rebuild`));
}

/** 扁平大纲节点（按 parent_id/sort_no 有序），前端自组树 */
export function getOutline(projectId: string): Promise<OutlineNodeVO[]> {
  return unwrap<OutlineNodeVO[]>(apiClient.get(`${BASE}/projects/${projectId}/outline`));
}

/** 改标题/目标字数（置 words_manual=1）/排序，返回更新后的合计校验 */
export function updateOutlineNode(nodeId: string, req: OutlineNodeUpdateReq): Promise<OutlineSummary> {
  return unwrap<OutlineSummary>(apiClient.put(`${BASE}/outline/nodes/${nodeId}`, req));
}

export function createOutlineNode(req: OutlineNodeCreateReq): Promise<OutlineNodeVO> {
  return unwrap<OutlineNodeVO>(apiClient.post(`${BASE}/outline/nodes`, req));
}

export function deleteOutlineNode(nodeId: string): Promise<void> {
  return unwrap<void>(apiClient.delete(`${BASE}/outline/nodes/${nodeId}`));
}

/** blob 下载（坑：axios blob 数据在 .data，文件名取 Content-Disposition），返回原始 AxiosResponse */
export function exportProject(id: string, format: 'md' | 'xlsx'): Promise<AxiosResponse> {
  return apiClient.get(`${BASE}/projects/${id}/export`, {
    params: { format },
    responseType: 'blob',
    timeout: 120000,
  });
}
