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