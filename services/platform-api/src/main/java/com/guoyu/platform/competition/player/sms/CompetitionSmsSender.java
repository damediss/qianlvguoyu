package com.guoyu.platform.competition.player.sms;

/** Provider boundary. Real SDK implementations are intentionally deferred until supplier selection. */
public interface CompetitionSmsSender {

  CompetitionSmsSendResult send(CompetitionSmsMessage message);
}
