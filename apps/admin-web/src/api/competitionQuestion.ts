import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { CompetitionId } from '../types/competition';
import type {
  CompetitionMakeExamPlan,
  CompetitionMakeExamPlanRequest,
  CompetitionMakeExamSnapshot,
  CompetitionMakeExamPlanPrecheck,
  CompetitionQuestionContentType,
  CompetitionQuestion,
  CompetitionQuestionImportBatch,
  CompetitionQuestionQuery,
  CompetitionQuestionRequest,
} from '../types/competitionQuestion';

export function pageCompetitionQuestions(eventId: CompetitionId, params: CompetitionQuestionQuery) {
  return unwrap<PageResponse<CompetitionQuestion>>(
    apiClient.get(`/competition/events/${eventId}/questions`, { params }),
  );
}

export function getCompetitionQuestion(eventId: CompetitionId, questionId: CompetitionId) {
  return unwrap<CompetitionQuestion>(apiClient.get(`/competition/events/${eventId}/questions/${questionId}`));
}

export function createCompetitionQuestion(eventId: CompetitionId, body: CompetitionQuestionRequest) {
  return unwrap<CompetitionQuestion>(apiClient.post(`/competition/events/${eventId}/questions`, body));
}

export function updateCompetitionQuestion(
  eventId: CompetitionId,
  questionId: CompetitionId,
  body: Partial<CompetitionQuestionRequest>,
) {
  return unwrap<CompetitionQuestion>(apiClient.put(`/competition/events/${eventId}/questions/${questionId}`, body));
}

export function deleteCompetitionQuestion(eventId: CompetitionId, questionId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/events/${eventId}/questions/${questionId}`));
}

export function batchBindCompetitionQuestions(
  eventId: CompetitionId,
  body: { blueprintId: CompetitionId; blueprintNodeId: CompetitionId; questionIds: CompetitionId[] },
) {
  return unwrap<CompetitionQuestion[]>(apiClient.post(
    `/competition/events/${eventId}/questions/blueprint-binding/batch`, body,
  ));
}

export function importCompetitionQuestions(
  eventId: CompetitionId,
  scope: { eventWorkTypeId: CompetitionId; eventWorkTypeLevelId: CompetitionId; contentType: string; blueprintId: CompetitionId; selectedSheet?: string },
  file: File,
) {
  const form = new FormData();
  form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
  form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
  form.append('contentType', scope.contentType);
  form.append('blueprintId', String(scope.blueprintId));
  if (scope.selectedSheet) form.append('selectedSheet', scope.selectedSheet);
  form.append('file', file);
  return unwrap<CompetitionQuestionImportBatch>(apiClient.post(
    `/competition/events/${eventId}/question-imports`, form,
  ));
}

export function getCompetitionQuestionImportBatch(eventId: CompetitionId, batchId: CompetitionId) {
  return unwrap<CompetitionQuestionImportBatch>(apiClient.get(`/competition/events/${eventId}/question-imports/${batchId}`));
}

export async function downloadCompetitionQuestionTemplate(contentType: string): Promise<Blob> {
  return (await apiClient.get('/competition/question-import-template', {
    params: { contentType },
    responseType: 'blob',
  })).data as Blob;
}

export function pageCompetitionMakeExamPlans(eventId: CompetitionId, params: Record<string, unknown>) {
  return unwrap<PageResponse<CompetitionMakeExamPlan>>(
    apiClient.get(`/competition/events/${eventId}/make-exam-plans`, { params }),
  );
}

export function getCompetitionMakeExamPlan(eventId: CompetitionId, planId: CompetitionId) {
  return unwrap<CompetitionMakeExamPlan>(apiClient.get(`/competition/events/${eventId}/make-exam-plans/${planId}`));
}

export function createCompetitionMakeExamPlan(eventId: CompetitionId, body: CompetitionMakeExamPlanRequest) {
  return unwrap<CompetitionMakeExamPlan>(apiClient.post(`/competition/events/${eventId}/make-exam-plans`, body));
}

export function precheckCompetitionMakeExamPlan(eventId: CompetitionId, body: CompetitionMakeExamPlanRequest) {
  return unwrap<CompetitionMakeExamPlanPrecheck>(apiClient.post(`/competition/events/${eventId}/make-exam-plans/precheck`, body));
}

export function updateCompetitionMakeExamPlan(eventId: CompetitionId, planId: CompetitionId, body: CompetitionMakeExamPlanRequest) {
  return unwrap<CompetitionMakeExamPlan>(apiClient.put(`/competition/events/${eventId}/make-exam-plans/${planId}`, body));
}

export function deleteCompetitionMakeExamPlan(eventId: CompetitionId, planId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/events/${eventId}/make-exam-plans/${planId}`));
}

export function publishCompetitionMakeExamPlan(eventId: CompetitionId, planId: CompetitionId) {
  return unwrap<CompetitionMakeExamPlan>(apiClient.post(`/competition/events/${eventId}/make-exam-plans/${planId}/publish`));
}

export function listCompetitionMakeExamSnapshots(eventId: CompetitionId, planId: CompetitionId) {
  return unwrap<CompetitionMakeExamSnapshot[]>(apiClient.get(`/competition/events/${eventId}/make-exam-plans/${planId}/snapshots`));
}

export function listCompetitionMakeExamSnapshotsForScope(
  eventId: CompetitionId,
  params: {
    eventWorkTypeId: CompetitionId;
    eventWorkTypeLevelId: CompetitionId;
    contentType?: CompetitionQuestionContentType;
  },
) {
  return unwrap<CompetitionMakeExamSnapshot[]>(
    apiClient.get(`/competition/events/${eventId}/make-exam-snapshots`, { params }),
  );
}

export function previewCompetitionMakeExamSnapshot(eventId: CompetitionId, snapshotId: CompetitionId) {
  return unwrap<CompetitionMakeExamSnapshot>(apiClient.get(`/competition/events/${eventId}/make-exam-plan-snapshots/${snapshotId}/preview`));
}
