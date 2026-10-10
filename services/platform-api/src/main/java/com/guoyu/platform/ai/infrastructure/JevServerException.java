package com.guoyu.platform.ai.infrastructure;

/** Jev 服务端/网络故障（5xx、429/529 重试超限、超时、连接失败）：该行降级 LLM 全流程（方案 §5） */
public class JevServerException extends JevDecisionException {

  public JevServerException(String message) {
    super(message);
  }

  public JevServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
