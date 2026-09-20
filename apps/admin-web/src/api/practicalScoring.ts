import { apiClient, unwrap } from './client';
import type {
  AssessorExamDetail,
  AssessorExamListItem,
  AssessorWorkbenchContext,
  AssessorScoringTask,
  AssessorScoringTaskDetail,
  PracticalAdjustScorePayload,
  PracticalBatchGenerateResult,
  PracticalCompleteSign,
  PracticalCompleteTaskResult,
  PracticalGenerateResult,
  PracticalImportPaperResult,
  PracticalPaperQuestion,
  PracticalSaveScorePayload,
  PracticalScoringConfig,
  PracticalScoringProgress,
  PracticalScoringSetup,
  PracticalStartSession,
  PracticalTemplateItem,
} from '../types/practicalScoring';

const adminBase = '/certification/practical-scoring';
const assessorBase = '/certification/assessor-scoring';

export function getPracticalScoringSetup(taskId: string) {
  return unwrap<PracticalScoringSetup>(apiClient.get(`${adminBase}/tasks/${taskId}/setup`));
}

export function savePracticalScoringConfig(
  taskId: string,
  payload: { scopeId: string; workTypeId: number | null; paperId: string; groupId: string },
) {
  return unwrap<PracticalScoringConfig>(apiClient.put(`${adminBase}/tasks/${taskId}/config`, payload));
}

export function savePracticalQuestionTemplate(
  paperId: string,
  questionId: string,
  items: PracticalTemplateItem[],
) {
  return unwrap<PracticalTemplateItem[]>(
    apiClient.put(`${adminBase}/papers/${paperId}/questions/${questionId}/template`, items),
  );
}

export function savePracticalQuestion(
  paperId: string,
  questionId: string,
  payload: { title: string; items: PracticalTemplateItem[] },
) {
  return unwrap<PracticalPaperQuestion>(
    apiClient.put(`${adminBase}/papers/${paperId}/questions/${questionId}`, payload),
  );
}

export function generatePracticalScoringTasks(taskId: string) {
  return unwrap<PracticalGenerateResult>(apiClient.post(`${adminBase}/tasks/${taskId}/generate`));
}

export function linkPracticalCandidateTemplate(
  taskId: string,
  candidateId: string,
  payload: { scopeId?: string; workTypeId?: number | null; paperId: string; groupId: string },
) {
  return unwrap<PracticalGenerateResult>(
    apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/link`, payload),
  );
}

export function batchLinkPracticalCandidateTemplates(
  taskId: string,
  payload: { candidateIds: string[]; paperId: string; groupId: string },
) {
  return unwrap<PracticalBatchGenerateResult>(
    apiClient.post(`${adminBase}/tasks/${taskId}/candidates/batch-link`, payload),
  );
}

export function cancelPracticalCandidateGeneratedTasks(taskId: string, candidateId: string) {
  return unwrap<void>(
    apiClient.delete(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/generated-tasks`),
  );
}

export function importPracticalPaper(taskId: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return unwrap<PracticalImportPaperResult>(
    apiClient.post(`${adminBase}/tasks/${taskId}/papers/import`, formData),
  );
}

export function getPracticalScoringProgress(taskId: string) {
  return unwrap<PracticalScoringProgress>(apiClient.get(`${adminBase}/tasks/${taskId}/progress`));
}

export function completePracticalScoringTask(taskId: string) {
  return unwrap<PracticalCompleteTaskResult>(apiClient.post(`${adminBase}/tasks/${taskId}/complete`));
}

export function requestPracticalCompleteSign(taskId: string) {
  return unwrap<PracticalCompleteSign>(apiClient.post(`${adminBase}/tasks/${taskId}/complete-sign/request`));
}

export function submitMyPracticalCompleteSign(
  taskId: string,
  payload: { signatureFileId: string | number; signatureHash: string; deviceInfo?: string },
) {
  return unwrap<PracticalCompleteSign>(apiClient.post(`${assessorBase}/tasks/${taskId}/complete-sign`, payload));
}

export function exportPracticalSummary(taskId: string, candidateIds: string[]) {
  return apiClient.get(`${adminBase}/tasks/${taskId}/summary/export`, {
    params: { candidateIds: candidateIds.join(',') },
    responseType: 'blob',
  });
}

export function returnPracticalScoringTask(assessorTaskId: string, reason: string) {
  return unwrap<void>(apiClient.post(`${adminBase}/assessor-tasks/${assessorTaskId}/return`, { reason }));
}

export function getPracticalScoringTaskDetail(assessorTaskId: string) {
  return unwrap<AssessorScoringTaskDetail>(apiClient.get(`${adminBase}/assessor-tasks/${assessorTaskId}`));
}

export function adjustPracticalScoringTaskScore(
  assessorTaskId: string,
  payload: PracticalAdjustScorePayload,
) {
  return unwrap<AssessorScoringTaskDetail>(
    apiClient.put(`${adminBase}/assessor-tasks/${assessorTaskId}/score`, payload),
  );
}

export function confirmPracticalCandidate(taskId: string, candidateId: string) {
  return unwrap<PracticalStartSession>(
    apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/confirm`),
  );
}

export function markPracticalCandidateAbsent(taskId: string, candidateId: string) {
  return unwrap<PracticalStartSession>(
    apiClient.post(`${adminBase}/tasks/${taskId}/candidates/${candidateId}/absent`),
  );
}

export function listMyAssessorScoringTasks(status?: string) {
  return unwrap<AssessorScoringTask[]>(apiClient.get(`${assessorBase}/tasks`, { params: { status } }));
}

export function getMyAssessorWorkbenchContext() {
  return unwrap<AssessorWorkbenchContext>(apiClient.get(`${assessorBase}/workbench-context`));
}

export function switchMyAssessorWorkbenchTask(taskId: string) {
  return unwrap<AssessorWorkbenchContext>(apiClient.put(`${assessorBase}/workbench-context`, { taskId }));
}

export function listMyAssessorExams(status?: string) {
  return unwrap<AssessorExamListItem[]>(apiClient.get(`${assessorBase}/exams`, { params: { status } }));
}

export function getMyAssessorExam(taskId: string, candidateId: string) {
  return unwrap<AssessorExamDetail>(apiClient.get(`${assessorBase}/exams/${taskId}/candidates/${candidateId}`));
}

export function getMyAssessorScoringTask(taskId: string) {
  return unwrap<AssessorScoringTaskDetail>(apiClient.get(`${assessorBase}/tasks/${taskId}`));
}

export function confirmMyPracticalStart(taskId: string) {
  return unwrap<PracticalStartSession>(
    apiClient.post(`${assessorBase}/tasks/${taskId}/confirm-start`),
  );
}

export function startMyPracticalSession(taskId: string) {
  return unwrap<PracticalStartSession>(
    apiClient.post(`${assessorBase}/tasks/${taskId}/start-session`),
  );
}

export function completeMyPracticalSession(taskId: string) {
  return unwrap<PracticalStartSession>(
    apiClient.post(`${assessorBase}/tasks/${taskId}/complete-session`),
  );
}

export function createPracticalWebSocketTicket() {
  return unwrap<string>(apiClient.post(`${assessorBase}/websocket-ticket`));
}

export function saveMyAssessorScoringDraft(taskId: string, payload: PracticalSaveScorePayload) {
  return unwrap<AssessorScoringTaskDetail>(apiClient.put(`${assessorBase}/tasks/${taskId}/draft`, payload));
}

export function submitMyAssessorScoringTask(taskId: string, payload: PracticalSaveScorePayload) {
  return unwrap<AssessorScoringTaskDetail>(apiClient.post(`${assessorBase}/tasks/${taskId}/submit`, payload));
}
