package com.guoyu.platform.ai.infrastructure;

/** Jev 401 鉴权失败（API 密钥无效/不存在）：配置错误不会因重试变好，调用方须立即熔断整批转 LLM（方案 §5） */
public class JevUnauthorizedException extends JevDecisionException {

  public JevUnauthorizedException(String message) {
    super(message);
  }
}
