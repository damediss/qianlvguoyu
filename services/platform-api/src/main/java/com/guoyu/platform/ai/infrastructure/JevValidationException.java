package com.guoyu.platform.ai.infrastructure;

/**
 * Jev 422 请求校验失败（FastAPI detail 数组，loc 指向出错字段）：
 * 属请求构造 bug 或行级数据越界（实测：criteria 空/<2 项、>255 项均 422），
 * 重试无意义——调用方对该行记失败并跳过，连续 422 达阈值熔断（方案 §5）。
 */
public class JevValidationException extends JevDecisionException {

  public JevValidationException(String message) {
    super(message);
  }
}
