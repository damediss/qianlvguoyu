import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import * as z from "zod/v4";
import { log, sanitizeError } from "./logger.js";
import type { PlatformClient, SearchTasksArgs } from "./platformClient.js";

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
    async (args) => runTool("certification.searchTasks", () => platformClient.searchTasks(args as SearchTasksArgs))
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
    async ({ taskId }) => runTool("certification.getTaskOverview", () => platformClient.getTaskOverview(taskId))
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
    async ({ taskId }) => runTool("certification.getCollectionProgress", () => platformClient.getCollectionProgress(taskId))
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
    async ({ taskId }) => runTool("certification.getPracticalScoreProgress", () => platformClient.getPracticalScoreProgress(taskId))
  );

  return server;
}

async function runTool(toolName: string, action: () => Promise<unknown>): Promise<CallToolResult> {
  const startedAt = Date.now();
  try {
    const data = await action();
    log("info", "mcp tool completed", {
      toolName,
      status: "success",
      durationMs: Date.now() - startedAt
    });
    return jsonResult(data);
  } catch (error) {
    log("warn", "mcp tool failed", {
      toolName,
      status: "error",
      durationMs: Date.now() - startedAt,
      error: sanitizeError(error)
    });
    throw error;
  }
}

function jsonResult(data: unknown): CallToolResult {
  const structuredContent = isRecord(data) ? data : { value: data };
  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(data, null, 2)
      }
    ],
    structuredContent
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
