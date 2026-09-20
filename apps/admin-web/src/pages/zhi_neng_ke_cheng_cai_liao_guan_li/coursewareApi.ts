/**
 * AI 课件生成管理接口封装 —— 方案 v1.2 §T5
 * 权限码 courseware:project:list/create/confirm/generate/retry/export
 */
import { apiClient, unwrap } from '../../api/client';
import type { PageResponse } from '../../types/api';

// ------------------------------------------------------------------
// 类型(雪花 id 均为 string:json-bigint storeAsString + 后端 ToStringSerializer)
// ------------------------------------------------------------------

export interface CoursewareProject {
  id: string;
  title: string;
  audience?: string | null;
  contentDensity: 'concise' | 'medium' | 'detailed';
  status: 'draft' | 'parsing' | 'digesting' | 'outline_generating' | 'outline_ready'
    | 'generating' | 'ready' | 'partial' | 'failed' | 'cancelled';
  totalPages: number;
  readyPages: number;
  failedPages: number;
  error?: string | null;
  createdAt?: string;
}

export interface CoursewareOutlinePage {
  id: string;
  chapterNo: number;
  chapterTitle?: string | null;
  position: number;
  title: string;
  objective?: string | null;
  keyPoints: string[];
  pageRole: 'cover' | 'toc' | 'section' | 'content' | 'review' | string;
  layoutId: string;
  sourceRefs: string[];
  fixed: boolean;
}

export interface CoursewareSlideBrief {
  id: string;
  position: number;
  title?: string | null;
  pageRole?: string | null;
  status: 'pending' | 'generating' | 'ready' | 'failed' | 'cancelled';
  error?: string | null;
}

export interface CoursewareProgress {
  status: CoursewareProject['status'];
  totalPages: number;
  readyPages: number;
  failedPages: number;
  error?: string | null;
  slides: CoursewareSlideBrief[];
}

export interface CoursewareIssue {
  severity: 'error' | 'warning' | string;
  code?: string | null;
  slotId?: string | null;
  message: string;
}

export interface CoursewareBlock {
  slotId: string;
  type: 'text' | 'bullets' | 'table' | 'kpi' | string;
  text?: string | null;
  items?: string[] | null;
  header?: string[] | null;
  rows?: string[][] | null;
  value?: string | null;
  label?: string | null;
  note?: string | null;
}

export interface CoursewareSlideDetail {
  id: string;
  position: number;
  title?: string | null;
  pageRole?: string | null;
  layoutId: string;
  blocks?: CoursewareBlock[] | null;
  speakerNotes?: string | null;
  issues?: CoursewareIssue[] | null;
  status: string;
  error?: string | null;
  revision: number;
}

export interface CoursewareExportResult {
  blocked: boolean;
  issues: CoursewareIssue[];
  fileId?: string | null;
  warnings: CoursewareIssue[];
}

// ------------------------------------------------------------------
// 项目
// ------------------------------------------------------------------

export const createCoursewareProject = (req: {
  sourceFileId: string; title?: string; audience?: string; contentDensity?: string;
}) => unwrap<CoursewareProject>(apiClient.post('/courseware/projects', req));

export const listCoursewareProjects = (params: { page?: number; pageSize?: number }) =>
  unwrap<PageResponse<CoursewareProject>>(apiClient.get('/courseware/projects/page', { params }));

export const getCoursewareProject = (id: string) =>
  unwrap<CoursewareProject>(apiClient.get(`/courseware/projects/${id}`));

export const deleteCoursewareProject = (id: string) =>
  unwrap<void>(apiClient.delete(`/courseware/projects/${id}`));

export const getCoursewareProgress = (id: string) =>
  unwrap<CoursewareProgress>(apiClient.get(`/courseware/projects/${id}/progress`));

export const cancelCoursewareProject = (id: string) =>
  unwrap<void>(apiClient.post(`/courseware/projects/${id}/cancel`));

// ------------------------------------------------------------------
// 大纲
// ------------------------------------------------------------------

export const getCoursewareOutline = (id: string) =>
  unwrap<CoursewareOutlinePage[]>(apiClient.get(`/courseware/projects/${id}/outline`));

export const updateOutlinePage = (pageId: string, patch: {
  title?: string; objective?: string; keyPoints?: string[]; chapterTitle?: string;
  layoutId?: string;
}) => unwrap<CoursewareOutlinePage>(apiClient.put(`/courseware/outline/pages/${pageId}`, patch));

export const addOutlinePage = (projectId: string, req: {
  chapterNo?: number; afterPageId?: string; title: string; objective?: string; keyPoints?: string[];
}) => unwrap<CoursewareOutlinePage>(
  apiClient.post(`/courseware/projects/${projectId}/outline/pages`, req));

export const deleteOutlinePage = (pageId: string) =>
  unwrap<void>(apiClient.delete(`/courseware/outline/pages/${pageId}`));

export const reorderOutline = (projectId: string, orderedIds: string[]) =>
  unwrap<void>(apiClient.post(`/courseware/projects/${projectId}/outline/reorder`, { orderedIds }));

export const confirmCoursewareOutline = (projectId: string) =>
  unwrap<void>(apiClient.post(`/courseware/projects/${projectId}/confirm`));

// ------------------------------------------------------------------
// 页 / 导出
// ------------------------------------------------------------------

export const getCoursewareSlide = (slideId: string) =>
  unwrap<CoursewareSlideDetail>(apiClient.get(`/courseware/slides/${slideId}`));

export const retryCoursewareSlide = (slideId: string) =>
  unwrap<void>(apiClient.post(`/courseware/slides/${slideId}/retry`));

export const getCoursewareIssues = (projectId: string) =>
  unwrap<CoursewareIssue[]>(apiClient.get(`/courseware/projects/${projectId}/issues`));

export const exportCoursewareProject = (projectId: string) =>
  unwrap<CoursewareExportResult>(
    apiClient.post(`/courseware/projects/${projectId}/export`, null, { timeout: 120000 }));
