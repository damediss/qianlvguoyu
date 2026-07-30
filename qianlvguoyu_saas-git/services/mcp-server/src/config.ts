export interface AppConfig {
  port: number;
  inboundBearerToken: string;
  platformApiBaseUrl: string;
  platformApiToken: string;
  platformTenantId: string;
  platformRequestTimeoutMs: number;
}

function required(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value.trim();
}

function integer(name: string, fallback: number): number {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    return fallback;
  }
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`Invalid positive integer environment variable: ${name}`);
  }
  return parsed;
}

function normalizeBaseUrl(value: string): string {
  return value.endsWith("/") ? value : `${value}/`;
}

export function loadConfig(): AppConfig {
  return {
    port: integer("PORT", 3001),
    inboundBearerToken: required("MCP_BEARER_TOKEN"),
    platformApiBaseUrl: normalizeBaseUrl(required("PLATFORM_API_BASE_URL")),
    platformApiToken: required("PLATFORM_API_TOKEN"),
    platformTenantId: required("PLATFORM_TENANT_ID"),
    platformRequestTimeoutMs: integer("PLATFORM_REQUEST_TIMEOUT_MS", 10000)
  };
}
