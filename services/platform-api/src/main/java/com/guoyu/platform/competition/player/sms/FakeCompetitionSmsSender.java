package com.guoyu.platform.competition.player.sms;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development-only deterministic acceptance adapter. It never logs or persists the code/mobile. */
@Component
@ConditionalOnProperty(prefix = "platform.competition.player.sms.aliyun", name = "enabled", havingValue = "false", matchIfMissing = true)
public class FakeCompetitionSmsSender implements CompetitionSmsSender {

  @Override
  public CompetitionSmsSendResult send(CompetitionSmsMessage message) {
    return new CompetitionSmsSendResult(true, "fake-" + UUID.randomUUID());
  }
}
