import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

export interface AppConfig {
  port: number;
  inboundBearerToken: string;
  platformApiBaseUrl: string;
  /** Optional cached token; refreshed via login credentials when missing or expired. */
  platformApiToken: string;
  /** Optional initial tenant id; overwritten by login response when present. */
  platformTenantId: string;
  platformTenantCode: string;
  platformUsername: string;
  platformPassword: string;
  /** Shared HMAC secret used only for signed BotActorContext headers. */
  botActorContextSecret: string;
  platformRequestTimeoutMs: number;
}

/**
 * Load key=value pairs from cwd/.env into process.env when the key is unset or empty.
 * Existing non-empty env (e.g. from PM2 ecosystem) wins.
 */
function loadDotEnvFile(): void {
  const envPath = resolve(process.cwd(), ".env");
  if (!existsSync(envPath)) {
    return;
  }
  const text = readFileSync(envPath, "utf8");
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#") || !line.includes("=")) {
      continue;
    }
    const idx = line.indexOf("=");
    const key = line.slice(0, idx).trim();
    let value = line.slice(idx + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    const current = process.env[key];
    if (current === undefined || current.trim().length === 0) {
      process.env[key] = value;
    }
  }
}

function required(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value.trim();
}

function optional(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    return "";
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
  loadDotEnvFile();
  return {
    port: integer("PORT", 3001),
    inboundBearerToken: required("MCP_BEARER_TOKEN"),
    platformApiBaseUrl: normalizeBaseUrl(required("PLATFORM_API_BASE_URL")),
    platformApiToken: optional("PLATFORM_API_TOKEN"),
    platformTenantId: optional("PLATFORM_TENANT_ID"),
    platformTenantCode: required("PLATFORM_TENANT_CODE"),
    platformUsername: required("PLATFORM_API_USERNAME"),
    platformPassword: required("PLATFORM_API_PASSWORD"),
    botActorContextSecret: optional("BOT_ACTOR_CONTEXT_SECRET"),
    platformRequestTimeoutMs: integer("PLATFORM_REQUEST_TIMEOUT_MS", 10000)
  };
}
