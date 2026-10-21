import { apiClient, unwrap } from './client';
export function saveCompetitionStageDraft(eventId, payload) {
    return unwrap(apiClient.put(`/competition/events/${eventId}/stages/draft`, payload));
}
export function saveCompetitionStageDraftForWorkType(eventId, eventWorkTypeId, payload, eventWorkTypeLevelCode) {
    return unwrap(apiClient.put(`/competition/events/${eventId}/work-types/${eventWorkTypeId}/stages/draft`, payload, { params: eventWorkTypeLevelCode == null ? undefined : { eventWorkTypeLevelCode } }));
}
export function confirmCompetitionStagePlan(eventId) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/stages/confirm`));
}
