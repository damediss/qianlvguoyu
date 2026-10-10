import assert from "node:assert/strict";
import test from "node:test";
import type { BotActorContext } from "../context/botActorContext.js";
import { assertTemplateSelectionToken, selectContractTemplate } from "./templateSelection.js";

process.env.CONTRACT_DRAFT_TOKEN_SECRET = "test-only-contract-draft-secret-32-bytes-minimum";

const actor: BotActorContext = {
  tenant_id: "1", bot_id: "bot-1", wecom_user_id: "user-1", chat_id: "user-1",
  chat_type: "private", channel_session_id: "channel-1", weknora_session_id: "session-1",
  request_id: "request-1", user_message_id: "message-1", user_message_text: "起草合同"
};

const kb = "ee3740b4-cd03-4fb2-84d4-667d05cfc7ac";

test("selects the type-compatible formal template and rejects a test sample", () => {
  const result = selectContractTemplate({
    contractType: "software-technology-service-framework",
    candidates: [
      {
        knowledgeBaseId: kb,
        knowledgeId: "e1c17b96-005f-452f-9fc4-e1f933b4d3ab",
        filename: "01_软件技术服务框架协议_源文档.docx",
        retrievalScore: 0.8,
        titleMatch: true,
        lifecycleStatus: "active",
        isTestSample: false,
        version: 2
      },
      {
        knowledgeBaseId: kb,
        knowledgeId: "6a521abb-3182-4ea1-b911-65cb95cac43a",
        filename: "WeKnora_Agent_IM合同生成链路测试样例.docx",
        retrievalScore: 0.99,
        titleMatch: false,
        lifecycleStatus: "test",
        isTestSample: true,
        version: 1
      }
    ]
  }, actor);
  assert.equal(result.status, "selected");
  assert.equal((result.selected as { knowledgeId: string }).knowledgeId,
    "e1c17b96-005f-452f-9fc4-e1f933b4d3ab");
  assert.doesNotThrow(() => assertTemplateSelectionToken(
    "software-technology-service-framework",
    kb,
    "e1c17b96-005f-452f-9fc4-e1f933b4d3ab",
    String(result.templateSelectionToken),
    actor
  ));
  assert.throws(() => assertTemplateSelectionToken(
    "software-technology-service-framework",
    kb,
    "6a521abb-3182-4ea1-b911-65cb95cac43a",
    String(result.templateSelectionToken),
    actor
  ), /select this exact historical DOCX/);
});

test("requires user choice when two compatible templates are effectively tied", () => {
  const result = selectContractTemplate({
    contractType: "information-platform-operations-service",
    candidates: [
      {
        knowledgeBaseId: kb,
        knowledgeId: "11111111-1111-4111-8111-111111111111",
        filename: "信息平台运维服务合同_A.docx",
        retrievalScore: 0.91,
        titleMatch: true,
        lifecycleStatus: "active",
        isTestSample: false,
        version: 1
      },
      {
        knowledgeBaseId: kb,
        knowledgeId: "22222222-2222-4222-8222-222222222222",
        filename: "信息平台运维服务合同_B.docx",
        retrievalScore: 0.90,
        titleMatch: true,
        lifecycleStatus: "active",
        isTestSample: false,
        version: 1
      }
    ]
  }, actor);
  assert.equal(result.status, "needs_user_choice");
});
