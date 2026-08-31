import assert from "node:assert/strict";
import test from "node:test";
import type { BotActorContext } from "../context/botActorContext.js";
import { assertDraftReadinessToken, planContractDraft } from "./draftPlanning.js";
import type { ContractDerivationRequest, ContractDraftPlanRequest } from "./schema.js";

process.env.CONTRACT_DRAFT_TOKEN_SECRET = "test-only-contract-draft-secret-32-bytes-minimum";

const actor: BotActorContext = {
  tenant_id: "1",
  bot_id: "bot-1",
  wecom_user_id: "user-1",
  chat_id: "user-1",
  chat_type: "private",
  channel_session_id: "channel-session-1",
  weknora_session_id: "weknora-session-1",
  request_id: "request-1",
  user_message_id: "message-1",
  user_message_text: "帮我起草一份合同"
};

const source = {
  provider: "weknora-knowledge" as const,
  knowledgeBaseId: "ee3740b4-cd03-4fb2-84d4-667d05cfc7ac",
  knowledgeId: "e1c17b96-005f-452f-9fc4-e1f933b4d3ab",
  sourceSnapshotId: "1001",
  originalFilename: "01_软件技术服务框架协议_源文档.docx",
  sourceFileHash: "a".repeat(64),
  manifestId: "2001",
  manifestVersion: 1,
  userConfirmed: true as const
};

const confirmed = (value: string) => ({ value, source: "user" as const, confirmed: true });

test("returns deterministic missing fields for a vague request", () => {
  const result = planContractDraft({
    contractType: "software-technology-service-framework",
    source,
    fields: { partyA: confirmed("广西远景产业运营有限公司") },
    userConfirmedSummary: false
  }, actor);
  assert.equal(result.readyForGeneration, false);
  assert.equal(result.nextAction, "collect_missing_fields");
  assert.deepEqual(result.missingFields, [
    "contractNumber", "partyB", "amount", "projectName", "serviceScope", "signingDate",
    "partyAContact", "partyAPhone", "partyBContact", "partyBPhone", "paymentPercentages"
  ]);
  assert.equal(result.draftReadinessToken, undefined);
});

test("issues a session-bound token only after final confirmation", () => {
  const fields = {
    contractNumber: confirmed("DEV-2026-001"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("人民币壹佰万元整（¥1,000,000.00）"),
    projectName: confirmed("系统开发项目"),
    serviceScope: confirmed("需求分析、开发、实施与培训"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  const request: ContractDraftPlanRequest = {
    contractType: "software-system-development-implementation",
    source,
    fields,
    userConfirmedSummary: false
  };
  const confirmation = planContractDraft(request, actor);
  assert.equal(confirmation.nextAction, "confirm_summary");
  const confirmedActor = {
    ...actor,
    user_message_id: "message-confirmed",
    user_message_text: "确认"
  };
  request.userConfirmedSummary = true;
  const result = planContractDraft(request, confirmedActor);
  assert.equal(result.readyForGeneration, true);
  const token = String(result.draftReadinessToken);
  assert.match(token, /^[a-f0-9]{64}$/);
  const derivationRequest: ContractDerivationRequest = {
    schema: "guoyu.contract-derivation-request/v1",
    contractType: request.contractType,
    source: request.source,
    fields,
    draftReadinessToken: token,
    sectionEdits: [],
    options: { preserveLayout: true, draftWatermark: true, outputFormats: ["docx"] }
  };
  assert.doesNotThrow(() => assertDraftReadinessToken(derivationRequest, confirmedActor));
});

test("accepts benign provenance metadata drift between planning and generation", () => {
  const fields = {
    contractNumber: confirmed("SVC-2026-012"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("¥100,000.00"),
    projectName: confirmed("技术服务项目"),
    serviceScope: confirmed("实施与运维"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  const plannedSource = { ...source, selectionReason: "检索标题与合同类型一致" };
  const confirmedActor = {
    ...actor,
    user_message_id: "message-confirmed-metadata-drift",
    user_message_text: "确认"
  };
  const token = String(planContractDraft({
    contractType: "software-technology-service-framework",
    source: plannedSource,
    fields,
    userConfirmedSummary: true
  }, confirmedActor).draftReadinessToken);
  const rematerializedFields = Object.fromEntries(Object.entries(fields).map(([field, value]) => [
    field,
    { ...value, source: "agent_inference" as const, sourceDocumentId: source.sourceSnapshotId }
  ])) as ContractDerivationRequest["fields"];
  const derivationRequest: ContractDerivationRequest = {
    schema: "guoyu.contract-derivation-request/v1",
    contractType: "software-technology-service-framework",
    source: { ...source, selectionReason: "模型重新表述但源文档未变化" },
    fields: rematerializedFields,
    draftReadinessToken: token,
    sectionEdits: [],
    options: { preserveLayout: true, draftWatermark: true, outputFormats: ["docx"] }
  };

  assert.doesNotThrow(() => assertDraftReadinessToken(derivationRequest, confirmedActor));
});

test("rejects a token when a confirmed field changes", () => {
  const fields = {
    contractNumber: confirmed("OPS-2026-001"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("¥500,000.00"),
    projectName: confirmed("平台运维项目"),
    serviceScope: confirmed("监控、巡检与故障响应"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  const request: ContractDraftPlanRequest = {
    contractType: "information-platform-operations-service",
    source,
    fields,
    userConfirmedSummary: false
  };
  const confirmation = planContractDraft(request, actor);
  assert.equal(confirmation.nextAction, "confirm_summary");
  const confirmedActor = {
    ...actor,
    user_message_id: "message-confirmed-original",
    user_message_text: "生成吧"
  };
  request.userConfirmedSummary = true;
  const token = String(planContractDraft(request, confirmedActor).draftReadinessToken);
  const tamperedRequest: ContractDerivationRequest = {
    schema: "guoyu.contract-derivation-request/v1",
    contractType: request.contractType,
    source: request.source,
    fields: { ...fields, amount: confirmed("¥600,000.00") },
    draftReadinessToken: token,
    sectionEdits: [],
    options: { preserveLayout: true, draftWatermark: true, outputFormats: ["docx"] }
  };
  const tamperedActor = {
    ...confirmedActor,
    user_message_id: "message-confirmed-tampered",
    user_message_text: "可以生成"
  };
  assert.throws(() => assertDraftReadinessToken(tamperedRequest, tamperedActor), /valid readiness token/);
});

test("does not trust a model confirmation flag without a direct current user confirmation", () => {
  const fields = {
    contractNumber: confirmed("SVC-2026-009"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("¥100,000.00"),
    projectName: confirmed("技术服务项目"),
    serviceScope: confirmed("实施与运维"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  const result = planContractDraft({
    contractType: "software-technology-service-framework",
    source,
    fields,
    userConfirmedSummary: true
  }, { ...actor, user_message_text: "我先看看" });

  assert.equal(result.readyForGeneration, false);
  assert.equal(result.nextAction, "confirm_summary");
  assert.equal(result.draftReadinessToken, undefined);
});

test("accepts common one-turn confirmations without a verification phrase", () => {
  const fields = {
    contractNumber: confirmed("SVC-2026-010"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("¥100,000.00"),
    projectName: confirmed("技术服务项目"),
    serviceScope: confirmed("实施与运维"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  for (const userMessage of [
    "确认", "生成吧", "可以生成", "好的，请生成", "好的，生成吧", "内容无误",
    "是的，请生成", "确认生成 DOCX"
  ]) {
    const result = planContractDraft({
      contractType: "software-technology-service-framework",
      source,
      fields,
      userConfirmedSummary: true
    }, { ...actor, user_message_id: `message-${userMessage}`, user_message_text: userMessage });
    assert.equal(result.readyForGeneration, true, userMessage);
    assert.match(String(result.draftReadinessToken), /^[a-f0-9]{64}$/, userMessage);
  }
});

test("rejects negative or change-request replies", () => {
  const fields = {
    contractNumber: confirmed("SVC-2026-011"),
    partyA: confirmed("甲方公司"),
    partyB: confirmed("乙方公司"),
    amount: confirmed("¥100,000.00"),
    projectName: confirmed("技术服务项目"),
    serviceScope: confirmed("实施与运维"),
    signingDate: confirmed("2026年8月17日"),
    partyAContact: confirmed("张明"),
    partyAPhone: confirmed("13800000001"),
    partyBContact: confirmed("李华"),
    partyBPhone: confirmed("13800000002"),
    paymentPercentages: confirmed("30%,40%,25%,5%")
  };
  for (const userMessage of [
    "不确认", "不要生成", "暂缓生成", "金额有误，请修改", "取消", "我稍后确认", "我确认后再生成"
  ]) {
    const result = planContractDraft({
      contractType: "software-technology-service-framework",
      source,
      fields,
      userConfirmedSummary: true
    }, { ...actor, user_message_text: userMessage });
    assert.equal(result.readyForGeneration, false, userMessage);
    assert.equal(result.draftReadinessToken, undefined, userMessage);
  }
});
