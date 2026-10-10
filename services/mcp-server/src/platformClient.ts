import { randomUUID } from "node:crypto";
import type { AppConfig } from "./config.js";
import type { BotActorContext } from "./context/botActorContext.js";
import { encodeSignedBotActorContext } from "./context/botActorContext.js";
import { log } from "./logger.js";
import type {
  ContractDerivationRequest,
  ContractSourceResolution,
  ResolveContractSourceRequest
} from "./contracts/schema.js";

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

export interface FileArtifactMetadata {
  fileId: string;
  filename: string;
  mimeType: string;
  size: number;
  sha256: string;
  accessPolicy: string;
}

export interface FileArtifactContent {
  data: Uint8Array;
  fileId: string;
  filename: string;
  mimeType: string;
  size: number;
  sha256: string;
}

const MAX_OUTBOUND_FILE_BYTES = 20 * 1024 * 1024;

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

  searchTasks(args: SearchTasksArgs, actorContext?: BotActorContext): Promise<PageResponse<unknown>> {
    return this.get("certification/mcp/tasks", { ...args }, actorContext);
  }

  getTaskOverview(taskId: string, actorContext?: BotActorContext): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/overview`, {}, actorContext);
  }

  getCollectionProgress(taskId: string, actorContext?: BotActorContext): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/collection-progress`, {}, actorContext);
  }

  getPracticalScoreProgress(taskId: string, actorContext?: BotActorContext): Promise<unknown> {
    return this.get(`certification/mcp/tasks/${encodeURIComponent(taskId)}/practical-score-progress`, {}, actorContext);
  }

  generateContractPreview(
    args: Omit<ContractDerivationRequest, "draftReadinessToken">,
    actorContext: BotActorContext
  ): Promise<unknown> {
    return this.post("contract/mcp/preview", { ...args }, actorContext);
  }

  resolveContractSource(
    args: Omit<ResolveContractSourceRequest, "contractType" | "templateSelectionToken">,
    actorContext: BotActorContext
  ): Promise<ContractSourceResolution> {
    // A unique result from contract.selectTemplate is the trusted selection gate.
    // Keep the legacy platform flag internal so it never creates a second user-confirmation turn.
    return this.post("contract/mcp/source/resolve", { ...args, userConfirmed: true }, actorContext);
  }

  getFileArtifactMetadata(fileId: string, actorContext: BotActorContext): Promise<FileArtifactMetadata> {
    return this.get(
      `internal/artifacts/files/${encodeURIComponent(fileId)}/metadata`,
      {},
      actorContext
    );
  }

  async getFileArtifactContent(fileId: string, actorContext: BotActorContext): Promise<FileArtifactContent> {
    await this.ensureToken();
    try {
      return await this.requestGetContent(fileId, actorContext);
    } catch (error) {
      if (!(error instanceof PlatformApiError) || error.status !== 401) {
        throw error;
      }
      log("warn", "platform-api returned 401 for artifact content, refreshing token and retrying once", {
        fileId
      });
      await this.refreshToken();
      return this.requestGetContent(fileId, actorContext);
    }
  }

  private async get<T>(
    path: string,
    params: Record<string, unknown> = {},
    actorContext?: BotActorContext
  ): Promise<T> {
    await this.ensureToken();
    try {
      return await this.requestGet<T>(path, params, actorContext);
    } catch (error) {
      if (!(error instanceof PlatformApiError) || error.status !== 401) {
        throw error;
      }
      log("warn", "platform-api returned 401, refreshing token and retrying once", {
        path
      });
      await this.refreshToken();
      return this.requestGet<T>(path, params, actorContext);
    }
  }

  private async post<T>(
    path: string,
    body: Record<string, unknown>,
    actorContext: BotActorContext
  ): Promise<T> {
    await this.ensureToken();
    try {
      return await this.requestPost<T>(path, body, actorContext);
    } catch (error) {
      if (!(error instanceof PlatformApiError) || error.status !== 401) {
        throw error;
      }
      await this.refreshToken();
      return this.requestPost<T>(path, body, actorContext);
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

  private async requestGet<T>(
    path: string,
    params: Record<string, unknown>,
    actorContext?: BotActorContext
  ): Promise<T> {
    const url = new URL(path.replace(/^\/+/, ""), this.config.platformApiBaseUrl);
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.platformRequestTimeoutMs);
    try {
      const headers: Record<string, string> = {
        "Accept": "application/json",
        "Authorization": `Bearer ${this.accessToken}`,
        "X-Tenant-Id": this.tenantId,
        "X-Request-Id": actorContext?.request_id || randomUUID()
      };
      if (actorContext) {
        const signed = encodeSignedBotActorContext(actorContext, this.config.botActorContextSecret);
        headers["X-Bot-Actor-Context"] = signed.encoded;
        headers["X-Bot-Actor-Signature"] = signed.signature;
      }
      const response = await fetch(url, {
        method: "GET",
        headers,
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

  private async requestPost<T>(
    path: string,
    body: Record<string, unknown>,
    actorContext: BotActorContext
  ): Promise<T> {
    const url = new URL(path.replace(/^\/+/, ""), this.config.platformApiBaseUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.platformRequestTimeoutMs);
    try {
      const signed = encodeSignedBotActorContext(actorContext, this.config.botActorContextSecret);
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json",
          "Authorization": `Bearer ${this.accessToken}`,
          "X-Tenant-Id": this.tenantId,
          "X-Request-Id": actorContext.request_id,
          "X-Bot-Actor-Context": signed.encoded,
          "X-Bot-Actor-Signature": signed.signature
        },
        body: JSON.stringify(body),
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

  private async requestGetContent(
    fileId: string,
    actorContext: BotActorContext
  ): Promise<FileArtifactContent> {
    const path = `internal/artifacts/files/${encodeURIComponent(fileId)}/content`;
    const url = new URL(path, this.config.platformApiBaseUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.platformRequestTimeoutMs);
    try {
      const signed = encodeSignedBotActorContext(actorContext, this.config.botActorContextSecret);
      const response = await fetch(url, {
        method: "GET",
        headers: {
          "Accept": "application/octet-stream",
          "Authorization": `Bearer ${this.accessToken}`,
          "X-Tenant-Id": this.tenantId,
          "X-Request-Id": actorContext.request_id,
          "X-Bot-Actor-Context": signed.encoded,
          "X-Bot-Actor-Signature": signed.signature
        },
        signal: controller.signal
      });
      if (!response.ok) {
        throw new PlatformApiError(`platform-api artifact content failed with HTTP ${response.status}`, response.status);
      }
      const declaredSize = parseRequiredSize(response.headers.get("x-file-size"));
      if (declaredSize > MAX_OUTBOUND_FILE_BYTES) {
        throw new PlatformApiError(`artifact exceeds ${MAX_OUTBOUND_FILE_BYTES}-byte outbound limit`, 413);
      }
      const data = new Uint8Array(await response.arrayBuffer());
      if (data.byteLength !== declaredSize) {
        throw new PlatformApiError("platform-api artifact size header does not match response body", 502);
      }
      return {
        data,
        fileId: requiredHeader(response, "x-file-id"),
        filename: decodeUtf8Base64Header(response, "x-file-name-base64"),
        mimeType: requiredHeader(response, "x-file-mime-type"),
        size: declaredSize,
        sha256: response.headers.get("x-file-sha256") || ""
      };
    } catch (error) {
      if (error instanceof PlatformApiError) {
        throw error;
      }
      if (error instanceof Error && error.name === "AbortError") {
        throw new PlatformApiError("platform-api artifact content request timed out");
      }
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
}

function decodeUtf8Base64Header(response: Response, name: string): string {
  const encoded = requiredHeader(response, name);
  try {
    const decoded = Buffer.from(encoded, "base64").toString("utf8");
    if (!decoded || decoded.includes("\uFFFD")) {
      throw new Error("invalid UTF-8 filename");
    }
    return decoded;
  } catch {
    throw new PlatformApiError(`platform-api artifact response has invalid ${name}`, 502);
  }
}

function requiredHeader(response: Response, name: string): string {
  const value = response.headers.get(name);
  if (!value || value.trim().length === 0) {
    throw new PlatformApiError(`platform-api artifact response missing ${name}`, 502);
  }
  return value;
}

function parseRequiredSize(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) {
    throw new PlatformApiError("platform-api artifact response has invalid x-file-size", 502);
  }
  const size = Number(value);
  if (!Number.isSafeInteger(size) || size < 0) {
    throw new PlatformApiError("platform-api artifact response has invalid x-file-size", 502);
  }
  return size;
}

function parseJson<T>(text: string): T {
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new PlatformApiError("platform-api returned invalid JSON");
  }
}
