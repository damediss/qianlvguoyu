package com.guoyu.platform.ai.infrastructure;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;

/**
 * FAQ LLM 调用异常基类。
 * 继承 BusinessException 以兼容现有全局异常处理器（GlobalExceptionHandler）。
 * 所有 LLM 调用相关的瞬时/确定性异常均继承此类。
 */
public class FaqLlmException extends BusinessException {

  public FaqLlmException(String message) {
    super(ErrorCode.BAD_GATEWAY, message);
  }
}
