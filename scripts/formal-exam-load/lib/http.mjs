export async function timedFetch(url, options = {}, fetchImpl = fetch) {
  const started = Date.now();
  try {
    const response = await fetchImpl(url, options);
    return { ok: response.ok, status: response.status, ms: Date.now() - started, response };
  } catch (error) {
    return { ok: false, status: 0, ms: Date.now() - started, error: String(error) };
  }
}

export async function readJson(result) {
  if (!result || !result.response) return null;
  const text = await result.response.text();
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text.slice(0, 500) };
  }
}

export function apiData(body) {
  if (!body) return null;
  if (Object.prototype.hasOwnProperty.call(body, 'data')) return body.data;
  return body;
}

export function jsonHeaders(token, tenantId, extra = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...extra,
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (tenantId != null && tenantId !== '') headers['X-Tenant-Id'] = String(tenantId);
  return headers;
}

export async function apiJson(baseUrl, path, options = {}) {
  const result = await timedFetch(`${baseUrl}${path}`, {
    method: options.method || 'GET',
    headers: jsonHeaders(options.token, options.tenantId, options.headers),
    body: options.body == null ? undefined : JSON.stringify(options.body),
  }, options.fetchImpl);
  const body = await readJson(result);
  return { ...result, body, data: apiData(body) };
}
