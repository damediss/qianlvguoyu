package com.guoyu.platform.ai.service;

import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.ai.dto.ObjectiveAnswerDecision;
import com.guoyu.platform.ai.dto.ObjectiveAnswerQuestion;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCallOutcome;
import java.util.function.Consumer;

/**
 * 业务依赖的窄接口（方案 §6）。业务模块只看到模型名与凭据就绪布尔值，
 * 不得接触 baseUrl/apiKey。每个逻辑块成功或失败都必须回调 auditSink。
 */
public interface ObjectiveAnswerCheckAiService {

  ObjectiveAnswerCheckAiDescriptor descriptor();

  ObjectiveAnswerDecision check(ObjectiveAnswerQuestion question,
      ObjectiveAnswerCheckCallContext context, Consumer<ObjectiveAnswerCallOutcome> auditSink);
}
