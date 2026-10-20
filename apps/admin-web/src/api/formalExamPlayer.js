import axios from 'axios';
import JSONBig from 'json-bigint';
import { useFormalExamPlayerStore } from '../stores/formalExamPlayerStore';
const jsonBig = JSONBig({ storeAsString: true });
export const formalExamPlayerClient = axios.create({
    baseURL: '/api',
    timeout: 25000,
    transformResponse: [(data) => {
            if (typeof data === 'string') {
                try {
                    return jsonBig.parse(data);
                }
                catch {
                    return data;
                }
            }
            return data;
        }],
});
export class FormalExamApiError extends Error {
    code;
    status;
    data;
    constructor(message, code, status, data) {
        super(message);
        this.name = 'FormalExamApiError';
        this.code = code;
        this.status = status;
        this.data = data;
    }
}
let examRequestGroup = new AbortController();
function examRequestSignal() {
    return examRequestGroup.signal;
}
function abortExamRequests() {
    examRequestGroup.abort();
    examRequestGroup = new AbortController();
}
formalExamPlayerClient.interceptors.request.use((config) => {
    const token = useFormalExamPlayerStore.getState().token;
    const tenantId = useFormalExamPlayerStore.getState().tenantId;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    if (tenantId) {
        config.headers['X-Tenant-Id'] = String(tenantId);
    }
    return config;
});
formalExamPlayerClient.interceptors.response.use((response) => {
    const payload = response.data;
    if (payload && payload.code && payload.code !== '0') {
        throw new FormalExamApiError(payload.message || '请求失败', payload.code, response.status, payload.data);
    }
    return response;
}, (error) => {
    if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || axios.isCancel(error)) {
        return Promise.reject(new FormalExamApiError('请求已取消', 'CANCELED'));
    }
    if (error?.response?.status === 401) {
        abortExamRequests();
        useFormalExamPlayerStore.getState().clear();
    }
    const payload = error?.response?.data;
    const raw = typeof payload?.message === 'string' && payload.message.trim()
        ? payload.message
        : (error?.message || '请求失败');
    const message = isTransportFailure(error, raw)
        ? '无法连接考试服务器。请用本机热点网关打开本页（Windows 热点一般为 http://192.168.137.1:8080/formal-exam），不要使用 localhost 或电脑网线/Wi-Fi 的 IP。'
        : raw;
    return Promise.reject(new FormalExamApiError(message, payload?.code, error?.response?.status, payload?.data));
});
function isTransportFailure(error, message) {
    const err = error;
    if (err?.response)
        return false;
    return err?.code === 'ERR_NETWORK'
        || err?.code === 'ECONNABORTED'
        || /network error|timeout|failed to fetch|err_connection/i.test(message);
}
async function unwrap(promise) {
    const response = await promise;
    return response.data.data;
}
export function loginFormalExamPlayer(account, password, sessionId, deviceId) {
    return unwrap(formalExamPlayerClient.post('/competition/formal-exam/player/auth/login', {
        account,
        password,
        ...(sessionId ? { sessionId } : {}),
        deviceId,
    }));
}
export function logoutFormalExamPlayer() {
    return unwrap(formalExamPlayerClient.post('/competition/formal-exam/player/auth/logout'));
}
export function getFormalExamPlayerContext(sessionId) {
    return unwrap(formalExamPlayerClient.get(`/competition/formal-exam/player/sessions/${sessionId}/context`, { signal: examRequestSignal() }));
}
export function getFormalExamPlayerStatus(sessionId) {
    return unwrap(formalExamPlayerClient.get(`/competition/formal-exam/player/sessions/${sessionId}/status`, { signal: examRequestSignal() }));
}
export function enterFormalExam(sessionId) {
    return unwrap(formalExamPlayerClient.post(`/competition/formal-exam/player/sessions/${sessionId}/enter`, undefined, { signal: examRequestSignal() }));
}
export function getFormalExamPlayerPaper(sessionId) {
    return unwrap(formalExamPlayerClient.get(`/competition/formal-exam/player/sessions/${sessionId}/paper`, { signal: examRequestSignal() }));
}
export function saveFormalExamAnswer(sessionId, questionSnapshotId, body) {
    return unwrap(formalExamPlayerClient.put(`/competition/formal-exam/player/sessions/${sessionId}/questions/${questionSnapshotId}/answer`, body, { timeout: 8000, signal: examRequestSignal() }));
}
export function syncFormalExamAnswers(sessionId, body) {
    return unwrap(formalExamPlayerClient.post(`/competition/formal-exam/player/sessions/${sessionId}/answers/sync`, body, { timeout: 8000, signal: examRequestSignal() }));
}
export function createFormalExamRealtimeTicket() {
    return unwrap(formalExamPlayerClient.post('/competition/formal-exam/player/realtime/ticket', undefined, { signal: examRequestSignal() }));
}
export function ackFormalExamRealtimeCommand(commandId) {
    return unwrap(formalExamPlayerClient.post('/competition/formal-exam/player/realtime/ack', { commandId }, { signal: examRequestSignal() }));
}
export function fetchFormalExamPlayerRealtimeEvents(query) {
    const params = new URLSearchParams();
    if (query.afterSeq != null && query.afterSeq !== '') {
        params.set('afterSeq', String(query.afterSeq));
    }
    if (query.limit != null) {
        params.set('limit', String(query.limit));
    }
    const suffix = params.toString() ? `?${params.toString()}` : '';
    return unwrap(formalExamPlayerClient.get(`/competition/formal-exam/player/realtime/events${suffix}`, { signal: examRequestSignal() }));
}
export function submitFormalExamPlayer(sessionId, body) {
    return unwrap(formalExamPlayerClient.post(`/competition/formal-exam/player/sessions/${sessionId}/submit`, body, { timeout: 10000, signal: examRequestSignal() }));
}
