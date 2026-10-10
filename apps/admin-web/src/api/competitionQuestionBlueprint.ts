import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { CompetitionId } from '../types/competition';
import type { CompetitionQuestionContentType } from '../types/competitionQuestion';
import type {
  CompetitionQuestionBlueprint,
  CompetitionQuestionBlueprintQuery,
  CompetitionQuestionBlueprintTree,
  CompetitionQuestionBlueprintCoverage,
} from '../types/competitionQuestionBlueprint';

export function pageCompetitionQuestionBlueprints(
  eventId: CompetitionId,
  params: CompetitionQuestionBlueprintQuery,
) {
  return unwrap<PageResponse<CompetitionQuestionBlueprint>>(
    apiClient.get(`/competition/events/${eventId}/question-blueprints`, { params }),
  );
}

export function getCompetitionQuestionBlueprintTree(eventId: CompetitionId, blueprintId: CompetitionId) {
  return unwrap<CompetitionQuestionBlueprintTree>(
    apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/tree`),
  );
}

export function getCompetitionQuestionBlueprintCoverage(eventId: CompetitionId, blueprintId: CompetitionId) {
  return unwrap<CompetitionQuestionBlueprintCoverage>(
    apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/coverage`),
  );
}

export function importCompetitionQuestionBlueprint(
  eventId: CompetitionId,
  scope: { eventWorkTypeId: CompetitionId; eventWorkTypeLevelId: CompetitionId; contentType: string; confirmWarnings?: boolean },
  file: File,
) {
  const form = new FormData();
  form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
  form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
  form.append('contentType', scope.contentType);
  form.append('confirmWarnings', String(Boolean(scope.confirmWarnings)));
  form.append('file', file);
  return unwrap<CompetitionQuestionBlueprint>(apiClient.post(
    `/competition/events/${eventId}/question-blueprints/import`, form,
  ));
}

export function activateCompetitionQuestionBlueprint(eventId: CompetitionId, blueprintId: CompetitionId, confirmWarnings = false) {
  return unwrap<CompetitionQuestionBlueprint>(
    apiClient.post(`/competition/events/${eventId}/question-blueprints/${blueprintId}/activate`, undefined, { params: { confirmWarnings } }),
  );
}

export function archiveCompetitionQuestionBlueprint(eventId: CompetitionId, blueprintId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/events/${eventId}/question-blueprints/${blueprintId}`));
}

export async function downloadCompetitionQuestionBlueprintSource(eventId: CompetitionId, blueprintId: CompetitionId): Promise<Blob> {
  return (await apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/source-file`, { responseType: 'blob' })).data as Blob;
}

export async function downloadCompetitionQuestionBlueprintTemplate(contentType: CompetitionQuestionContentType): Promise<Blob> {
  return (await apiClient.get('/competition/question-blueprint-template', {
    responseType: 'blob',
    params: { contentType },
  })).data as Blob;
}
