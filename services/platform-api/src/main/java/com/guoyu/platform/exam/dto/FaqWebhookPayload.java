package com.guoyu.platform.exam.dto;

/** WeKnora Webhook 回调请求体 */
public record FaqWebhookPayload(
    String event,
    String message_id,
    String conversation_id,
    String content,
    String reply_content,
    String timestamp
) {
}
