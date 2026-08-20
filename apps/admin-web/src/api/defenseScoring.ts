import { apiClient, unwrap } from './client';
import type {
  DefenseAssessorReportDetail,
  DefenseAssessorReportListItem,
  DefenseCandidateRow,
  DefenseCompleteResult,
  DefenseConfig,
  DefenseMatchResult,
  DefenseProgress,
  DefenseSaveScoreSheetReq,
  DefenseScoreSheetType,
  DefenseScoreSheetView,
  DefenseScoreSheetsView,
  DefenseSetup,
  DefenseSubmitScoreSheetReq,
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

export function getDefenseScoreSheets(reportId: string) {
  return unwrap<DefenseScoreSheetsView>(apiClient.get(`${assessorBase}/reports/${reportId}/score-sheets`));
}

export function saveDefenseScoreSheet(
  reportId: string,
  sheetType: DefenseScoreSheetType,
  payload: DefenseSaveScoreSheetReq,
) {
  return unwrap<DefenseScoreSheetView>(
    apiClient.put(`${assessorBase}/reports/${reportId}/score-sheets/${sheetType}`, payload),
  );
}

export function submitDefenseScoreSheet(
  reportId: string,
  sheetType: DefenseScoreSheetType,
  payload: DefenseSubmitScoreSheetReq,
) {
  return unwrap<DefenseScoreSheetView>(
    apiClient.post(`${assessorBase}/reports/${reportId}/score-sheets/${sheetType}/submit`, payload),
  );
}

export function getDefenseProgress(taskId: string) {
  return unwrap<DefenseProgress>(apiClient.get(`${adminBase}/tasks/${taskId}/progress`));
}

export function completeDefenseTask(taskId: string) {
  return unwrap<DefenseCompleteResult>(apiClient.post(`${adminBase}/tasks/${taskId}/complete`));
}

export function exportDefenseSummary(taskId: string) {
  return apiClient.get(`${adminBase}/tasks/${taskId}/summary/export`, {
    responseType: 'blob',
  });
}
