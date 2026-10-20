import { apiClient, unwrap } from './client';
export function pageCompetitionQuestionBlueprints(eventId, params) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/question-blueprints`, { params }));
}
export function getCompetitionQuestionBlueprintTree(eventId, blueprintId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/tree`));
}
export function getCompetitionQuestionBlueprintCoverage(eventId, blueprintId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/coverage`));
}
export function importCompetitionQuestionBlueprint(eventId, scope, file) {
    const form = new FormData();
    form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
    form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
    form.append('contentType', scope.contentType);
    form.append('confirmWarnings', String(Boolean(scope.confirmWarnings)));
    form.append('file', file);
    return unwrap(apiClient.post(`/competition/events/${eventId}/question-blueprints/import`, form));
}
export function activateCompetitionQuestionBlueprint(eventId, blueprintId, confirmWarnings = false) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/question-blueprints/${blueprintId}/activate`, undefined, { params: { confirmWarnings } }));
}
export function archiveCompetitionQuestionBlueprint(eventId, blueprintId) {
    return unwrap(apiClient.delete(`/competition/events/${eventId}/question-blueprints/${blueprintId}`));
}
export async function downloadCompetitionQuestionBlueprintSource(eventId, blueprintId) {
    return (await apiClient.get(`/competition/events/${eventId}/question-blueprints/${blueprintId}/source-file`, { responseType: 'blob' })).data;
}
export async function downloadCompetitionQuestionBlueprintTemplate(contentType) {
    return (await apiClient.get('/competition/question-blueprint-template', {
        responseType: 'blob',
        params: { contentType },
    })).data;
}
