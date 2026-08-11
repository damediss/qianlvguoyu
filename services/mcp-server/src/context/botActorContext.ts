import { createHmac } from "node:crypto";
import * as z from "zod/v4";

export const BOT_ACTOR_META_KEY = "guoyu/bot-actor-context";

const botActorContextSchema = z.object({
  tenant_id: z.string().regex(/^\d+$/).max(32),
  bot_id: z.string().trim().min(1).max(128),
  wecom_user_id: z.string().trim().min(1).max(256),
  chat_id: z.string().trim().min(1).max(256),
  chat_type: z.string().trim().min(1).max(32),
  channel_session_id: z.string().trim().min(1).max(256),
  weknora_session_id: z.string().trim().min(1).max(256),
  request_id: z.string().trim().min(1).max(128)
}).strict();

export type BotActorContext = z.infer<typeof botActorContextSchema>;

export function decodeBotActorContext(encoded: string): BotActorContext {
  if (!encoded || encoded.length > 4096) {
    throw new Error("Invalid BotActorContext header");
  }
  try {
    const value = JSON.parse(Buffer.from(encoded, "base64url").toString("utf8")) as unknown;
    const result = botActorContextSchema.safeParse(value);
    if (!result.success) {
      throw new Error("Invalid BotActorContext header");
    }
    return result.data;
  } catch (error) {
    if (error instanceof Error && error.message === "Invalid BotActorContext header") {
      throw error;
    }
    throw new Error("Invalid BotActorContext header");
  }
}

type ToolRequestExtra = {
  _meta?: unknown;
  requestId?: string | number;
};

export function readBotActorContext(
  extra: ToolRequestExtra,
  required: boolean
): BotActorContext | undefined {
  const meta = isRecord(extra?._meta) ? extra._meta : undefined;
  const candidate = meta?.[BOT_ACTOR_META_KEY];
  if (candidate === undefined) {
    if (required) {
      throw new Error("Verified BotActorContext is required for this tool");
    }
    return undefined;
  }
  const result = botActorContextSchema.safeParse(candidate);
  if (!result.success) {
    throw new Error("Invalid BotActorContext metadata");
  }
  return result.data;
}

/**
 * Encodes the verified context for the platform-api header pair. The signature covers the encoded
 * payload, so both services can verify the exact bytes without depending on JSON key ordering.
 */
export function encodeSignedBotActorContext(
  context: BotActorContext,
  secret: string
): { encoded: string; signature: string } {
  if (!secret || secret.length < 32) {
    throw new Error("BOT_ACTOR_CONTEXT_SECRET must contain at least 32 characters");
  }
  const json = JSON.stringify({
    tenant_id: context.tenant_id,
    bot_id: context.bot_id,
    wecom_user_id: context.wecom_user_id,
    chat_id: context.chat_id,
    chat_type: context.chat_type,
    channel_session_id: context.channel_session_id,
    weknora_session_id: context.weknora_session_id,
    request_id: context.request_id
  });
  const encoded = Buffer.from(json, "utf8").toString("base64url");
  const signature = createHmac("sha256", secret).update(encoded, "utf8").digest("hex");
  return { encoded, signature };
}

export function actorRequestId(context: BotActorContext | undefined, fallback?: string | number): string {
  return context?.request_id || (fallback === undefined ? "unknown" : String(fallback));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
