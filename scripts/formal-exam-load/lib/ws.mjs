import WebSocket from 'ws';

export function wsHttpToWs(baseUrl, wsPath) {
  const api = new URL(baseUrl);
  const proto = api.protocol === 'https:' ? 'wss:' : 'ws:';
  const path = wsPath.startsWith('/') ? wsPath : `/${wsPath}`;
  return `${proto}//${api.host}${path}`;
}

export function ticketUrl(baseUrl, wsPath, ticket) {
  return `${wsHttpToWs(baseUrl, wsPath)}?ticket=${encodeURIComponent(ticket)}`;
}

export function openTicketSocket(options) {
  const {
    baseUrl,
    wsPath = '/api/ws/competition/formal-exam/player',
    ticket,
    timeoutMs = 8000,
    WebSocketImpl = WebSocket,
  } = options;
  return new Promise((resolve) => {
    const url = ticketUrl(baseUrl, wsPath, ticket);
    const started = Date.now();
    let settled = false;
    let opened = false;
    const socket = new WebSocketImpl(url);
    const finish = (payload) => {
      if (settled) return;
      settled = true;
      resolve({ socket, ...payload, ms: Date.now() - started });
    };
    socket.on('open', () => {
      opened = true;
      finish({ attempted: true, opened: true });
    });
    socket.on('error', (error) => {
      if (!opened) {
        finish({ attempted: true, opened: false, error: String(error) });
      }
    });
    socket.on('close', (code, reason) => {
      if (!opened) {
        finish({
          attempted: true,
          opened: false,
          code,
          reason: reason ? String(reason) : '',
        });
      }
    });
    setTimeout(() => {
      if (!opened) {
        try { socket.close(); } catch { /* ignore */ }
        finish({ attempted: true, opened: false, error: 'timeout' });
      }
    }, timeoutMs);
  });
}

export function parseEnvelope(raw) {
  try {
    return JSON.parse(String(raw));
  } catch {
    return null;
  }
}
