import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  AssessorInfo,
  AssessorInfoPayload,
  ExamStaffInfo,
  ExamStaffInfoPayload,
  CertificationInfoQuery,
  ExamEquipmentInfo,
  ExamEquipmentInfoPayload,
  ExamSiteInfo,
  ExamSiteInfoPayload,
  InfoDisablePayload,
  InfoReviewPayload,
} from '../types/certificationInfo';

const base = '/certification/info';

export function pageAssessorInfo(params: CertificationInfoQuery) {
  return unwrap<PageResponse<AssessorInfo>>(apiClient.get(`${base}/assessors`, { params }));
}

export function listAvailableAssessors(keyword?: string) {
  return unwrap<AssessorInfo[]>(apiClient.get(`${base}/assessors/available`, { params: { keyword } }));
}

export function createAssessorInfo(payload: AssessorInfoPayload) {
  return unwrap<AssessorInfo>(apiClient.post(`${base}/assessors`, payload));
}

export function updateAssessorInfo(id: string, payload: AssessorInfoPayload) {
  return unwrap<AssessorInfo>(apiClient.put(`${base}/assessors/${id}`, payload));
}

export function submitAssessorInfo(id: string) {
  return unwrap<AssessorInfo>(apiClient.post(`${base}/assessors/${id}/submit`));
}

export function reviewAssessorInfo(id: string, payload: InfoReviewPayload) {
  return unwrap<AssessorInfo>(apiClient.post(`${base}/assessors/${id}/review`, payload));
}

export function disableAssessorInfo(id: string, payload: InfoDisablePayload) {
  return unwrap<void>(apiClient.post(`${base}/assessors/${id}/disable`, payload));
}

export function enableAssessorInfo(id: string) {
  return unwrap<void>(apiClient.post(`${base}/assessors/${id}/enable`));
}

export function deleteAssessorInfo(id: string) {
  return unwrap<void>(apiClient.delete(`${base}/assessors/${id}`));
}

export function pageExamStaffInfo(params: CertificationInfoQuery) {
  return unwrap<PageResponse<ExamStaffInfo>>(apiClient.get(`${base}/exam-staff`, { params }));
}

export function listAvailableExamStaff(keyword?: string) {
  return unwrap<ExamStaffInfo[]>(apiClient.get(`${base}/exam-staff/available`, { params: { keyword } }));
}

export function createExamStaffInfo(payload: ExamStaffInfoPayload) {
  return unwrap<ExamStaffInfo>(apiClient.post(`${base}/exam-staff`, payload));
}

export function updateExamStaffInfo(id: string, payload: ExamStaffInfoPayload) {
  return unwrap<ExamStaffInfo>(apiClient.put(`${base}/exam-staff/${id}`, payload));
}

export function submitExamStaffInfo(id: string) {
  return unwrap<ExamStaffInfo>(apiClient.post(`${base}/exam-staff/${id}/submit`));
}

export function reviewExamStaffInfo(id: string, payload: InfoReviewPayload) {
  return unwrap<ExamStaffInfo>(apiClient.post(`${base}/exam-staff/${id}/review`, payload));
}

export function disableExamStaffInfo(id: string, payload: InfoDisablePayload) {
  return unwrap<void>(apiClient.post(`${base}/exam-staff/${id}/disable`, payload));
}

export function enableExamStaffInfo(id: string) {
  return unwrap<void>(apiClient.post(`${base}/exam-staff/${id}/enable`));
}

export function deleteExamStaffInfo(id: string) {
  return unwrap<void>(apiClient.delete(`${base}/exam-staff/${id}`));
}

export function pageExamSiteInfo(params: CertificationInfoQuery) {
  return unwrap<PageResponse<ExamSiteInfo>>(apiClient.get(`${base}/sites`, { params }));
}

export function listAvailableExamSites(keyword?: string) {
  return unwrap<ExamSiteInfo[]>(apiClient.get(`${base}/sites/available`, { params: { keyword } }));
}

export function createExamSiteInfo(payload: ExamSiteInfoPayload) {
  return unwrap<ExamSiteInfo>(apiClient.post(`${base}/sites`, payload));
}

export function updateExamSiteInfo(id: string, payload: ExamSiteInfoPayload) {
  return unwrap<ExamSiteInfo>(apiClient.put(`${base}/sites/${id}`, payload));
}

export function submitExamSiteInfo(id: string) {
  return unwrap<ExamSiteInfo>(apiClient.post(`${base}/sites/${id}/submit`));
}

export function reviewExamSiteInfo(id: string, payload: InfoReviewPayload) {
  return unwrap<ExamSiteInfo>(apiClient.post(`${base}/sites/${id}/review`, payload));
}

export function disableExamSiteInfo(id: string, payload: InfoDisablePayload) {
  return unwrap<void>(apiClient.post(`${base}/sites/${id}/disable`, payload));
}

export function enableExamSiteInfo(id: string) {
  return unwrap<void>(apiClient.post(`${base}/sites/${id}/enable`));
}

export function deleteExamSiteInfo(id: string) {
  return unwrap<void>(apiClient.delete(`${base}/sites/${id}`));
}

export function pageExamEquipmentInfo(params: CertificationInfoQuery) {
  return unwrap<PageResponse<ExamEquipmentInfo>>(apiClient.get(`${base}/equipment`, { params }));
}

export function listAvailableEquipment(keyword?: string) {
  return unwrap<ExamEquipmentInfo[]>(apiClient.get(`${base}/equipment/available`, { params: { keyword } }));
}

export function createExamEquipmentInfo(payload: ExamEquipmentInfoPayload) {
  return unwrap<ExamEquipmentInfo>(apiClient.post(`${base}/equipment`, {
    ...payload,
    equipmentStatus: payload.equipmentStatus || 'idle',
  }));
}

export function updateExamEquipmentInfo(id: string, payload: ExamEquipmentInfoPayload) {
  return unwrap<ExamEquipmentInfo>(apiClient.put(`${base}/equipment/${id}`, payload));
}

export function submitExamEquipmentInfo(id: string) {
  return unwrap<ExamEquipmentInfo>(apiClient.post(`${base}/equipment/${id}/submit`));
}

export function reviewExamEquipmentInfo(id: string, payload: InfoReviewPayload) {
  return unwrap<ExamEquipmentInfo>(apiClient.post(`${base}/equipment/${id}/review`, payload));
}

export function disableExamEquipmentInfo(id: string, payload: InfoDisablePayload) {
  return unwrap<void>(apiClient.post(`${base}/equipment/${id}/disable`, payload));
}

export function enableExamEquipmentInfo(id: string) {
  return unwrap<void>(apiClient.post(`${base}/equipment/${id}/enable`));
}

export function deleteExamEquipmentInfo(id: string) {
  return unwrap<void>(apiClient.delete(`${base}/equipment/${id}`));
}
