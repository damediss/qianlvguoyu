package com.guoyu.platform.ai.infrastructure;

/** Jev 响应结构不合法（零信任：answers 缺失/choice 空/confidence 非数值或越界/probabilities 缺失）——按失败处理，不猜不硬兜底（规范 #5） */
public class JevParseException extends JevDecisionException {

  public JevParseException(String message) {
    super(message);
  }
}
