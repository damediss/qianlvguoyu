import { apiClient, unwrap } from './client';
import type {
  DefenseAssessorReportDetail,
  DefenseAssessorReportListItem,
  DefenseCandidateRow,
  DefenseConfig,
  DefenseMatchResult,
  DefenseSetup,
} from '../types/defenseScoring';

const adminBase = '/certification/defense-scoring';
const assessorBase = '/certification/assessor-defense';

export function getDefenseScoringSetup(taskId: string) {
  return unwrap<DefenseSetup>(apiClient.get(`${adminBase}/tasks/${taskId}/setup`));
}

export function saveDefenseScoringConfig(taskId: string, groupId: string) {
  return unwrap<DefenseConfig>(apiClient.put(`${adminBase}/tasks/${taskId}/config`, { groupId }));
}

export function matchDefenseReports(taskId: string, files: Array<{ fileId: string; fileName?: string }>) {
  return unwrap<DefenseMatchResult>(apiClient.post(`${adminBase}/tasks/${taskId}/reports/match`, { files }));
}

export function bindDefenseCandidateReport(
  taskId: string,
  candidateId: string,
  payload: { fileId: string; fileName?: string },
) {
  return unwrap<DefenseCandidateRow>(
    apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/report`, payload),
  );
}

export function deleteDefenseCandidateReport(taskId: string, candidateId: string) {
  return unwrap<void>(apiClient.delete(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/report`));
}

export function listMyDefenseReports() {
  return unwrap<DefenseAssessorReportListItem[]>(apiClient.get(`${assessorBase}/reports`));
}

export function getMyDefenseReport(reportId: string) {
  return unwrap<DefenseAssessorReportDetail>(apiClient.get(`${assessorBase}/reports/${reportId}`));
}
