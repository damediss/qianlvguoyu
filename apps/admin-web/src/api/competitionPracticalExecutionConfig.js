import { apiClient, unwrap } from './client';
export function getCompetitionPracticalExecutionConfig(stageId) {
    return unwrap(apiClient.get(`/competition/stages/${stageId}/practical-execution-config`));
}
export function listCompetitionPracticalScoringSchemes(scope) {
    return unwrap(apiClient.get('/competition/practical-scoring-schemes', { params: scope }));
}
export function validateCompetitionPracticalExecutionConfig(stageId, request) {
    return unwrap(apiClient.post(`/competition/stages/${stageId}/practical-execution-config/validate`, request));
}
export function saveCompetitionPracticalExecutionConfig(stageId, request) {
    return unwrap(apiClient.put(`/competition/stages/${stageId}/practical-execution-config`, request));
}
