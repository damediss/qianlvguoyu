import { createHmac, timingSafeEqual } from "node:crypto";
import type { BotActorContext } from "../context/botActorContext.js";
import type {
  ContractDerivationRequest,
  ContractDraftPlanRequest
} from "./schema.js";

const REQUIRED_FIELDS = [
  "contractNumber",
  "partyA",
  "partyB",
  "amount",
  "projectName",
  "serviceScope",
  "signingDate",
  "partyAContact",
  "partyAPhone",
  "partyBContact",
  "partyBPhone",
  "paymentPercentages"
] as const;

const FIELD_LABELS: Record<(typeof REQUIRED_FIELDS)[number], string> = {
  contractNumber: "合同编号",
  partyA: "甲方",
  partyB: "乙方",
  amount: "合同金额",
  projectName: "项目名称",
  serviceScope: "服务范围",
  signingDate: "签署日期",
  partyAContact: "甲方联系人",
  partyAPhone: "甲方联系电话",
  partyBContact: "乙方联系人",
  partyBPhone: "乙方联系电话",
  paymentPercentages: "付款比例（按顺序，以逗号分隔）"
};

export function planContractDraft(
  request: ContractDraftPlanRequest,
  actor: BotActorContext
): Record<string, unknown> {
  const missingFields = REQUIRED_FIELDS.filter((field) => !request.fields[field]?.value.trim());
  const unconfirmedFields = REQUIRED_FIELDS.filter(
    (field) => request.fields[field] && !request.fields[field]?.confirmed
  );
  const confirmationSummary = {
    template: request.source.originalFilename,
    contractType: request.contractType,
    fields: Object.fromEntries(
      REQUIRED_FIELDS
        .filter((field) => request.fields[field])
        .map((field) => [FIELD_LABELS[field], request.fields[field]?.value])
    )
  };
  if (missingFields.length > 0) {
    return {
      readyForGeneration: false,
      nextAction: "collect_missing_fields",
      missingFields,
      unconfirmedFields,
      questions: missingFields.map((field) => `请提供${FIELD_LABELS[field]}。`),
      confirmationSummary
    };
  }
  if (!request.userConfirmedSummary || unconfirmedFields.length > 0) {
    return {
      readyForGeneration: false,
      nextAction: "confirm_summary",
      missingFields: [],
      unconfirmedFields,
      questions: ["请确认是否按以上历史模板和合同字段生成 DOCX。"],
      confirmationSummary
    };
  }
  if (!isGenerationConfirmation(actor.user_message_text)) {
    return {
      readyForGeneration: false,
      nextAction: "confirm_summary",
      missingFields: [],
      unconfirmedFields: [],
      questions: ["请明确确认是否生成 DOCX；可直接回复“确认”“生成吧”或“可以生成”。"],
      confirmationSummary
    };
  }
  return {
    readyForGeneration: true,
    nextAction: "generate_preview",
    missingFields: [],
    unconfirmedFields: [],
    confirmationSummary,
    draftReadinessToken: createDraftReadinessToken(request, actor)
  };
}

export function assertDraftReadinessToken(
  request: ContractDerivationRequest,
  actor: BotActorContext
): void {
  const planRequest: ContractDraftPlanRequest = {
    contractType: request.contractType,
    source: request.source,
    fields: request.fields,
    userConfirmedSummary: true
  };
  if (!isGenerationConfirmation(actor.user_message_text)) {
    throw new Error("The current trusted user message does not confirm this contract summary");
  }
  if (REQUIRED_FIELDS.some((field) => !request.fields[field]?.confirmed)) {
    throw new Error("Every contract field must remain confirmed before generation");
  }
  const expected = Buffer.from(createDraftReadinessToken(planRequest, actor), "hex");
  const actual = Buffer.from(request.draftReadinessToken, "hex");
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
    throw new Error("contract.planDraft must return a valid readiness token before generation");
  }
}

function createDraftReadinessToken(
  request: ContractDraftPlanRequest,
  actor: BotActorContext
): string {
  const secret = readinessSecret();
  const canonical = stableStringify({
    tenantId: actor.tenant_id,
    channelSessionId: actor.channel_session_id,
    weknoraSessionId: actor.weknora_session_id,
    contractType: request.contractType,
    // Bind the token to the immutable source identity and the actual confirmed
    // business values.  Do not bind it to optional provenance/display metadata:
    // an LLM may faithfully re-materialize source/confirmed/sourceDocumentId or
    // selectionReason between two tool calls without changing the contract the
    // user approved.  The generation schema and the explicit confirmed check in
    // assertDraftReadinessToken keep those safety gates intact.
    source: canonicalSource(request.source),
    fields: canonicalFieldValues(request.fields),
    confirmationMessageId: actor.user_message_id
  });
  return createHmac("sha256", secret).update(canonical).digest("hex");
}

function canonicalSource(source: ContractDraftPlanRequest["source"]): Record<string, unknown> {
  return {
    provider: source.provider,
    knowledgeBaseId: source.knowledgeBaseId,
    knowledgeId: source.knowledgeId,
    sourceSnapshotId: source.sourceSnapshotId,
    originalFilename: source.originalFilename,
    sourceFileHash: source.sourceFileHash,
    manifestId: source.manifestId,
    manifestVersion: source.manifestVersion
  };
}

function canonicalFieldValues(
  fields: ContractDraftPlanRequest["fields"]
): Record<string, string> {
  return Object.fromEntries(REQUIRED_FIELDS.map((field) => [
    field,
    fields[field]?.value.trim() ?? ""
  ]));
}

function isGenerationConfirmation(message: string): boolean {
  const normalized = message
    .trim()
    .toLowerCase()
    .replace(/[\s，,。.!！?？；;：:]/g, "");
  if (!normalized || normalized.length > 32) {
    return false;
  }
  if (/(不要|不生成|不能生成|不可生成|别生成|勿生成|不确认|暂缓|取消|停止|有误|请修改|需要修改|先修改|等等|稍等|稍后|后再|否|不是)/.test(normalized)) {
    return false;
  }
  return /^(我)?((是|是的|确认|同意|可以|好|好的|没问题|无误|内容无误)(请)?(立即|现在)?(开始)?(生成)?(合同|docx|文件)?(吧|了)?|(请)?(立即|现在)?(开始)?生成(合同|docx|文件)?(吧|了)?)$/.test(normalized);
}

function readinessSecret(): string {
  const secret = process.env.CONTRACT_DRAFT_TOKEN_SECRET || process.env.BOT_ACTOR_CONTEXT_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("Contract draft readiness token secret must contain at least 32 characters");
  }
  return secret;
}

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  if (value !== null && typeof value === "object") {
    return `{${Object.entries(value as Record<string, unknown>)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, item]) => `${JSON.stringify(key)}:${stableStringify(item)}`)
      .join(",")}}`;
  }
  return JSON.stringify(value);
}
