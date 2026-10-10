import { createHmac, timingSafeEqual } from "node:crypto";
import type { BotActorContext } from "../context/botActorContext.js";
import type { ContractTemplateSelectionRequest } from "./schema.js";

const TEST_FILENAME_PATTERN = /(?:测试|样例|示例|demo|sample)/i;

const TYPE_FILENAME_PATTERNS = {
  "software-technology-service-framework": /软件.*技术服务.*框架协议/i,
  "software-system-development-implementation": /软件.*(?:系统)?开发.*实施合同/i,
  "information-platform-operations-service": /信息.*平台.*(?:运行维护|运维).*合同/i
} as const;

export function selectContractTemplate(
  request: ContractTemplateSelectionRequest,
  actor: BotActorContext
): Record<string, unknown> {
  const pattern = TYPE_FILENAME_PATTERNS[request.contractType];
  const ranked = request.candidates
    .map((candidate) => {
      const rejected = candidate.isTestSample || candidate.lifecycleStatus === "test"
        || candidate.lifecycleStatus === "void" || TEST_FILENAME_PATTERN.test(candidate.filename);
      const typeMatch = pattern.test(candidate.filename);
      const score = (typeMatch ? 100 : 0)
        + (candidate.titleMatch ? 20 : 0)
        + (["active", "signed"].includes(candidate.lifecycleStatus) ? 10 : 0)
        + ((candidate.retrievalScore ?? 0) * 10)
        + Math.min(candidate.version, 1000) / 1000;
      return { ...candidate, rejected, typeMatch, selectionScore: Number(score.toFixed(4)) };
    })
    .sort((left, right) => right.selectionScore - left.selectionScore);

  const eligible = ranked.filter((candidate) => !candidate.rejected && candidate.typeMatch);
  if (eligible.length === 0) {
    return {
      status: "no_compatible_template",
      selected: null,
      rejectedCandidates: ranked.filter((candidate) => candidate.rejected),
      message: "没有找到类型一致且可用的正式历史 DOCX；不得使用测试、样例或作废合同。"
    };
  }
  if (eligible.length > 1 && eligible[0].selectionScore - eligible[1].selectionScore < 0.5) {
    return {
      status: "needs_user_choice",
      selected: null,
      candidates: eligible.slice(0, 3),
      message: "多个正式历史合同的选择分差过小，请让用户明确选择模板。"
    };
  }
  return {
    status: "selected",
    selected: eligible[0],
    templateSelectionToken: createTemplateSelectionToken(
      request.contractType,
      eligible[0].knowledgeBaseId,
      eligible[0].knowledgeId,
      actor
    ),
    alternatives: eligible.slice(1, 3),
    rejectedCandidates: ranked.filter((candidate) => candidate.rejected),
    selectionReason: "合同类型一致，并按标题命中、有效状态、检索分数和版本综合排序。"
  };
}

export function assertTemplateSelectionToken(
  contractType: ContractTemplateSelectionRequest["contractType"],
  knowledgeBaseId: string,
  knowledgeId: string,
  token: string,
  actor: BotActorContext
): void {
  const expected = Buffer.from(createTemplateSelectionToken(
    contractType, knowledgeBaseId, knowledgeId, actor
  ), "hex");
  const actual = Buffer.from(token, "hex");
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
    throw new Error("contract.selectTemplate must select this exact historical DOCX before resolution");
  }
}

function createTemplateSelectionToken(
  contractType: ContractTemplateSelectionRequest["contractType"],
  knowledgeBaseId: string,
  knowledgeId: string,
  actor: BotActorContext
): string {
  const secret = process.env.CONTRACT_DRAFT_TOKEN_SECRET || process.env.BOT_ACTOR_CONTEXT_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("Contract template selection secret must contain at least 32 characters");
  }
  const canonical = JSON.stringify({
    tenantId: actor.tenant_id,
    channelSessionId: actor.channel_session_id,
    weknoraSessionId: actor.weknora_session_id,
    contractType,
    knowledgeBaseId,
    knowledgeId
  });
  return createHmac("sha256", secret).update(canonical).digest("hex");
}
