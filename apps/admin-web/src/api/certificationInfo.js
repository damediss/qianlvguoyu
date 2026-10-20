import { apiClient, unwrap } from './client';
const base = '/certification/info';
export function pageAssessorInfo(params) {
    return unwrap(apiClient.get(`${base}/assessors`, { params }));
}
export function listAvailableAssessors(keyword, includeAssigned = false, requiredWorkTypeIds, requiredWorkTypes, requireWorkTypeCoverage = false) {
    return unwrap(apiClient.get(`${base}/assessors/available`, {
        params: { keyword, includeAssigned, requiredWorkTypeIds, requiredWorkTypes, requireWorkTypeCoverage },
    }));
}
export function createAssessorInfo(payload) {
    return unwrap(apiClient.post(`${base}/assessors`, payload));
}
export function updateAssessorInfo(id, payload) {
    return unwrap(apiClient.put(`${base}/assessors/${id}`, payload));
}
export function createAssessorAccount(id, payload) {
    return unwrap(apiClient.post(`${base}/assessors/${id}/account`, payload));
}
export function submitAssessorInfo(id) {
    return unwrap(apiClient.post(`${base}/assessors/${id}/submit`));
}
export function reviewAssessorInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/assessors/${id}/review`, payload));
}
export function disableAssessorInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/assessors/${id}/disable`, payload));
}
export function enableAssessorInfo(id) {
    return unwrap(apiClient.post(`${base}/assessors/${id}/enable`));
}
export function deleteAssessorInfo(id) {
    return unwrap(apiClient.delete(`${base}/assessors/${id}`));
}
export function startAssessorLedgerImport(fileId) {
    return unwrap(apiClient.post(`${base}/assessors/imports`, { fileId }));
}
export function getAssessorLedgerImport(id) {
    return unwrap(apiClient.get(`${base}/assessors/imports/${id}`));
}
export function pageExamStaffInfo(params) {
    return unwrap(apiClient.get(`${base}/exam-staff`, { params }));
}
export function listAvailableExamStaff(keyword) {
    return unwrap(apiClient.get(`${base}/exam-staff/available`, { params: { keyword } }));
}
export function createExamStaffInfo(payload) {
    return unwrap(apiClient.post(`${base}/exam-staff`, payload));
}
export function updateExamStaffInfo(id, payload) {
    return unwrap(apiClient.put(`${base}/exam-staff/${id}`, payload));
}
export function submitExamStaffInfo(id) {
    return unwrap(apiClient.post(`${base}/exam-staff/${id}/submit`));
}
export function reviewExamStaffInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/exam-staff/${id}/review`, payload));
}
export function disableExamStaffInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/exam-staff/${id}/disable`, payload));
}
export function enableExamStaffInfo(id) {
    return unwrap(apiClient.post(`${base}/exam-staff/${id}/enable`));
}
export function deleteExamStaffInfo(id) {
    return unwrap(apiClient.delete(`${base}/exam-staff/${id}`));
}
export function pageExamSiteInfo(params) {
    return unwrap(apiClient.get(`${base}/sites`, { params }));
}
export function listAvailableExamSites(keyword) {
    return unwrap(apiClient.get(`${base}/sites/available`, { params: { keyword } }));
}
export function createExamSiteInfo(payload) {
    return unwrap(apiClient.post(`${base}/sites`, payload));
}
export function updateExamSiteInfo(id, payload) {
    return unwrap(apiClient.put(`${base}/sites/${id}`, payload));
}
export function submitExamSiteInfo(id) {
    return unwrap(apiClient.post(`${base}/sites/${id}/submit`));
}
export function reviewExamSiteInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/sites/${id}/review`, payload));
}
export function disableExamSiteInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/sites/${id}/disable`, payload));
}
export function enableExamSiteInfo(id) {
    return unwrap(apiClient.post(`${base}/sites/${id}/enable`));
}
export function deleteExamSiteInfo(id) {
    return unwrap(apiClient.delete(`${base}/sites/${id}`));
}
export function pageExamEquipmentInfo(params) {
    return unwrap(apiClient.get(`${base}/equipment`, { params }));
}
export function listAvailableEquipment(keyword) {
    return unwrap(apiClient.get(`${base}/equipment/available`, { params: { keyword } }));
}
export function createExamEquipmentInfo(payload) {
    return unwrap(apiClient.post(`${base}/equipment`, {
        ...payload,
        equipmentStatus: payload.equipmentStatus || 'idle',
    }));
}
export function updateExamEquipmentInfo(id, payload) {
    return unwrap(apiClient.put(`${base}/equipment/${id}`, payload));
}
export function submitExamEquipmentInfo(id) {
    return unwrap(apiClient.post(`${base}/equipment/${id}/submit`));
}
export function reviewExamEquipmentInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/equipment/${id}/review`, payload));
}
export function disableExamEquipmentInfo(id, payload) {
    return unwrap(apiClient.post(`${base}/equipment/${id}/disable`, payload));
}
export function enableExamEquipmentInfo(id) {
    return unwrap(apiClient.post(`${base}/equipment/${id}/enable`));
}
export function deleteExamEquipmentInfo(id) {
    return unwrap(apiClient.delete(`${base}/equipment/${id}`));
}
