import axios from 'axios';
import JSONBig from 'json-bigint';
import type { ApiResponse } from '../types/api';
import { useFormalExamPlayerStore } from '../stores/formalExamPlayerStore';

const jsonBig = JSONBig({ storeAsString: true });

export const formalExamPlayerClient = axios.create({
  baseURL: '/api',
  timeout: 25000,
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        return jsonBig.parse(data);
      } catch {
        return data;
      }
    }
    return data;
  }],
});

export class FormalExamApiError extends Error {
  code?: string;
  status?: number;
  data?: unknown;

  constructor(message: string, code?: string, status?: number, data?: unknown) {
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

formalExamPlayerClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload && payload.code && payload.code !== '0') {
      throw new FormalExamApiError(payload.message || '请求失败', payload.code, response.status, payload.data);
    }
    return response;
  },
  (error) => {
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
  },
);

function isTransportFailure(error: unknown, message: string) {
  const err = error as { code?: string; response?: unknown };
  if (err?.response) return false;
  return err?.code === 'ERR_NETWORK'
    || err?.code === 'ECONNABORTED'
    || /network error|timeout|failed to fetch|err_connection/i.test(message);
}

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}

export interface FormalExamPlayerLogin {
  playerId: string;
  tenantId: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  accessSessionId?: string;
  generation?: number;
  deviceId?: string;
  examSessionId?: string;
  candidateExamId?: string;
}

export interface FormalExamPlayerContext {
  examSession: {
    id: string;
    status: string;
    examType: string;
    durationSeconds: number;
    actualStartTime?: string | null;
    deadlineTime?: string | null;
  };
  candidateExam: {
    id: string;
    status: string;
    candidateId: string;
    candidateNo?: string | null;
    candidateName?: string | null;
    submitTime?: string | null;
  };
  competition: {
    eventName?: string | null;
    stageName?: string | null;
    tradeName?: string | null;
    skillLevelName?: string | null;
  };
  serverTime: string;
  runtimeIssue?: string | null;
}

export function loginFormalExamPlayer(
  account: string,
  password: string,
  sessionId: string | undefined,
  deviceId: string,
) {
  return unwrap<FormalExamPlayerLogin>(
    formalExamPlayerClient.post('/competition/formal-exam/player/auth/login', {
      account,
      password,
      ...(sessionId ? { sessionId } : {}),
      deviceId,
    }),
  );
}

export function logoutFormalExamPlayer() {
  return unwrap<void>(
    formalExamPlayerClient.post('/competition/formal-exam/player/auth/logout'),
  );
}

export function getFormalExamPlayerContext(sessionId: string) {
  return unwrap<FormalExamPlayerContext>(
    formalExamPlayerClient.get(
      `/competition/formal-exam/player/sessions/${sessionId}/context`,
      { signal: examRequestSignal() },
    ),
  );
}

export interface FormalExamPlayerStatus {
  examSession: FormalExamPlayerContext['examSession'];
  candidateExam: {
    id: string;
    status: string;
    answeredCount?: number | null;
    totalQuestionCount?: number | null;
    submitTime?: string | null;
    lastActiveTime?: string | null;
  };
  serverTime: string;
  runtimeIssue?: string | null;
}

export function getFormalExamPlayerStatus(sessionId: string) {
  return unwrap<FormalExamPlayerStatus>(
    formalExamPlayerClient.get(
      `/competition/formal-exam/player/sessions/${sessionId}/status`,
      { signal: examRequestSignal() },
    ),
  );
}

export function enterFormalExam(sessionId: string) {
  return unwrap<FormalExamPlayerContext>(
    formalExamPlayerClient.post(
      `/competition/formal-exam/player/sessions/${sessionId}/enter`,
      undefined,
      { signal: examRequestSignal() },
    ),
  );
}

export interface FormalExamPlayerQuestion {
  questionSnapshotId: string;
  order: number;
  type: string;
  stem?: string | null;
  score?: number | null;
  options?: FormalExamPlayerOption[];
}

export interface FormalExamPlayerOption {
  optionId: string;
  label: string;
  content?: string | null;
}

export interface FormalExamPlayerSavedAnswer {
  questionSnapshotId: string;
  answer?: {
    selectedOptionIds?: string[];
    blanks?: string[];
  } | null;
  marked: boolean;
  version: number;
  isAnswered: boolean;
  savedAt?: string | null;
}

export interface FormalExamPlayerPaper {
  session: {
    id: string;
    status: string;
    serverTime: string;
    actualStartTime?: string | null;
    deadlineTime?: string | null;
    durationSeconds: number;
  };
  candidate: {
    candidateNo?: string | null;
    candidateName?: string | null;
  };
  paper: {
    questionCount: number;
    totalScore?: number | null;
    answeredCount?: number | null;
  };
  questions: FormalExamPlayerQuestion[];
  answers: FormalExamPlayerSavedAnswer[];
}

export interface FormalExamPlayerAnswerSaveResult {
  questionSnapshotId: string;
  version: number;
  isAnswered: boolean;
  marked: boolean;
  savedAt?: string | null;
  answeredCount: number;
  serverTime: string;
  accepted: boolean;
  conflictCode?: string | null;
  status?: string | null;
  answer?: {
    selectedOptionIds?: string[];
    blanks?: string[];
  } | null;
}

export interface FormalExamPlayerAnswerSyncResult {
  results: FormalExamPlayerAnswerSaveResult[];
  serverTime: string;
}

export interface FormalExamPlayerAnswerSyncItem {
  questionSnapshotId: string;
  answer?: { selectedOptionIds?: string[]; blanks?: string[] } | null;
  marked?: boolean;
  version: number;
}

export function getFormalExamPlayerPaper(sessionId: string) {
  return unwrap<FormalExamPlayerPaper>(
    formalExamPlayerClient.get(
      `/competition/formal-exam/player/sessions/${sessionId}/paper`,
      { signal: examRequestSignal() },
    ),
  );
}

export function saveFormalExamAnswer(
  sessionId: string,
  questionSnapshotId: string,
  body: {
    answer?: { selectedOptionIds?: string[]; blanks?: string[] } | null;
    marked?: boolean;
    version: number;
  },
) {
  return unwrap<FormalExamPlayerAnswerSaveResult>(
    formalExamPlayerClient.put(
      `/competition/formal-exam/player/sessions/${sessionId}/questions/${questionSnapshotId}/answer`,
      body,
      { timeout: 8000, signal: examRequestSignal() },
    ),
  );
}

export function syncFormalExamAnswers(
  sessionId: string,
  body: { answers: FormalExamPlayerAnswerSyncItem[] },
) {
  return unwrap<FormalExamPlayerAnswerSyncResult>(
    formalExamPlayerClient.post(
      `/competition/formal-exam/player/sessions/${sessionId}/answers/sync`,
      body,
      { timeout: 8000, signal: examRequestSignal() },
    ),
  );
}

export interface FormalExamPlayerAnswerVersionItem {
  questionSnapshotId: string;
  version: number;
}

export interface FormalExamPlayerSubmitRequest {
  confirmUnanswered?: boolean;
  answerVersions: FormalExamPlayerAnswerVersionItem[];
}

export interface FormalExamPlayerSubmitResult {
  candidateExamStatus: string;
  submitTime?: string | null;
  answeredCount: number;
  totalQuestionCount: number;
  unansweredCount: number;
  serverTime: string;
}

export interface FormalExamPlayerRealtimeTicket {
  ticket: string;
  expiresIn: number;
  wsPath: string;
  heartbeatSeconds: number;
}

export function createFormalExamRealtimeTicket() {
  return unwrap<FormalExamPlayerRealtimeTicket>(
    formalExamPlayerClient.post(
      '/competition/formal-exam/player/realtime/ticket',
      undefined,
      { signal: examRequestSignal() },
    ),
  );
}

export function ackFormalExamRealtimeCommand(commandId: string) {
  return unwrap<void>(
    formalExamPlayerClient.post(
      '/competition/formal-exam/player/realtime/ack',
      { commandId },
      { signal: examRequestSignal() },
    ),
  );
}

export interface FormalExamRealtimeCatchUpPage {
  events: Array<{
    eventId?: string;
    commandId?: string | null;
    type?: string;
    aggregateId?: string;
    examSessionId?: string;
    candidateExamId?: string | null;
    accessSessionId?: string | null;
    generation?: number | null;
    seq?: number | string | null;
    occurredAt?: string;
    expiresAt?: string;
    target?: string;
    payload?: Record<string, unknown> | null;
  }>;
  nextSeq: string;
  highWatermark: string;
  hasMore: boolean;
  snapshotRequired: boolean;
}

export function fetchFormalExamPlayerRealtimeEvents(query: {
  afterSeq?: string | number | null;
  limit?: number;
}) {
  const params = new URLSearchParams();
  if (query.afterSeq != null && query.afterSeq !== '') {
    params.set('afterSeq', String(query.afterSeq));
  }
  if (query.limit != null) {
    params.set('limit', String(query.limit));
  }
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return unwrap<FormalExamRealtimeCatchUpPage>(
    formalExamPlayerClient.get(
      `/competition/formal-exam/player/realtime/events${suffix}`,
      { signal: examRequestSignal() },
    ),
  );
}

export function submitFormalExamPlayer(sessionId: string, body: FormalExamPlayerSubmitRequest) {
  return unwrap<FormalExamPlayerSubmitResult>(
    formalExamPlayerClient.post(
      `/competition/formal-exam/player/sessions/${sessionId}/submit`,
      body,
      { timeout: 10000, signal: examRequestSignal() },
    ),
  );
}
