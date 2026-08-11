import { randomUUID } from "node:crypto";
import type { AppConfig } from "./config.js";
import { log } from "./logger.js";

interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId?: string;
}

interface LoginData {
  accessToken?: string;
  token?: string;
  tenantId?: number | string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface SearchTasksArgs {
  keyword?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

export class PlatformApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly traceId?: string
  ) {
    super(message);
    this.name = "PlatformApiError";
  }
}

export class PlatformClient {
  private accessToken: string;
  private tenantId: string;
  private loginInFlight: Promise<void> | null = null;

  constructor(private readonly config: AppConfig) {
    this.accessToken = config.platformApiToken;
    this.tenantId = config.platformTenantId;
  }

  searchTasks(args: SearchTasksArgs): Promise<PageResponse<unknown>> {
    return this.get("certification/mcp/tasks", { ...args });
  }

  getTaskOverview(taskId: string): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/overview`);
  }

  getCollectionProgress(taskId: string): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/collection-progress`);
  }

  getPracticalScoreProgress(taskId: string): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/practical-score-progress`);
  }

  private async get<T>(path: string, params: Record<string, unknown> = {}): Promise<T> {
    await this.ensureToken();
    try {
      return await this.requestGet<T>(path, params);
    } catch (error) {
      if (!(error instanceof PlatformApiError) || error.status !== 401) {
        throw error;
      }
      log("warn", "platform-api returned 401, refreshing token and retrying once", {
        path
      });
      await this.refreshToken();
      return this.requestGet<T>(path, params);
    }
  }

  private async ensureToken(): Promise<void> {
    if (this.accessToken) {
      return;
    }
    await this.refreshToken();
  }

  private refreshToken(): Promise<void> {
    if (!this.loginInFlight) {
      this.loginInFlight = this.login().finally(() => {
        this.loginInFlight = null;
      });
    }
    return this.loginInFlight;
  }

  private async login(): Promise<void> {
    const url = new URL("auth/login", this.config.platformApiBaseUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.platformRequestTimeoutMs);
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json",
          "X-Request-Id": randomUUID()
        },
        body: JSON.stringify({
          tenantCode: this.config.platformTenantCode,
          username: this.config.platformUsername,
          password: this.config.platformPassword
        }),
        signal: controller.signal
      });
      const text = await response.text();
      if (!response.ok) {
        log("warn", "platform-api login failed", { status: response.status });
        throw new PlatformApiError(`platform-api login failed with HTTP ${response.status}`, response.status);
      }
      const payload = parseJson<ApiResponse<LoginData>>(text);
      if (payload.code !== "0") {
        log("warn", "platform-api login rejected", { code: payload.code });
        throw new PlatformApiError(payload.message || `platform-api login returned code ${payload.code}`, response.status, payload.traceId);
      }
      const token = payload.data?.accessToken || payload.data?.token;
      if (!token) {
        throw new PlatformApiError("platform-api login response missing accessToken", response.status, payload.traceId);
      }
      this.accessToken = token;
      if (payload.data?.tenantId !== undefined && payload.data.tenantId !== null && String(payload.data.tenantId).length > 0) {
        this.tenantId = String(payload.data.tenantId);
      } else if (!this.tenantId) {
        throw new PlatformApiError("platform-api login response missing tenantId and PLATFORM_TENANT_ID is empty", response.status, payload.traceId);
      }
      log("info", "platform-api login success", {
        tenantId: this.tenantId,
        username: this.config.platformUsername,
        tokenLength: this.accessToken.length
      });
    } catch (error) {
      if (error instanceof PlatformApiError) {
        throw error;
      }
      if (error instanceof Error && error.name === "AbortError") {
        throw new PlatformApiError("platform-api login timed out");
      }
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }

  private async requestGet<T>(path: string, params: Record<string, unknown>): Promise<T> {
    const url = new URL(path.replace(/^\/+/, ""), this.config.platformApiBaseUrl);
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.platformRequestTimeoutMs);
    try {
      const response = await fetch(url, {
        method: "GET",
        headers: {
          "Accept": "application/json",
          "Authorization": `Bearer ${this.accessToken}`,
          "X-Tenant-Id": this.tenantId,
          "X-Request-Id": randomUUID()
        },
        signal: controller.signal
      });
      const text = await response.text();
      if (!response.ok) {
        throw new PlatformApiError(`platform-api request failed with HTTP ${response.status}`, response.status);
      }
      const payload = parseJson<ApiResponse<T>>(text);
      if (payload.code !== "0") {
        throw new PlatformApiError(payload.message || `platform-api returned code ${payload.code}`, response.status, payload.traceId);
      }
      return payload.data;
    } catch (error) {
      if (error instanceof PlatformApiError) {
        throw error;
      }
      if (error instanceof Error && error.name === "AbortError") {
        throw new PlatformApiError("platform-api request timed out");
      }
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
}

function parseJson<T>(text: string): T {
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new PlatformApiError("platform-api returned invalid JSON");
  }
}
