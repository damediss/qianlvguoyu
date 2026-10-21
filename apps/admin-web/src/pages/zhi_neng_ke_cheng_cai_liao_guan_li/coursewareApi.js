/**
 * AI 课件生成管理接口封装 —— 方案 v1.2 §T5
 * 权限码 courseware:project:list/create/confirm/generate/retry/export
 */
import { apiClient, unwrap } from '../../api/client';
// ------------------------------------------------------------------
// 项目
// ------------------------------------------------------------------
export const createCoursewareProject = (req) => unwrap(apiClient.post('/courseware/projects', req));
export const listCoursewareProjects = (params) => unwrap(apiClient.get('/courseware/projects/page', { params }));
export const getCoursewareProject = (id) => unwrap(apiClient.get(`/courseware/projects/${id}`));
export const deleteCoursewareProject = (id) => unwrap(apiClient.delete(`/courseware/projects/${id}`));
export const getCoursewareProgress = (id) => unwrap(apiClient.get(`/courseware/projects/${id}/progress`));
export const cancelCoursewareProject = (id) => unwrap(apiClient.post(`/courseware/projects/${id}/cancel`));
// ------------------------------------------------------------------
// 大纲
// ------------------------------------------------------------------
export const getCoursewareOutline = (id) => unwrap(apiClient.get(`/courseware/projects/${id}/outline`));
export const updateOutlinePage = (pageId, patch) => unwrap(apiClient.put(`/courseware/outline/pages/${pageId}`, patch));
export const addOutlinePage = (projectId, req) => unwrap(apiClient.post(`/courseware/projects/${projectId}/outline/pages`, req));
export const deleteOutlinePage = (pageId) => unwrap(apiClient.delete(`/courseware/outline/pages/${pageId}`));
export const reorderOutline = (projectId, orderedIds) => unwrap(apiClient.post(`/courseware/projects/${projectId}/outline/reorder`, { orderedIds }));
export const confirmCoursewareOutline = (projectId) => unwrap(apiClient.post(`/courseware/projects/${projectId}/confirm`));
// ------------------------------------------------------------------
// 页 / 导出
// ------------------------------------------------------------------
export const getCoursewareSlide = (slideId) => unwrap(apiClient.get(`/courseware/slides/${slideId}`));
export const retryCoursewareSlide = (slideId) => unwrap(apiClient.post(`/courseware/slides/${slideId}/retry`));
export const getCoursewareIssues = (projectId) => unwrap(apiClient.get(`/courseware/projects/${projectId}/issues`));
export const exportCoursewareProject = (projectId) => unwrap(apiClient.post(`/courseware/projects/${projectId}/export`, null, { timeout: 120000 }));
