import { apiClient, unwrap } from './client';
import type { CompetitionId, CompetitionStage } from '../types/competition';
import type { CompetitionEvent } from '../types/competition';
import type { StageDraftSavePayload } from '../types/competitionStagePlanning';

export function saveCompetitionStageDraft(
  eventId: CompetitionId,
  payload: StageDraftSavePayload,
) {
  return unwrap<CompetitionStage[]>(
    apiClient.put(`/competition/events/${eventId}/stages/draft`, payload),
  );
}

export function saveCompetitionStageDraftForWorkType(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  payload: StageDraftSavePayload,
  eventWorkTypeLevelCode?: string,
) {
  return unwrap<CompetitionStage[]>(
    apiClient.put(
      `/competition/events/${eventId}/work-types/${eventWorkTypeId}/stages/draft`,
      payload,
      { params: eventWorkTypeLevelCode == null ? undefined : { eventWorkTypeLevelCode } },
    ),
  );
}

export function confirmCompetitionStagePlan(eventId: CompetitionId) {
  return unwrap<CompetitionEvent>(
    apiClient.post(`/competition/events/${eventId}/stages/confirm`),
  );
}
