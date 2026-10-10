package com.guoyu.platform.ai.infrastructure;

/**
 * Jev 决策调用异常基类（bocha-jev-v1 / TypeSafe SystemOne 协议，非 OpenAI 兼容）。
 * 子类按 HTTP 语义分类：401 → {@link JevUnauthorizedException}（立即熔断）；
 * 422 → {@link JevValidationException}（请求构造 bug，行记失败跳过）；
 * 5xx/429/529/超时重试超限 → {@link JevServerException}；响应结构不合法 → {@link JevParseException}。
 * 全部为行级可恢复异常，由调用方降级 LLM 或跳过（方案 §5）。
 */
public class JevDecisionException extends RuntimeException {

  public JevDecisionException(String message) {
    super(message);
  }

  public JevDecisionException(String message, Throwable cause) {
    super(message, cause);
  }
}
