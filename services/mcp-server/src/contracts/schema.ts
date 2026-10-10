import * as z from "zod/v4";

const weknoraUuidSchema = z.string().trim().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
  "Use the exact UUID returned by a WeKnora retrieval tool; names, slugs, and guessed aliases are invalid."
);

export const contractFieldValueSchema = z.object({
  value: z.string().trim().min(1).max(5000),
  source: z.enum(["user", "source_document", "rag", "wiki", "system", "agent_inference"]),
  sourceDocumentId: z.string().trim().min(1).max(64).optional(),
  confirmed: z.boolean()
}).strict();

export const contractTypeSchema = z.enum([
  "software-technology-service-framework",
  "software-system-development-implementation",
  "information-platform-operations-service"
]);

export const contractTemplateCandidateSchema = z.object({
  knowledgeBaseId: weknoraUuidSchema,
  knowledgeId: weknoraUuidSchema,
  filename: z.string().trim().min(1).max(255).regex(/\.docx$/i),
  retrievalScore: z.number().min(0).max(1).optional(),
  titleMatch: z.boolean().default(false),
  lifecycleStatus: z.enum(["active", "signed", "draft", "void", "test", "unknown"]).default("unknown"),
  isTestSample: z.boolean().default(false),
  version: z.number().int().min(1).max(1000000).default(1)
}).strict();

export const contractTemplateSelectionInputShape = {
  contractType: contractTypeSchema,
  candidates: z.array(contractTemplateCandidateSchema).min(1).max(20)
};

export const contractTemplateSelectionRequestSchema = z.object(contractTemplateSelectionInputShape).strict();
export type ContractTemplateSelectionRequest = z.infer<typeof contractTemplateSelectionRequestSchema>;

export const contractSourceDocumentSchema = z.object({
  provider: z.literal("weknora-knowledge"),
  knowledgeBaseId: weknoraUuidSchema.describe(
    "Exact knowledge_base_id UUID from the selected WeKnora retrieval result. Never guess or substitute a knowledge-base name."
  ),
  knowledgeId: weknoraUuidSchema.describe(
    "Exact knowledge_id UUID from the selected historical DOCX retrieval result. Never guess or substitute a title, slug, or contract type."
  ),
  sourceSnapshotId: z.string().regex(/^[1-9][0-9]{0,31}$/),
  originalFilename: z.string().trim().min(1).max(255).regex(/\.docx$/i),
  sourceFileHash: z.string().regex(/^[a-f0-9]{64}$/),
  manifestId: z.string().regex(/^[1-9][0-9]{0,31}$/),
  manifestVersion: z.number().int().min(1),
  selectionReason: z.string().trim().max(1000).optional(),
  userConfirmed: z.literal(true)
}).strict();

export const contractFieldsSchema = z.object({
  contractNumber: contractFieldValueSchema,
  partyA: contractFieldValueSchema,
  partyB: contractFieldValueSchema,
  amount: contractFieldValueSchema,
  projectName: contractFieldValueSchema,
  serviceScope: contractFieldValueSchema,
  signingDate: contractFieldValueSchema,
  partyAContact: contractFieldValueSchema,
  partyAPhone: contractFieldValueSchema,
  partyBContact: contractFieldValueSchema,
  partyBPhone: contractFieldValueSchema,
  paymentPercentages: contractFieldValueSchema
}).strict();

export const contractDraftFieldsSchema = contractFieldsSchema.partial();

export const contractDraftPlanInputShape = {
  contractType: contractTypeSchema,
  source: contractSourceDocumentSchema,
  fields: contractDraftFieldsSchema,
  userConfirmedSummary: z.boolean().describe(
    "True only when the current user message explicitly confirms generation from the latest template-and-field summary. No verification code or exact phrase is required."
  )
};

export const contractDraftPlanRequestSchema = z.object(contractDraftPlanInputShape).strict();
export type ContractDraftPlanRequest = z.infer<typeof contractDraftPlanRequestSchema>;

export const contractDerivationInputShape = {
  schema: z.literal("guoyu.contract-derivation-request/v1"),
  contractType: contractTypeSchema,
  source: contractSourceDocumentSchema,
  fields: contractFieldsSchema,
  draftReadinessToken: z.string().regex(/^[a-f0-9]{64}$/).describe(
    "Internal readiness token returned by contract.planDraft after all required fields and one explicit user confirmation are present. The user never needs to type or see this token."
  ),
  sectionEdits: z.array(z.never()).max(0).default([]),
  options: z.object({
    preserveLayout: z.literal(true),
    draftWatermark: z.literal(true),
    outputFormats: z.array(z.literal("docx")).length(1)
  }).strict()
};

export const contractDerivationRequestSchema = z.object(contractDerivationInputShape).strict();
export type ContractDerivationRequest = z.infer<typeof contractDerivationRequestSchema>;

export const resolveContractSourceInputShape = {
  contractType: contractTypeSchema,
  knowledgeBaseId: weknoraUuidSchema.describe(
    "Exact knowledge_base_id UUID returned by knowledge_search, grep_chunks, or another WeKnora retrieval tool."
  ),
  knowledgeId: weknoraUuidSchema.describe(
    "Exact knowledge_id UUID of the retrieved historical DOCX. Do not use a filename, title, slug, or invented alias."
  ),
  templateSelectionToken: z.string().regex(/^[a-f0-9]{64}$/).describe(
    "Opaque selection token returned by contract.selectTemplate for this type and exact historical document."
  )
};

export const resolveContractSourceRequestSchema = z.object(resolveContractSourceInputShape).strict();
export type ResolveContractSourceRequest = z.infer<typeof resolveContractSourceRequestSchema>;

export interface ContractSourceResolution {
  sourceSnapshotId: string;
  knowledgeBaseId: string;
  knowledgeId: string;
  originalFilename: string;
  sourceFileHash: string;
  manifestId: string;
  manifestVersion: number;
  derivationStatus: "derivable" | "requires_review";
  detectedFields: Record<string, string>;
  allowedAnchors: Array<{
    anchorId: string;
    field: string;
    allowedOperations: string[];
  }>;
  warnings: string[];
}
