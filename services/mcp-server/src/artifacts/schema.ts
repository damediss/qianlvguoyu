import * as z from "zod/v4";

const snowflakeId = z.string().regex(/^\d+$/).max(32);

export const generatedArtifactRefSchema = z.object({
  provider: z.literal("platform-file"),
  documentId: snowflakeId,
  fileId: snowflakeId,
  filename: z.string().trim().min(1).max(255),
  mimeType: z.string().trim().min(1).max(128),
  size: z.number().int().nonnegative().max(1_073_741_824),
  sha256: z.string().regex(/^[a-fA-F0-9]{64}$/),
  version: z.number().int().positive().max(1_000_000),
  draft: z.boolean()
}).strict();

export const generatedArtifactEnvelopeSchema = z.object({
  schema: z.literal("guoyu.generated-artifacts/v1"),
  businessType: z.string().trim().min(1).max(64),
  businessId: snowflakeId,
  taskId: snowflakeId,
  artifacts: z.array(generatedArtifactRefSchema).min(1).max(20)
}).strict();

export type GeneratedArtifactRef = z.infer<typeof generatedArtifactRefSchema>;
export type GeneratedArtifactEnvelope = z.infer<typeof generatedArtifactEnvelopeSchema>;

export function parseGeneratedArtifactEnvelope(value: unknown): GeneratedArtifactEnvelope | undefined {
  const result = generatedArtifactEnvelopeSchema.safeParse(value);
  return result.success ? result.data : undefined;
}

export function isGeneratedArtifactEnvelope(value: unknown): value is GeneratedArtifactEnvelope {
  return parseGeneratedArtifactEnvelope(value) !== undefined;
}
