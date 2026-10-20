import { apiClient, unwrap } from './client';
export function pageCompetitionQuestions(eventId, params) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/questions`, { params }));
}
export function getCompetitionQuestion(eventId, questionId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/questions/${questionId}`));
}
export function createCompetitionQuestion(eventId, body) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/questions`, body));
}
export function createCompetitionSimplePracticalQuestion(eventId, body) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/simple-practical-questions`, body));
}
export function updateCompetitionQuestion(eventId, questionId, body) {
    return unwrap(apiClient.put(`/competition/events/${eventId}/questions/${questionId}`, body));
}
export function deleteCompetitionQuestion(eventId, questionId) {
    return unwrap(apiClient.delete(`/competition/events/${eventId}/questions/${questionId}`));
}
export function batchBindCompetitionQuestions(eventId, body) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/questions/blueprint-binding/batch`, body));
}
export function importCompetitionQuestions(eventId, scope, file) {
    const form = new FormData();
    form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
    form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
    form.append('contentType', scope.contentType);
    form.append('blueprintId', String(scope.blueprintId));
    if (scope.selectedSheet)
        form.append('selectedSheet', scope.selectedSheet);
    form.append('file', file);
    return unwrap(apiClient.post(`/competition/events/${eventId}/question-imports`, form));
}
export function importCompetitionSimpleTheoryQuestions(eventId, scope, file) {
    const form = new FormData();
    form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
    form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
    form.append('file', file);
    return unwrap(apiClient.post(`/competition/events/${eventId}/simple-question-imports`, form));
}
export function importCompetitionSimplePracticalDetails(eventId, scope, file) {
    const form = new FormData();
    form.append('eventWorkTypeId', String(scope.eventWorkTypeId));
    form.append('eventWorkTypeLevelId', String(scope.eventWorkTypeLevelId));
    form.append('file', file);
    return unwrap(apiClient.post(`/competition/events/${eventId}/simple-practical-detail-imports`, form));
}
export function getCompetitionQuestionImportBatch(eventId, batchId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/question-imports/${batchId}`));
}
export async function downloadCompetitionQuestionTemplate(contentType, simple = false) {
    return (await apiClient.get('/competition/question-import-template', {
        params: { contentType, simple },
        responseType: 'blob',
    })).data;
}
export function pageCompetitionMakeExamPlans(eventId, params) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/make-exam-plans`, { params }));
}
export function getCompetitionMakeExamPlan(eventId, planId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/make-exam-plans/${planId}`));
}
export function createCompetitionMakeExamPlan(eventId, body) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/make-exam-plans`, body));
}
export function precheckCompetitionMakeExamPlan(eventId, body) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/make-exam-plans/precheck`, body));
}
export function updateCompetitionMakeExamPlan(eventId, planId, body) {
    return unwrap(apiClient.put(`/competition/events/${eventId}/make-exam-plans/${planId}`, body));
}
export function deleteCompetitionMakeExamPlan(eventId, planId) {
    return unwrap(apiClient.delete(`/competition/events/${eventId}/make-exam-plans/${planId}`));
}
export function publishCompetitionMakeExamPlan(eventId, planId) {
    return unwrap(apiClient.post(`/competition/events/${eventId}/make-exam-plans/${planId}/publish`));
}
export function listCompetitionMakeExamSnapshots(eventId, planId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/make-exam-plans/${planId}/snapshots`));
}
export function listCompetitionMakeExamSnapshotsForScope(eventId, params) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/make-exam-snapshots`, { params }));
}
export function previewCompetitionMakeExamSnapshot(eventId, snapshotId) {
    return unwrap(apiClient.get(`/competition/events/${eventId}/make-exam-plan-snapshots/${snapshotId}/preview`));
}
