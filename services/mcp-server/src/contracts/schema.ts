import * as z from "zod/v4";

export const contractFieldValueSchema = z.object({
  value: z.string().trim().min(1).max(5000),
  source: z.enum(["user", "source_document", "rag", "wiki", "system", "agent_inference"]),
  sourceDocumentId: z.string().trim().min(1).max(64).optional(),
  confirmed: z.boolean()
}).strict();

export const contractSourceDocumentSchema = z.object({
  provider: z.literal("weknora-knowledge"),
  knowledgeBaseId: z.string().trim().min(1).max(64),
  knowledgeId: z.string().trim().min(1).max(64),
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
  projectName: contractFieldValueSchema.optional(),
  serviceScope: contractFieldValueSchema.optional()
}).strict();

export const contractDerivationInputShape = {
  schema: z.literal("guoyu.contract-derivation-request/v1"),
  contractType: z.literal("software-technology-service-framework"),
  source: contractSourceDocumentSchema,
  fields: contractFieldsSchema,
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
  knowledgeBaseId: z.string().trim().min(1).max(64),
  knowledgeId: z.string().trim().min(1).max(64),
  userConfirmed: z.literal(true)
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
