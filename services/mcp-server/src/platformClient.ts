import { randomUUID } from "node:crypto";
import type { AppConfig } from "./config.js";

interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId?: string;
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
  constructor(private readonly config: AppConfig) {
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
          "Authorization": `Bearer ${this.config.platformApiToken}`,
          "X-Tenant-Id": this.config.platformTenantId,
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
  } catch (error) {
    throw new PlatformApiError("platform-api returned invalid JSON");
  }
}
