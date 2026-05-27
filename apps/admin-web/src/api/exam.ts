import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  ExamWorkType,
  ExamWorkTypeCreateReq,
  ExamWorkTypeUpdateReq,
  ExamWorkTypeQuery,
  ExamCourse,
  ExamCourseCreateReq,
  ExamCourseUpdateReq,
  ExamCourseQuery,
  ExamUserAuthCourse,
  ExamUserAuthCourseCreateReq,
  ExamUserAuthCourseQuery,
  ExamTaskXimu,
  ExamTaskXimuDetail,
  ExamTaskXimuDaily,
  ExamTaskXimuQuery,
  ExamTaskXimuAssignExpertReq,
  PublicData,
} from '../types/exam';

export function pageWorkTypes(params: ExamWorkTypeQuery) {
  return unwrap<PageResponse<ExamWorkType>>(apiClient.get('/exam/work-types', { params }));
}

export function treeWorkTypes() {
  return unwrap<ExamWorkType[]>(apiClient.get('/exam/work-types/tree'));
}

export function getWorkType(id: number) {
  return unwrap<ExamWorkType>(apiClient.get(`/exam/work-types/${id}`));
}

export function createWorkType(payload: ExamWorkTypeCreateReq) {
  return unwrap<ExamWorkType>(apiClient.post('/exam/work-types', payload));
}

export function updateWorkType(id: number, payload: ExamWorkTypeUpdateReq) {
  return unwrap<ExamWorkType>(apiClient.put(`/exam/work-types/${id}`, payload));
}

export function deleteWorkType(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/work-types/${id}`));
}

export function pageCourses(params: ExamCourseQuery) {
  return unwrap<PageResponse<ExamCourse>>(apiClient.get('/exam/courses', { params }));
}

export function getCourse(id: number) {
  return unwrap<ExamCourse>(apiClient.get(`/exam/courses/${id}`));
}

export function createCourse(payload: ExamCourseCreateReq) {
  return unwrap<ExamCourse>(apiClient.post('/exam/courses', payload));
}

export function updateCourse(id: number, payload: ExamCourseUpdateReq) {
  return unwrap<ExamCourse>(apiClient.put(`/exam/courses/${id}`, payload));
}

export function deleteCourse(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/courses/${id}`));
}

export function pageUserAuthCourses(params: ExamUserAuthCourseQuery) {
  return unwrap<PageResponse<ExamUserAuthCourse>>(apiClient.get('/exam/user-auth-courses', { params }));
}

export function getUserAuthCourse(realname: string, idCard: string) {
  return unwrap<ExamUserAuthCourse>(apiClient.get('/exam/user-auth-courses/detail', {
    params: { realname, idCard },
  }));
}

export function createUserAuthCourse(payload: ExamUserAuthCourseCreateReq) {
  return unwrap<ExamUserAuthCourse>(apiClient.post('/exam/user-auth-courses', payload));
}

export function updateUserAuthCourse(realname: string, idCard: string, payload: ExamUserAuthCourseCreateReq) {
  return unwrap<ExamUserAuthCourse>(apiClient.put(
    `/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`,
    payload,
  ));
}

export function deleteUserAuthCourse(realname: string, idCard: string) {
  return unwrap<void>(apiClient.delete(
    `/exam/user-auth-courses/${encodeURIComponent(realname)}/${encodeURIComponent(idCard)}`,
  ));
}

// ---- Exercise task ximu ----
export function pageTaskXimus(params: ExamTaskXimuQuery) {
  return unwrap<PageResponse<ExamTaskXimu>>(apiClient.get('/exam/task-ximus', { params }));
}

export function getTaskXimuDetails(taskId: number) {
  return unwrap<ExamTaskXimuDetail[]>(apiClient.get(`/exam/task-ximus/${taskId}/details`));
}

export function getTaskXimuDaily(taskId: number, role: number) {
  return unwrap<ExamTaskXimuDaily[]>(apiClient.get(`/exam/task-ximus/${taskId}/daily`, { params: { role } }));
}

export function importTaskXimu(formData: FormData) {
  return unwrap<number>(apiClient.post('/exam/task-ximus/import', formData));
}

export function assignTaskXimuExpert(taskId: number, payload: ExamTaskXimuAssignExpertReq) {
  return unwrap<void>(apiClient.post(`/exam/task-ximus/${taskId}/assign-expert`, payload));
}

export function deleteTaskXimu(id: number) {
  return unwrap<void>(apiClient.delete(`/exam/task-ximus/${id}`));
}

export function exportTaskXimu(id: number) {
  return apiClient.get(`/exam/task-ximus/${id}/export`, { responseType: 'blob' });
}

export function getPublicData() {
  return unwrap<PublicData>(apiClient.get('/exam/public-data'));
}
