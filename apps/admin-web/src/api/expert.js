import { apiClient, unwrap } from './client';
export function pageStartTasks(params) {
    return unwrap(apiClient.get('/expert/start-tasks', { params }));
}
export function pageEndTasks(params) {
    return unwrap(apiClient.get('/expert/end-tasks', { params }));
}
export function getExamList(taskId, role) {
    return unwrap(apiClient.get('/expert/exam-list', { params: { taskId, role } }));
}
export function submitExam(payload) {
    return unwrap(apiClient.post('/expert/exam-submit', payload));
}
export function getExamshicaoList(taskId, role) {
    return unwrap(apiClient.get('/expert/examshicao-list', { params: { taskId, role } }));
}
export function submitExamshicao(payload) {
    return unwrap(apiClient.post('/expert/examshicao-submit', payload));
}
export function getExamsubList(taskId, role) {
    return unwrap(apiClient.get('/expert/examsub-list', { params: { taskId, role } }));
}
export function submitExamsub(payload) {
    return unwrap(apiClient.post('/expert/examsub-submit', payload));
}
export function getXimuList(taskId, role) {
    return unwrap(apiClient.get('/expert/ximu-list', { params: { taskId, role } }));
}
export function submitXimu(payload) {
    return unwrap(apiClient.post('/expert/ximu-submit', payload));
}
