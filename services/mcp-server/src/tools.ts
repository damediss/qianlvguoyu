import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import * as z from "zod/v4";
import { log, sanitizeError } from "./logger.js";
import type { PlatformClient, SearchTasksArgs } from "./platformClient.js";
import {
  actorRequestId,
  readBotActorContext,
  type BotActorContext
} from "./context/botActorContext.js";
import { parseGeneratedArtifactEnvelope } from "./artifacts/schema.js";

const dateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional();
const taskIdSchema = z.string().min(1).describe("Certification task ID from platform-api.");

export function createCertificationMcpServer(platformClient: PlatformClient): McpServer {
  const server = new McpServer(
    {
      name: "certification-data-assistant",
      version: "0.1.0"
    },
    {
      capabilities: {
        logging: {}
      }
    }
  );

  server.registerTool(
    "certification.searchTasks",
    {
      description:
        "Search read-only certification task list data. Use this when the user asks for certification/recognition tasks by keyword, task status, or date range. Returns paged structured JSON from platform-api.",
      inputSchema: {
        keyword: z.string().trim().min(1).max(100).optional().describe("Keyword matched against task name, task number, manager, or company."),
        status: z.string().trim().min(1).max(40).optional().describe("Task status code, for example in_progress, pending_review, rejected, or completed."),
        startDate: dateSchema.describe("Inclusive start date in YYYY-MM-DD. Matches task exam date, plan window, or created date."),
        endDate: dateSchema.describe("Inclusive end date in YYYY-MM-DD. Matches task exam date, plan window, or created date."),
        page: z.number().int().min(1).max(1000).default(1).describe("Page number."),
        pageSize: z.number().int().min(1).max(100).default(20).describe("Page size.")
      }
    },
    async (args, extra) => runTool(
      "certification.searchTasks",
      (actor) => platformClient.searchTasks(args as SearchTasksArgs, actor),
      extra
    )
  );

  server.registerTool(
    "certification.getTaskOverview",
    {
      description:
        "Get a read-only certification task overview by taskId, including task name, work type, level, candidate count, assessor count, current phase, collection completion count, and practical score completion count.",
      inputSchema: {
        taskId: taskIdSchema
      }
    },
    async ({ taskId }, extra) => runTool(
      "certification.getTaskOverview",
      (actor) => platformClient.getTaskOverview(taskId, actor),
      extra
    )
  );

  server.registerTool(
    "certification.getCollectionProgress",
    {
      description:
        "Get read-only practical information collection progress for a certification task. Use this when the user asks about total candidates, completed or incomplete collection, completion rate, incomplete candidates, missing items, or collection exceptions.",
      inputSchema: {
        taskId: taskIdSchema
      }
    },
    async ({ taskId }, extra) => runTool(
      "certification.getCollectionProgress",
      (actor) => platformClient.getCollectionProgress(taskId, actor),
      extra
    )
  );

  server.registerTool(
    "certification.getPracticalScoreProgress",
    {
      description:
        "Get read-only practical scoring progress for a certification task. Use this when the user asks about scored or unscored candidates, practical score completion rate, returned scoring tasks, review-required results, absences, or scoring anomalies.",
      inputSchema: {
        taskId: taskIdSchema
      }
    },
    async ({ taskId }, extra) => runTool(
      "certification.getPracticalScoreProgress",
      (actor) => platformClient.getPracticalScoreProgress(taskId, actor),
      extra
    )
  );

  server.registerTool(
    "file.getArtifactMetadata",
    {
      description:
        "Read trusted metadata for a platform-file artifact. The file ID is the only business input; the current verified BotActorContext is injected out of band and controls tenant and private-chat access.",
      inputSchema: {
        fileId: z.string().regex(/^\d+$/).max(32).describe("Platform file ID as a string.")
      }
    },
    async ({ fileId }, extra) => runTool(
      "file.getArtifactMetadata",
      (actor) => platformClient.getFileArtifactMetadata(fileId, actor!),
      extra,
      true
    )
  );

  return server;
}

async function runTool(
  toolName: string,
  action: (actorContext?: BotActorContext) => Promise<unknown>,
  extra: { _meta?: unknown; requestId?: string | number },
  requireActorContext = false
): Promise<CallToolResult> {
  const startedAt = Date.now();
  const actorContext = readBotActorContext(extra, requireActorContext);
  try {
    const data = await action(actorContext);
    log("info", "mcp tool completed", {
      toolName,
      status: "success",
      requestId: actorRequestId(actorContext, extra.requestId),
      durationMs: Date.now() - startedAt
    });
    return jsonResult(data);
  } catch (error) {
    log("warn", "mcp tool failed", {
      toolName,
      status: "error",
      requestId: actorRequestId(actorContext, extra.requestId),
      durationMs: Date.now() - startedAt,
      error: sanitizeError(error)
    });
    throw error;
  }
}

function jsonResult(data: unknown): CallToolResult {
  const artifactEnvelope = parseGeneratedArtifactEnvelope(data);
  const sanitized = sanitizeToolOutput(isRecord(data) ? data : { value: data });
  const structuredContent = artifactEnvelope || (isRecord(sanitized) ? sanitized : { value: sanitized });
  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(structuredContent, null, 2)
      }
    ],
    structuredContent
  };
}

const MAX_OUTPUT_STRING_LENGTH = 128_000;
const BINARY_FIELD_PATTERN = /(?:base64|bytes|binary|filecontent|contentbytes|^content$)/i;

/** Prevent accidental binary injection into model context while retaining small diagnostic values. */
function sanitizeToolOutput(value: unknown, fieldName = "", depth = 0): unknown {
  if (depth > 8) {
    return "[nested output omitted]";
  }
  if (value instanceof Uint8Array) {
    return { type: "binary", length: value.byteLength };
  }
  if (typeof value === "string") {
    if (BINARY_FIELD_PATTERN.test(fieldName) && value.length > 4096) {
      return { type: "binary-text", length: value.length, omitted: true };
    }
    return value.length > MAX_OUTPUT_STRING_LENGTH
      ? `${value.slice(0, MAX_OUTPUT_STRING_LENGTH)}...[output truncated]`
      : value;
  }
  if (Array.isArray(value)) {
    return value.slice(0, 200).map((item) => sanitizeToolOutput(item, fieldName, depth + 1));
  }
  if (isRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, sanitizeToolOutput(item, key, depth + 1)])
    );
  }
  return value;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
