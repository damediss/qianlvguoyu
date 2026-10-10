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
import {
  contractDraftPlanInputShape,
  contractDerivationInputShape,
  contractTemplateSelectionInputShape,
  resolveContractSourceInputShape,
  type ContractDraftPlanRequest,
  type ContractDerivationRequest,
  type ContractTemplateSelectionRequest,
  type ResolveContractSourceRequest
} from "./contracts/schema.js";
import { assertDraftReadinessToken, planContractDraft } from "./contracts/draftPlanning.js";
import { assertTemplateSelectionToken, selectContractTemplate } from "./contracts/templateSelection.js";

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
    "contract.selectTemplate",
    {
      description:
        "Mandatory deterministic template gate after WeKnora retrieval and before contract.resolveSourceDocument. Pass every relevant DOCX candidate with the exact retrieval UUIDs and truthful metadata. The tool excludes test/sample/void documents, requires filename compatibility with the requested contract type, ranks valid candidates, and returns needs_user_choice when the leading candidates are too close. Resolve only the selected result; never bypass a no_compatible_template or needs_user_choice status.",
      inputSchema: contractTemplateSelectionInputShape
    },
    async (args, extra) => runTool(
      "contract.selectTemplate",
      async (actor) => selectContractTemplate(args as ContractTemplateSelectionRequest, actor!),
      extra,
      true
    )
  );

  server.registerTool(
    "contract.resolveSourceDocument",
    {
      description:
        "Resolve the unique historical DOCX selected by contract.selectTemplate into an immutable source snapshot and a reviewed structure manifest. Do not ask the user to confirm this template separately; name the selected DOCX in the final template-and-fields summary. Before calling, you MUST retrieve candidates, call contract.selectTemplate, and pass its contractType and templateSelectionToken with the exact selected knowledge_base_id and knowledge_id UUIDs. The token is session- and document-bound; never invent IDs or bypass an ambiguous/no-template result. If this tool fails or derivationStatus is requires_review, do not call contract.generatePreview and do not claim that a DOCX exists.",
      inputSchema: resolveContractSourceInputShape
    },
    async (args, extra) => runTool(
      "contract.resolveSourceDocument",
      (actor) => {
        const request = args as ResolveContractSourceRequest;
        assertTemplateSelectionToken(
          request.contractType,
          request.knowledgeBaseId,
          request.knowledgeId,
          request.templateSelectionToken,
          actor!
        );
        const {
          contractType: _contractType,
          templateSelectionToken: _selectionToken,
          ...platformRequest
        } = request;
        return platformClient.resolveContractSource(platformRequest, actor!);
      },
      extra,
      true
    )
  );

  server.registerTool(
    "contract.planDraft",
    {
      description:
        "Mandatory intake gate for contract generation. After retrieving and resolving the selected historical DOCX, call this tool with every contract field currently supplied by the user. For vague requests it deterministically returns all missingFields together. When all twelve required fields are complete it returns one template-and-fields summary and asks once whether to generate. On the user's next direct confirmation, such as 确认, 生成吧, or 可以生成, set userConfirmedSummary=true and call this tool again in the same turn. No exact phrase or verification code is required. contract.generatePreview requires the returned internal readiness token; never expose, invent, or reuse it.",
      inputSchema: contractDraftPlanInputShape
    },
    async (args, extra) => runTool(
      "contract.planDraft",
      async (actor) => planContractDraft(args as ContractDraftPlanRequest, actor!),
      extra,
      true
    )
  );

  server.registerTool(
    "contract.generatePreview",
    {
      description:
        "Derive a draft DOCX from a confirmed immutable historical source snapshot while preserving its structure and layout. This tool is forbidden until contract.planDraft returns readyForGeneration=true and an internal draftReadinessToken after one direct confirmation from the current trusted user message. No exact phrase or verification code is required. Supply every confirmed business and sensitive field, including signing date, both contacts and phones, and paymentPercentages. Payment-row amounts are recalculated from the new total and confirmed percentages. Section edits and PDF are not enabled.",
      inputSchema: contractDerivationInputShape
    },
    async (args, extra) => runTool(
      "contract.generatePreview",
      (actor) => {
        const request = args as ContractDerivationRequest;
        assertDraftReadinessToken(request, actor!);
        const { draftReadinessToken: _token, ...platformRequest } = request;
        return platformClient.generateContractPreview(platformRequest, actor!);
      },
      extra,
      true
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
