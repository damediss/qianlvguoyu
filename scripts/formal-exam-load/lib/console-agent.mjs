import { apiJson } from './http.mjs';
import { openTicketSocket, parseEnvelope } from './ws.mjs';
import { fullJitter } from './crypto.mjs';

export function createConsoleAgent(options) {
  const { baseUrl, tenantCode, account, password, sessionId, tenantId, ops, wsStats } = options;
  const state = { token: null, tenantId, socket: null, messages: 0 };

  async function login() {
    const result = await apiJson(baseUrl, '/auth/login', {
      method: 'POST',
      body: { tenantCode, username: account.username, password },
    });
    ops.record('console-login', result);
    if (!result.ok || !result.data?.accessToken) return false;
    state.token = result.data.accessToken;
    state.tenantId = result.data.tenantId || tenantId;
    return true;
  }

  async function authed(path, init = {}) {
    return apiJson(baseUrl, path, { ...init, token: state.token, tenantId: state.tenantId });
  }

  async function snapshot() {
    const result = await authed(`/competition/formal-exam-sessions/${sessionId}`);
    ops.record('console-session', result);
    return result;
  }

  async function startSession(confirmWaitingCandidates = true) {
    const result = await authed(`/competition/formal-exam-sessions/${sessionId}/start`, {
      method: 'POST',
      body: { confirmWaitingCandidates },
    });
    ops.record('console-start', result);
    return result;
  }

  async function pageCandidates(page = 1, size = 20) {
    const result = await authed(
      `/competition/formal-exam-sessions/${sessionId}/candidates?page=${page}&size=${size}`,
    );
    ops.record('console-candidates', result);
    return result;
  }

  async function connectWs() {
    const ticket = await authed(`/competition/formal-exam-sessions/${sessionId}/realtime/ticket`, {
      method: 'POST',
    });
    ops.record('console-ticket', ticket);
    wsStats.attempted += 1;
    if (!ticket.ok || !ticket.data?.ticket) return false;
    const opened = await openTicketSocket({
      baseUrl,
      wsPath: ticket.data.wsPath || '/api/ws/competition/formal-exam/console',
      ticket: ticket.data.ticket,
    });
    if (!opened.opened) {
      wsStats.connectFailed += 1;
      return false;
    }
    wsStats.opened += 1;
    wsStats.active += 1;
    state.socket = opened.socket;
    state.socket.on('message', (raw) => {
      const envelope = parseEnvelope(raw);
      if (envelope) {
        state.messages += 1;
        wsStats.messages += 1;
      }
    });
    state.socket.on('close', () => {
      if (wsStats.active > 0) wsStats.active -= 1;
      wsStats.closed += 1;
      state.socket = null;
    });
    return true;
  }

  async function loop(durationMs) {
    const deadline = Date.now() + durationMs;
    while (Date.now() < deadline) {
      await snapshot();
      await pageCandidates(1, 20);
      await new Promise((resolve) => setTimeout(resolve, 8000 + fullJitter(4000)));
    }
  }

  function stop() {
    if (state.socket) {
      try { state.socket.close(); } catch { /* ignore */ }
    }
  }

  return { login, snapshot, startSession, pageCandidates, connectWs, loop, stop, state };
}
