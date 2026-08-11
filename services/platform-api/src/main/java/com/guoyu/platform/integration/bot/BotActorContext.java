package com.guoyu.platform.integration.bot;

/**
 * Verified external actor context supplied by WeKnora through MCP.
 *
 * <p>This context is not a replacement for the platform service-account principal. The principal
 * remains the final authorization subject; this record only preserves the actual WeCom actor and
 * channel for audit and delivery-boundary checks.</p>
 */
public record BotActorContext(
    Long tenantId,
    String botId,
    String wecomUserId,
    String chatId,
    String chatType,
    String channelSessionId,
    String weknoraSessionId,
    String requestId
) {

  public boolean isPrivateChat() {
    return "private".equalsIgnoreCase(chatType)
        || "single".equalsIgnoreCase(chatType)
        || "single_chat".equalsIgnoreCase(chatType)
        || "1".equals(chatType);
  }
}
